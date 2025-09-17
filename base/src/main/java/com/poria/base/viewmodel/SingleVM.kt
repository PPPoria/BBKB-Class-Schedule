package com.poria.base.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SingleVM<T>: ViewModel() {
    val cur: MutableLiveData<T> by lazy {
        MutableLiveData<T>()
    }
}