package com.bbkb.sc

import android.content.Context
import android.content.res.Configuration
import com.poria.base.BaseApp

class SCApp : BaseApp() {
    companion object {
        lateinit var app: SCApp
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        BaseApp.app = this
    }
}

fun Context.isNightModeYes() = resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES