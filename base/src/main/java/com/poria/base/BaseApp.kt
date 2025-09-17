package com.poria.base

import android.app.Application

open class BaseApp : Application() {
    companion object {
        lateinit var app: BaseApp
    }

    override fun onCreate() {
        super.onCreate()
        app = this
    }
}