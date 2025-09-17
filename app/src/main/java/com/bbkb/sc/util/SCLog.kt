package com.bbkb.sc.util

import android.util.Log
import com.bbkb.sc.BuildConfig

object SCLog {
    fun debug(TAG: String, message: String) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "debug:\n$message")
    }

    fun error(TAG: String, e: Throwable) {
        Log.e(TAG, "ERROR: ", e)
    }

}