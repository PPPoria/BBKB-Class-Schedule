package com.poria.base.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SingleVM<T>: ViewModel() {
    val cur: MutableLiveData<T> by lazy {
        MutableLiveData<T>()
    }
    private val mutableFlow: MutableStateFlow<T> by lazy {
        MutableStateFlow(cur.value!!)
    }
    val flow = mutableFlow.asStateFlow()
}