package com.bbkb.sc


import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.bbkb.sc.datastore.IntKeys
import com.poria.base.BaseApp
import com.poria.base.store.DSManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SCApp : BaseApp() {
    companion object {
        lateinit var app: SCApp
        val isDarkTheme: Boolean
            get() = app.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        BaseApp.app = this
        val mode = runBlocking {
            DSManager.getInt(
                IntKeys.NIGHT_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ).first()
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}