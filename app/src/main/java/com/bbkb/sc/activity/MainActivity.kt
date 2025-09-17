package com.bbkb.sc.activity


import android.content.Intent
import com.bbkb.sc.databinding.ActivityMainBinding
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onViewBindingCreate() = ActivityMainBinding.inflate(layoutInflater)

    override fun initWindowInsets(l: Int, t: Int, r: Int, b: Int) {
        super.initWindowInsets(l, t, r, b)
        binding.root.setPadding(
            systemBarPadding[l],
            systemBarPadding[t],
            systemBarPadding[r],
            0
        )
        binding.contentLayout.setPadding(
            0, 0, 0,
            systemBarPadding[b]
        )
    }

    override fun initView() {
        setLightStatusBar(false)
        setLightNavigationBar(true)
    }

    override fun initListener() = binding.apply {
        userSettingsBtn.setOnClickListenerWithClickAnimation { goToUserSettings() }
        myNotesLayout.setOnClickListenerWithClickAnimation { goToMyNotes() }
        classScheduleLayout.setOnClickListenerWithClickAnimation { goToClassSchedule() }
    }.let { }

    private fun goToUserSettings() {
        Intent(this, UserSettingsActivity::class.java)
            .also { startActivity(it) }
    }

    private fun goToMyNotes() {

    }

    private fun goToClassSchedule() {
        Intent(this, TableActivity::class.java)
            .also { startActivity(it) }
    }
}