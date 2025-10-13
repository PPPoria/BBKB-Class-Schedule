package com.bbkb.sc.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityWebBinding
import com.poria.base.base.BaseActivity

class WebActivity : BaseActivity<ActivityWebBinding>(){
    override fun onViewBindingCreate() = ActivityWebBinding.inflate(layoutInflater)

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
        intent.getStringExtra("url")?.let {
            showWeb(it)
        } ?: throw IllegalArgumentException("No URL provided")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showWeb(url: String) = with(binding.webView) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        loadUrl(url)
    }

    companion object {
        fun startPrivacyPolicy(activity: AppCompatActivity) {
            Intent(activity, WebActivity::class.java).apply {
                putExtra("url", "file:///android_asset/privacy_policy.html")
                activity.startActivity(this)
            }
        }
    }
}