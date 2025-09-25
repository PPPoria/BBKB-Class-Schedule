package com.poria.base.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SingleVM<T> : ViewModel() {
    private val mutableFlow: MutableStateFlow<T> by lazy {
        MutableStateFlow(latestData.value!!)
    }
    private val latestData by lazy { MutableLiveData<T>() }
    val flow by lazy { mutableFlow.asStateFlow() }

    fun update(value: T) = CoroutineScope(Dispatchers.Main).launch {
        latestData.value = value
        mutableFlow.value = value
        mutableFlow.emit(value)
    }.let { }

    fun latest(): T? = mutableFlow.value
}