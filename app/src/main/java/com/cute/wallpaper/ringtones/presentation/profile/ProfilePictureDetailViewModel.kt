package com.cute.wallpaper.ringtones.presentation.profile

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.data.profile.ProfileImageExporter
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import com.cute.wallpaper.ringtones.presentation.navigation.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ProfilePictureDetailViewModel @Inject constructor(
    repository: ContentRepository,
    private val exporter: ProfileImageExporter,
    private val saved: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val available = repository.getContents()
        .filter { it.type == ContentType.PROFILE_PICTURE }
        .map { it.toUiModel() }
    private val requestedIds = saved.get<ArrayList<String>>(ARG_CONTENT_IDS)
    val items = if (requestedIds.isNullOrEmpty()) {
        available
    } else {
        requestedIds.mapNotNull { id ->
            available.firstOrNull { it.id == id }
        }.ifEmpty { available }
    }
    val position = saved.getLiveData(
        "profile_position",
        items.indexOfFirst { it.id == saved.get<String>(ARG_CONTENT_ID) }.coerceAtLeast(0)
    )
    val photoUri = saved.getLiveData("profile_photo_uri", "")
    val name = saved.getLiveData("profile_name", "")
    val tool = saved.getLiveData("profile_tool", "")
    val adjustment = saved.getLiveData("profile_adjustment", ProfileAdjustment.ORIGINAL.name)
    val frame = saved.getLiveData("profile_frame", ProfileFrame.NONE.name)
    val transform: ProfilePhotoTransform
        get() = ProfilePhotoTransform(
            saved["profile_zoom"] ?: 1f,
            saved["profile_x"] ?: 0f,
            saved["profile_y"] ?: 0f
        )
    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> = _bitmap
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _saving = MutableLiveData(false)
    val saving: LiveData<Boolean> = _saving
    private val _result = MutableLiveData<Event<Boolean>>()
    val result: LiveData<Event<Boolean>> = _result
    private val _loadFailed = MutableLiveData(false)
    val loadFailed: LiveData<Boolean> = _loadFailed
    private var imageJob: Job? = null
    val currentItem get() = items.getOrNull(position.value ?: 0)
    var resumeAdsBeforePicker: Boolean?
        get() = saved["profile_resume_ads"]
        set(value) {
            saved["profile_resume_ads"] = value
        }

    init {
        loadPhoto()
    }

    fun select(delta: Int) {
        if (_saving.value == true) return
        val next = (position.value ?: 0) + delta
        if (next !in items.indices) return
        position.value = next
        photoUri.value = ""
        name.value = ""
        tool.value = ""
        adjustment.value = ProfileAdjustment.ORIGINAL.name
        frame.value = ProfileFrame.NONE.name
        updateTransform(ProfilePhotoTransform())
        loadPhoto()
    }

    fun selectPhoto(uri: String) {
        photoUri.value = uri
        tool.value = "PHOTO"
        updateTransform(ProfilePhotoTransform())
        loadPhoto()
    }

    fun updateTransform(value: ProfilePhotoTransform) {
        saved["profile_zoom"] = value.zoom
        saved["profile_x"] = value.offsetX
        saved["profile_y"] = value.offsetY
    }

    fun reloadPhoto() = loadPhoto()

    private fun loadPhoto() {
        imageJob?.cancel()
        _bitmap.value = null
        _loadFailed.value = false
        val source = photoUri.value?.takeIf(String::isNotBlank) ?: currentItem?.contentUrl
        if (source.isNullOrBlank()) {
            _loadFailed.value = true
            _loading.value = false
            return
        }
        _loading.value = true
        imageJob = viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) {
                    val manager = Glide.with(context)
                    val target = manager.asBitmap().load(source).submit(1024, 1024)
                    try {
                        val loaded = target.get(30, TimeUnit.SECONDS)
                        ensureActive()
                        requireNotNull(loaded.copy(Bitmap.Config.ARGB_8888, false))
                    } finally {
                        manager.clear(target)
                    }
                }
                _bitmap.value = decoded
                _loading.value = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _loadFailed.value = true
                _loading.value = false
            }
        }
    }

    fun download(composition: ProfileComposition) {
        if (_saving.value == true || currentItem?.downloadEnabled != true) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) { composition.renderBitmap(1024) }
                val success = try {
                    exporter.save(bitmap) != null
                } finally {
                    bitmap.recycle()
                }
                _result.value = Event(success)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _result.value = Event(false)
            } finally {
                _saving.value = false
            }
        }
    }

    companion object {
        const val ARG_CONTENT_ID = "profile_content_id"
        const val ARG_CONTENT_IDS = "profile_content_ids"
    }
}
