package com.cute.wallpaper.ringtones.presentation.navigation

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    private val _actionDestination = MutableLiveData<Event<ActionNavigate>>()
    val actionDestination: LiveData<Event<ActionNavigate>> = _actionDestination

    private val _naviDirection = MutableLiveData<Event<NavDirections>>()
    val naviDirection: LiveData<Event<NavDirections>> = _naviDirection

    private val _actionBack = MutableLiveData<Event<ActionBack>>()
    val actionBack: LiveData<Event<ActionBack>> = _actionBack

    fun navigate(
        destination: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null
    ) {
        _actionDestination.value = Event(
            ActionNavigate(
                destination = destination,
                args = args?.let(::Bundle),
                navOptions = navOptions
            )
        )
    }

    fun navigate(direction: NavDirections) {
        _naviDirection.value = Event(direction)
    }

    fun navigate(action: ActionNavigate) {
        _actionDestination.value = Event(action)
    }

    fun back(
        destinationId: Int? = null,
        inclusive: Boolean = false
    ) {
        _actionBack.value = Event(
            ActionBack(
                destinationId = destinationId,
                inclusive = inclusive
            )
        )
    }

    fun backTo(destinationId: Int, inclusive: Boolean = false) {
        back(destinationId = destinationId, inclusive = inclusive)
    }
}

data class ActionNavigate(
    val destination: Int,
    val args: Bundle? = null,
    val navOptions: NavOptions? = null
)

data class ActionBack(
    val destinationId: Int? = null,
    val inclusive: Boolean = false
)
