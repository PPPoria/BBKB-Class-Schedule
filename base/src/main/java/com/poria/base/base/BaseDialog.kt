package com.poria.base.base

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class BaseDialog<out T : ViewBinding> : DialogFragment() {
    val binding by lazy { onViewBindingCreate() }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply { initWindowInsets(this) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.apply {
        initView()
        initListener()
        initData()
        initLiveData()
        initObserver()
    }.root

    abstract fun onViewBindingCreate(): T
    open fun initView() {}
    open fun initWindowInsets(
        window: Window,
        gravity: Int = Gravity.BOTTOM,
        width: Int = WindowManager.LayoutParams.MATCH_PARENT,
        height: Int = WindowManager.LayoutParams.WRAP_CONTENT
    ) = window.apply {
        val params = attributes
        params.gravity = gravity
        params.width = width
        params.height = height
        attributes = params
        setGravity(gravity)
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }.let { }

    open fun initListener() {}
    open fun initData() {}
    open fun initLiveData() {}
    open fun initObserver() {}
}