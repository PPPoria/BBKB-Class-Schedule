package com.bbkb.sc.ui.activity

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.bbkb.sc.BuildConfig
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityMainBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.store.DSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onViewBindingCreate() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        binding.version.text = StringBuilder().apply {
            if (BuildConfig.DEBUG) append("测试版")
            else if (BuildConfig.VERSION_CODE % 10 != 0) append("修订版")
            else append("正式版")
            append(" v")
            append(BuildConfig.VERSION_NAME)
        }.toString()
    }

    override fun initListener() = binding.apply {
        userSettingsBtn.setOnClickListener {
            Intent(
                this@MainActivity,
                UserSettingsActivity::class.java
            ).also { startActivity(it) }
        }
        updateCoursesBtn.setOnClickListener {
            onUpdateCoursesBtnClick()
        }
        myNotesLayout.setOnClickListener {
            Intent(
                this@MainActivity,
                NoteCategoryListActivity::class.java
            ).also { startActivity(it) }
        }
        classScheduleLayout.setOnClickListener {
            onClassScheduleLayoutClick()
        }
    }.let { }

    private fun onUpdateCoursesBtnClick() = lifecycleScope.launch {
        val id = withContext(Dispatchers.IO) {
            DSManager.run {
                getString(StringKeys.SCHOOL_NAME).first()
            }.let { name ->
                School.dataList.find { it.name == name }
            }
        }.also {
            if (it == null) {
                SCToast.show(getString(R.string.please_bind_school))
                return@launch
            }
        }.let { it!!.id }
        val zc = ScheduleUtils.getZC(System.currentTimeMillis())
        Intent(this@MainActivity, AuthActivity::class.java).apply {
            putExtra(AuthActivity.KEY_UPDATE_ZC, zc)
            putExtra(AuthActivity.KEY_SCHOOL_ID, id)
            startActivity(this)
        }
    }

    private fun onClassScheduleLayoutClick() = lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            DSManager.run {
                getString(StringKeys.SCHOOL_NAME).first()
            }.let { name ->
                School.dataList.find { it.name == name }
            }
        }.also {
            if (it == null) {
                SCToast.show(getString(R.string.please_bind_school))
                return@launch
            }
        }.run {
            Intent(
                this@MainActivity,
                TableActivity::class.java
            ).also { startActivity(it) }
        }
    }
}