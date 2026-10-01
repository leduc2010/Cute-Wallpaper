package com.cute.wallpaper.ringtones.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.cute.wallpaper.ringtones.presentation.navigation.NavigationViewModel

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    protected val navViewModel: NavigationViewModel by activityViewModels()

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "Binding is only valid between onCreateView and onDestroyView" }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflateBinding(inflater, container).also { _binding = it }.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
        observeData()
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    protected open fun initView() = Unit
    protected open fun initListener() = Unit
    protected open fun observeData() = Unit

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
