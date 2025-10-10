package com.bbkb.sc.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import com.bbkb.sc.databinding.ActivitySplashBinding
import com.poria.base.base.BaseActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override fun onViewBindingCreate() =  ActivitySplashBinding.inflate(layoutInflater)

    override fun initView() {
        super.initView()
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}