package com.poria.base.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

abstract class BaseFragment<out T : ViewBinding> : Fragment() {
    val binding by lazy { onViewBindingCreate() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.apply {
        initView()
        initListener()
    }.root

    abstract fun onViewBindingCreate(): T
    open fun initView() {}
    open fun initListener() {}
    /**
     * 由 activity 进行调用刷新与监听数据
     */
    open suspend fun refreshDataInScope() {}
    open suspend fun observeDataInScope() {}
}