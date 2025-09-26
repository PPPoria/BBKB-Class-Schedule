package com.poria.base.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Objects

class SingleVM<T> : ViewModel() {
    private var latestState: T? = null
    private val mutableFlow by lazy { MutableStateFlow<T>(latestState!!) }
    val flow by lazy { mutableFlow.asStateFlow() }

    fun update(value: T) = synchronized(this) {
        latestState = value
        mutableFlow.value = value
    }

    fun latest(): T? = latestState
}