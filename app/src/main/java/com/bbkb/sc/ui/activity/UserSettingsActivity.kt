package com.bbkb.sc.ui.activity

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityUserSettingsBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.isNightModeYes
import com.bbkb.sc.schedule.StartPreference
import com.bbkb.sc.ui.dialog.SchoolSelectorDialog
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class UserSettingsActivity : BaseActivity<ActivityUserSettingsBinding>() {
    override fun onViewBindingCreate() = ActivityUserSettingsBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()

    override fun initListener() = binding.apply {
        bindSchoolBtn.setOnClickListenerWithClickAnimation {
            SchoolSelectorDialog().also {
                it.schoolName = vm.latest?.schoolName ?: "--"
                it.show(supportFragmentManager, "SchoolSelectorDialog")
            }
        }
        privacyPolicyBtn.setOnClickListenerWithClickAnimation {
            WebActivity.startPrivacyPolicy(this@UserSettingsActivity)
        }
        goToMain.setOnClickListenerWithClickAnimation {
            vm.latest?.run {
                StartPreference.latest = StartPreference.latest.copy(
                    goToMainWhenStartApp = true,
                    goToNoteWhenStartApp = false,
                    goToTableWhenStartApp = false,
                )
                copy(lastUpdatedTime = System.currentTimeMillis())
            }?.run { vm.update(this) }
        }
        goToNote.setOnClickListenerWithClickAnimation {
            vm.latest?.run {
                StartPreference.latest = StartPreference.latest.copy(
                    goToMainWhenStartApp = false,
                    goToNoteWhenStartApp = true,
                    goToTableWhenStartApp = false,
                )
                copy(lastUpdatedTime = System.currentTimeMillis())
            }?.run { vm.update(this) }
        }
        goToTable.setOnClickListenerWithClickAnimation {
            vm.latest?.run {
                StartPreference.latest = StartPreference.latest.copy(
                    goToMainWhenStartApp = false,
                    goToNoteWhenStartApp = false,
                    goToTableWhenStartApp = true,
                )
                copy(lastUpdatedTime = System.currentTimeMillis())
            }?.run { vm.update(this) }
        }
    }.let { }

    private fun TextView.setUnselectedColor() {
        if (isNightModeYes()) {
            alpha = 1f
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.gray))
            setTextColor(getColor(R.color.black))
        } else {
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.white_dim))
            setTextColor(getColor(R.color.black))
        }
    }

    private fun TextView.setSelectedColor() {
        backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
        setTextColor(getColor(R.color.white))
        if (isNightModeYes()) {
            alpha = 0.9f
        }
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: MData()
        old.copy(
            schoolName = withContext(Dispatchers.IO) {
                DSManager.getString(
                    StringKeys.SCHOOL_NAME,
                    defaultValue = "--"
                ).first()
            }
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                binding.bindSchoolName.text = data.schoolName
                binding.startPreferenceLayout.isGone = data.schoolName == "--"
                StartPreference.latest.run {
                    if (goToMainWhenStartApp) binding.goToMain.setSelectedColor()
                    else binding.goToMain.setUnselectedColor()
                    if (goToNoteWhenStartApp) binding.goToNote.setSelectedColor()
                    else binding.goToNote.setUnselectedColor()
                    if (goToTableWhenStartApp) binding.goToTable.setSelectedColor()
                    else binding.goToTable.setUnselectedColor()
                }
            }
        }
    }

    data class MData(
        val lastUpdatedTime: Long = System.currentTimeMillis(),
        val schoolName: String = "--",
    )
}