package com.cute.wallpaper.ringtones.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cute.wallpaper.ringtones.R

class SafeAreaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var enableSafeTop: Boolean = true
    private var enableSafeBottom: Boolean = true

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.SafeAreaView, 0, 0).apply {
            try {
                enableSafeTop = getBoolean(R.styleable.SafeAreaView_enableSafeTop, true)
                enableSafeBottom = getBoolean(R.styleable.SafeAreaView_enableSafeBottom, true)
            } finally {
                recycle()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val topPadding = if (enableSafeTop) systemBars.top else 0
            val bottomPadding = if (enableSafeBottom) systemBars.bottom else 0

            setPadding(systemBars.left, topPadding, systemBars.right, bottomPadding)
            insets
        }
    }

    fun setEnableSafeTop(enable: Boolean) {
        enableSafeTop = enable
        requestApplyInsets()
    }

    fun setEnableSafeBottom(enable: Boolean) {
        enableSafeBottom = enable
        requestApplyInsets()
    }
}
