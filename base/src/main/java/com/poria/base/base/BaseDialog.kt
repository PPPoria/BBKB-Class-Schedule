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
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

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
        lifecycleScope.launch {
            refreshDataInScope()
            initView()
            initListener()
            observeDataInScope()
        }
    }.root

    abstract fun onViewBindingCreate(): T
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
    open fun initView() {}
    open fun initListener() {}
    open suspend fun refreshDataInScope() {}
    open suspend fun observeDataInScope() {}
}