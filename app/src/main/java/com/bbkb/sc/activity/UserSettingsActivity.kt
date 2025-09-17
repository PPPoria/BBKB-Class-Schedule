package com.bbkb.sc.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityUserSettingsBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.dialog.SchoolSelectorDialog
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSHelper
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.MainScope
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

    override fun refreshDataWhenOnStart() = MainScope().launch {
        vm.cur.value ?: MData().also { data ->
            data.schoolName = DSHelper
                .getString(StringKeys.SCHOOL_NAME, defaultValue = "--")
                .first()
            vm.cur.value = data
        }
    }.let { }

    data class MData(
        var schoolName: String = "--",
    )
}