package com.bbkb.sc.activity

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bbkb.sc.databinding.ActivityUserSettingsBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.dialog.SchoolSelectorDialog
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                it.schoolName = vm.cur.value?.schoolName ?: "--"
                it.show(supportFragmentManager, "SchoolSelectorDialog")
            }
        }
        privacyPolicyBtn.setOnClickListenerWithClickAnimation {

        }
    }.let { }

    override fun initObserver() = vm.cur.observe(this) { data ->
        binding.bindSchoolName.text = data.schoolName
    }

    override fun refreshDataWhenOnStart() = lifecycleScope.launch {
        val data = vm.cur.value ?: MData()
        data.schoolName = DSManager
            .getString(StringKeys.SCHOOL_NAME, defaultValue = "--")
            .first()
        vm.cur.value = data
    }.let { }

    data class MData(
        var schoolName: String = "--",
    )
}