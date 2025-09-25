package com.poria.base.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Objects

class SingleVM<T> : ViewModel() {
    private var latestState: State<T>? = null
    private val mutableFlow by lazy { MutableStateFlow(latestState!!) }
    val flow by lazy { mutableFlow.asStateFlow() }

    fun update(value: T, timeStamp: Long = System.currentTimeMillis()) {
        synchronized(this) {
            latestState = State(
                timeStamp = timeStamp,
                value = value
            )
            mutableFlow.value = latestState!!
        }
    }

    fun latest(): T? = latestState?.value

    data class State<T> (
        var timeStamp: Long,
        var value: T
    ) {
        override fun equals(other: Any?): Boolean {
            return other is State<*> && other.timeStamp == timeStamp
        }

        override fun hashCode(): Int {
            return Objects.hash(timeStamp)
        }
    }
}