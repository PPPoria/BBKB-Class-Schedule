package com.bbkb.sc


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