package com.bbkb.sc.util

import android.widget.Toast
import com.bbkb.sc.BuildConfig
import com.bbkb.sc.SCApp

object SCToast {
    fun show(message: String, isShort: Boolean = true) {
        Toast.makeText(
            SCApp.app, message,
            if (isShort) Toast.LENGTH_SHORT
            else Toast.LENGTH_LONG
        ).show()
    }

    fun debug(message: String) {
        if (BuildConfig.DEBUG) show(message)
    }
}