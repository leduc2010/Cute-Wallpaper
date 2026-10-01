package com.cute.wallpaper.ringtones.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    private lateinit var _binding: VB
    protected val binding: VB get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        initView()
        initListener()
        observeData()
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB
    protected open fun initView() = Unit
    protected open fun initListener() = Unit
    protected open fun observeData() = Unit
}
