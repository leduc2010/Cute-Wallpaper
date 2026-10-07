package com.cute.wallpaper.ringtones.presentation.quotes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Build
import com.cute.wallpaper.ringtones.utils.showCenterToast
import com.cute.wallpaper.ringtones.utils.showErrorToast
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

object QuoteActions {
    fun copy(context: Context, item: HomeContentUiModel) {
        val text = item.quote?.takeIf(String::isNotBlank) ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.quotes_new), text))
        // Android 13+ already presents the system clipboard confirmation.
        if (Build.VERSION.SDK_INT < 33) {
            context.showCenterToast(R.string.quotes_copied)
        }
    }

    fun shareText(context: Context, item: HomeContentUiModel) {
        val text = item.quote?.takeIf(String::isNotBlank) ?: return
        try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, context.getString(R.string.quotes_share)))
        } catch (_: ActivityNotFoundException) {
            context.showErrorToast(R.string.quotes_export_failed)
        }
    }
}
