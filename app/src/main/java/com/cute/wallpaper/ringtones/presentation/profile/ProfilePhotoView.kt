package com.cute.wallpaper.ringtones.presentation.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.cute.wallpaper.ringtones.R
import kotlin.math.hypot
import kotlin.math.min

class ProfilePhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var bitmap: Bitmap? = null
    private var transform = ProfilePhotoTransform()
    private var name = ""
    private var adjustment = ProfileAdjustment.ORIGINAL
    private var frame = ProfileFrame.NONE
    private var backgroundColor = Color.rgb(225, 202, 234)
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var previousFocusX = 0f
    private var previousFocusY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val nameTypeface = ResourcesCompat.getFont(context, R.font.poppins_semi_bold)
    private val compositionIcons by lazy {
        fun icon(resource: Int, size: Int): Bitmap {
            val drawable = requireNotNull(AppCompatResources.getDrawable(context, resource))
            val pixels = size * 4
            return Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888).also { bitmap ->
                drawable.setBounds(0, 0, pixels, pixels)
                drawable.draw(Canvas(bitmap))
            }
        }
        ProfileCompositionIcons(
            sparkle24 = icon(R.drawable.ic_sparkle, 24),
            sparkle32 = icon(R.drawable.ic_sparkle, 32),
            sparkle20 = icon(R.drawable.ic_sparkle, 20),
            sparkle16 = icon(R.drawable.ic_sparkle, 16),
            nameMagic = icon(R.drawable.ic_sparkles, 16)
        )
    }

    var photoGesturesEnabled: Boolean = false
    var drawSparkles: Boolean = true
    var thumbnailCornerRadius: Float? = null
    var onTransformChanged: ((ProfilePhotoTransform) -> Unit)? = null

    init {
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.ProfilePhotoView).use { typedArray ->
                if (typedArray.hasValue(R.styleable.ProfilePhotoView_profileThumbnailCornerRadius)) {
                    thumbnailCornerRadius = typedArray.getDimension(
                        R.styleable.ProfilePhotoView_profileThumbnailCornerRadius,
                        0f
                    )
                }
                drawSparkles = typedArray.getBoolean(
                    R.styleable.ProfilePhotoView_profileDrawSparkles,
                    drawSparkles
                )
            }
        }
    }

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                previousFocusX = detector.focusX
                previousFocusY = detector.focusY
                moved = true
                return photoGesturesEnabled && bitmap != null
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val photo = bitmap ?: return false
                val diameter = photoDiameter()
                if (diameter <= 0f) return false
                val focusX = (previousFocusX - contentBounds().centerX()) / diameter
                val focusY = (previousFocusY - contentBounds().centerY()) / diameter
                updateTransform(transform.zoomedAt(detector.scaleFactor, focusX, focusY,
                    photo.width, photo.height).translated(
                    (detector.focusX - previousFocusX) / diameter,
                    (detector.focusY - previousFocusY) / diameter,
                    photo.width, photo.height))
                previousFocusX = detector.focusX
                previousFocusY = detector.focusY
                return true
            }
        })

    fun setPhotoBitmap(photo: Bitmap?) {
        bitmap = photo
        invalidate()
    }

    fun renderState(
        transform: ProfilePhotoTransform,
        name: String,
        adjustment: ProfileAdjustment,
        frame: ProfileFrame,
        backgroundColor: Int
    ) {
        this.transform = transform
        this.name = name
        this.adjustment = adjustment
        this.frame = frame
        this.backgroundColor = backgroundColor
        invalidate()
    }

    fun snapshot(): ProfileComposition? = bitmap?.takeUnless { it.isRecycled }?.let {
        ProfileComposition(it, transform, name, adjustment, frame, backgroundColor, icons = compositionIcons).let { composition ->
            nameTypeface?.let { font -> composition.copy(nameTypeface = font) } ?: composition
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        snapshot()?.let { composition ->
            val radius = thumbnailCornerRadius
            if (radius == null) composition.draw(canvas, contentBounds(), drawSparkles)
            else composition.drawPhoto(canvas, contentBounds(), radius)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val photo = bitmap
        if (!photoGesturesEnabled || photo == null || photo.isRecycled) return super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val bounds = contentBounds()
            val radius = photoDiameter() / 2f
            if (hypot(event.x - bounds.centerX(), event.y - bounds.centerY()) > radius) return false
            activePointerId = event.getPointerId(0)
            lastX = event.x
            lastY = event.y
            downX = event.x
            downY = event.y
            moved = false
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) {
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    if (hypot(x - downX, y - downY) > touchSlop) moved = true
                    if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                        val diameter = photoDiameter()
                        if (diameter > 0f) updateTransform(transform.translated(
                            (x - lastX) / diameter, (y - lastY) / diameter, photo.width, photo.height))
                    }
                    lastX = x
                    lastY = y
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    val nextIndex = if (event.actionIndex == 0) 1 else 0
                    activePointerId = event.getPointerId(nextIndex)
                    lastX = event.getX(nextIndex)
                    lastY = event.getY(nextIndex)
                }
            }
            MotionEvent.ACTION_UP -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!moved) performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateTransform(next: ProfilePhotoTransform) {
        if (transform == next) return
        transform = next
        onTransformChanged?.invoke(next)
        invalidate()
    }

    private fun contentBounds() = RectF(paddingLeft.toFloat(), paddingTop.toFloat(),
        (width - paddingRight).toFloat(), (height - paddingBottom).toFloat())

    private fun photoDiameter() = min(width - paddingLeft - paddingRight,
        height - paddingTop - paddingBottom) * 300f / 340f
}
