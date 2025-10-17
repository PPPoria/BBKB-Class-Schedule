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
    private var isRestarted = false

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

    override fun onStart() {
        super.onStart()
        if (isRestarted) {
            lifecycleScope.launch {
                refreshDataInScope()
            }
        } else isRestarted = true
    }

    abstract fun onViewBindingCreate(): T
    open fun initView() {}
    open fun initListener() {}
    open suspend fun refreshDataInScope() {}
    open suspend fun observeDataInScope() {}
}