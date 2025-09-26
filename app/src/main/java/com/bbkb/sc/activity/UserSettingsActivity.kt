package com.bbkb.sc.activity

import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.bbkb.sc.databinding.ActivityUserSettingsBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.dialog.SchoolSelectorDialog
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.flow.first

class UserSettingsActivity : BaseActivity<ActivityUserSettingsBinding>() {
    override fun onViewBindingCreate() = ActivityUserSettingsBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
    }

    override fun initListener() = binding.apply {
        bindSchoolBtn.setOnClickListenerWithClickAnimation {
            SchoolSelectorDialog().also {
                it.schoolName = vm.latest()?.schoolName ?: "--"
                it.show(supportFragmentManager, "SchoolSelectorDialog")
            }
        }
        privacyPolicyBtn.setOnClickListenerWithClickAnimation {

        }
    }.let { }

    override suspend fun observeDataInScope() = vm.flow
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect { data ->
            binding.bindSchoolName.text = data.schoolName
        }

    override suspend fun refreshDataInScope() {
        val data = vm.latest() ?: MData()
        data.schoolName = DSManager
            .getString(StringKeys.SCHOOL_NAME, defaultValue = "--")
            .first()
        vm.update(data)
    }

    data class MData(
        var schoolName: String = "--",
    )
}