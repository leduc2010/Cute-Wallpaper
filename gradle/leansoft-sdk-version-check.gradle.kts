// Chặn build khi app đang dùng bản Leansoft Publishing SDK cũ hơn bản mới nhất
// trên GitHub Packages.
//
// Dependency vẫn được pin cứng: build luôn tái lập được và bản release không bao giờ
// tự nhảy sang một version chưa ai test. Task này không tự sửa version, chỉ báo lỗi
// để member buộc phải cập nhật.
//
// Ba trường hợp KHÔNG chặn build:
//   - Build offline (--offline): bỏ qua, vì không có mạng thì cũng không tải được
//     bản mới về mà sửa.
//   - Không truy cập được GitHub Packages (VPN/token lỗi): bỏ qua im lặng.
//   - Chạy kèm -PallowOutdatedLeansoftSdk: chỉ cảnh báo. Dùng khi cần build gấp
//     hoặc khi bản mới có breaking change mà chưa kịp migrate.
//
// Cách dùng, thêm vào build.gradle.kts của module app:
//   apply(from = rootProject.file("gradle/leansoft-sdk-version-check.gradle.kts"))

import java.util.concurrent.TimeUnit

val sdkGroup = "com.github.ngoxuanhungbk"
val sdkArtifact = "ls-leansoft-publishing-sdk"

// Mặc định chặn build. Đặt allowOutdatedLeansoftSdk=true trong gradle.properties
// hoặc truyền -PallowOutdatedLeansoftSdk để tạm hạ xuống mức cảnh báo.
val allowOutdated = providers.gradleProperty("allowOutdatedLeansoftSdk").isPresent
// Gradle offline vẫn resolve được version từ cache, nên phải tự loại trừ: chặn build
// lúc này chỉ làm member kẹt, vì họ cũng không tải nổi AAR mới về.
val isOfflineBuild = gradle.startParameter.isOffline

fun compareVersions(a: String, b: String): Int {
    val left = a.split('.', '-')
    val right = b.split('.', '-')
    for (i in 0 until maxOf(left.size, right.size)) {
        val l = left.getOrNull(i)
        val r = right.getOrNull(i)
        val ln = l?.toIntOrNull()
        val rn = r?.toIntOrNull()
        val cmp = when {
            ln != null && rn != null -> ln.compareTo(rn)
            // Thiếu segment thì nhỏ hơn (1.3 < 1.3.1)
            l == null -> -1
            r == null -> 1
            else -> l.compareTo(r)
        }
        if (cmp != 0) return cmp
    }
    return 0
}

// Đăng ký trong afterEvaluate để block `dependencies { }` đã chạy xong, nhờ đó đọc được
// đúng version đang pin từ chính dependency - không sợ khai báo lệch nhau.
afterEvaluate {

    val declaredVersion: String? = configurations.findByName("implementation")
        ?.dependencies
        ?.firstOrNull { it.group == sdkGroup && it.name == sdkArtifact }
        ?.version

    val checkTask = tasks.register("checkLeansoftSdkVersion") {
        group = "verification"
        description = "Báo khi có bản Leansoft Publishing SDK mới hơn trên GitHub Packages."

        val declared = declaredVersion
        val allowOld = allowOutdated
        val offline = isOfflineBuild
        val log = logger

        outputs.upToDateWhen { false }

        doLast {
            if (declared == null) {
                log.info("Leansoft SDK: không tìm thấy dependency trong `implementation`, bỏ qua.")
                return@doLast
            }
            if (offline) {
                log.info("Leansoft SDK: build offline, bỏ qua kiểm tra version.")
                return@doLast
            }
            if (declared.contains('+') || declared.contains('[')) {
                log.lifecycle("Leansoft SDK: version '$declared' là dynamic, không cần so sánh.")
                return@doLast
            }

            // Hỏi registry ở mọi lần build, không cache: SDK vừa ra bản mới là chặn ngay,
            // không để member lọt qua trong một khoảng thời gian nào cả.
            // Resolve qua chính repository đã khai báo trong settings.gradle.kts nên dùng
            // luôn credential có sẵn - member không phải cấu hình token riêng.
            // cacheDynamicVersionsFor(0) là bắt buộc: thiếu nó Gradle trả về kết quả cũ
            // trong cache mặc định 24h của nó và check sẽ báo sai.
            val latest = runCatching {
                val probe = configurations.detachedConfiguration(
                    dependencies.create("$sdkGroup:$sdkArtifact:+@pom")
                ).apply {
                    isTransitive = false
                    resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
                }
                probe.resolvedConfiguration
                    .lenientConfiguration
                    .firstLevelModuleDependencies
                    .firstOrNull()
                    ?.moduleVersion
            }.getOrNull()

            if (latest.isNullOrBlank()) {
                // Mất mạng, VPN, token hết hạn: không bao giờ là lý do làm hỏng build của member.
                log.info("Leansoft SDK: không truy cập được GitHub Packages, bỏ qua kiểm tra version.")
                return@doLast
            }

            if (compareVersions(declared, latest) < 0) {
                val message = buildString {
                    appendLine("")
                    appendLine("Leansoft Publishing SDK đang dùng bản cũ.")
                    appendLine("")
                    appendLine("    Đang dùng : $declared")
                    appendLine("    Mới nhất  : $latest")
                    appendLine("")
                    appendLine("Sửa trong ${project.name}/build.gradle.kts:")
                    appendLine("    implementation(\"$sdkGroup:$sdkArtifact:$latest\")")
                    appendLine("")
                    appendLine("Bản mới có thể đổi API. Xem mục \"Nâng cấp từ bản cũ\" trong README")
                    appendLine("của repo demo trước khi sửa.")
                    appendLine("")
                    appendLine("Cần build gấp mà chưa kịp cập nhật, thêm:")
                    appendLine("    -PallowOutdatedLeansoftSdk")
                }
                if (allowOld) {
                    log.warn(message)
                } else {
                    throw GradleException(message)
                }
            } else {
                log.lifecycle("Leansoft Publishing SDK $declared - đang là bản mới nhất.")
            }
        }
    }

    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(checkTask)
    }
}
