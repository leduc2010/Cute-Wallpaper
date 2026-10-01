package com.cute.wallpaper.ringtones.presentation.navigation

import androidx.lifecycle.Observer

class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        if (hasBeenHandled) return null
        hasBeenHandled = true
        return content
    }
}

class EventObserver<T>(
    private val onEventUnhandledContent: (T) -> Unit
) : Observer<Event<T>?> {

    override fun onChanged(value: Event<T>?) {
        value?.getContentIfNotHandled()?.let(onEventUnhandledContent)
    }
}
