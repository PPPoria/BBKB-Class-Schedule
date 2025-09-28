package com.bbkb.sc.activity


import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.bbkb.sc.BuildConfig
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityMainBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.schedule.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onViewBindingCreate() = ActivityMainBinding.inflate(layoutInflater)

    /*override fun initWindowInsets(l: Int, t: Int, r: Int, b: Int) {
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
    }*/

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
        "${
          if (BuildConfig.DEBUG) "测试版"
          else "正式版"
        } v${BuildConfig.VERSION_NAME}".also {
            binding.version.text = it
        }

    }

    override fun initListener() = binding.apply {
        userSettingsBtn.setOnClickListenerWithClickAnimation {
            Intent(
                this@MainActivity,
                UserSettingsActivity::class.java
            ).also { startActivity(it) }
        }
        updateCoursesBtn.setOnClickListenerWithClickAnimation {
            onUpdateCoursesBtnClick()
        }
        myNotesLayout.setOnClickListenerWithClickAnimation {
            Intent(
                this@MainActivity,
                NoteCategoryListActivity::class.java
            ).also { startActivity(it) }
        }
        classScheduleLayout.setOnClickListenerWithClickAnimation {
            Intent(
                this@MainActivity,
                TableActivity::class.java
            ).also { startActivity(it) }
        }
    }.let { }

    private fun onUpdateCoursesBtnClick() = lifecycleScope.launch {
        val zc = ScheduleUtils.getZC(System.currentTimeMillis())
        val id = DSManager
            .getString(StringKeys.SCHOOL_NAME, "--")
            .first().let { name ->
                School.dataList.find { it.name == name } ?: run {
                    SCToast.show(getString(R.string.please_bind_school))
                    return@launch
                }
            }.id
        Intent(this@MainActivity, AuthActivity::class.java).apply {
            putExtra("update_zc", zc)
            putExtra("school_id", id)
            startActivity(this)
        }
    }
}