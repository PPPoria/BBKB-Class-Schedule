package com.bbkb.sc.ui.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.FragmentTableConfigBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.ui.activity.TableActivity
import com.google.gson.Gson
import com.poria.base.base.BaseFragment
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TableConfigFragment : BaseFragment<FragmentTableConfigBinding>() {
    override fun onViewBindingCreate() = FragmentTableConfigBinding.inflate(layoutInflater)
    private val vm by activityViewModels<SingleVM<TableActivity.MData>>()

    override fun initListener() = with(binding) {
        ignoreSaturdayBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            data.tableConfig.let {
                it.copy(ignoreSaturday = !it.ignoreSaturday)
            }.let {
                saveTableConfigInBackground(it)
                data.copy(tableConfig = it)
            }.also { vm.update(it) }
        }
        ignoreSundayBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            data.tableConfig.let {
                it.copy(ignoreSunday = !it.ignoreSunday)
            }.let {
                saveTableConfigInBackground(it)
                data.copy(tableConfig = it)
            }.also { vm.update(it) }
        }
        ignoreEveningBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            data.tableConfig.let {
                it.copy(ignoreEvening = !it.ignoreEvening)
            }.let {
                saveTableConfigInBackground(it)
                data.copy(tableConfig = it)
            }.also { vm.update(it) }
        }
        nameFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val data = vm.latest ?: return
                data.tableConfig.copy(
                    nameFilter = et.toString()
                ).let {
                    saveTableConfigInBackground(it)
                    data.copy(tableConfig = it)
                }.also { vm.update(it) }
            }
        })
        majorFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val data = vm.latest ?: return
                data.tableConfig.copy(
                    majorFilter = et.toString()
                ).let {
                    saveTableConfigInBackground(it)
                    data.copy(tableConfig = it)
                }.also { vm.update(it) }
            }
        })
    }.let { }

    override suspend fun refreshDataInScope() {
        
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                tableConfigMenuUi(data.tableConfig)
            }
        }
    }

    // 筛选项的UI更新
    private fun tableConfigMenuUi(tableConfig: TableConfig) {
        binding.ignoreSaturdayBtn.also {
            if (tableConfig.ignoreSaturday) {
                it.setTextColor(SCApp.app.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(SCApp.app.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.white_dim)
                )
            }
        }
        binding.ignoreSundayBtn.also {
            if (tableConfig.ignoreSunday) {
                it.setTextColor(SCApp.app.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(SCApp.app.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.white_dim)
                )
            }
        }
        binding.ignoreEveningBtn.also {
            if (tableConfig.ignoreEvening) {
                it.setTextColor(SCApp.app.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(SCApp.app.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.white_dim)
                )
            }
        }
        binding.nameFilter.apply {
            if (text.toString() != tableConfig.nameFilter)
                setText(tableConfig.nameFilter)
        }
        binding.majorFilter.apply {
            if (text.toString() != tableConfig.majorFilter)
                setText(tableConfig.majorFilter)
        }
    }

    private fun saveTableConfigInBackground(tableConfig: TableConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            Gson().toJson(tableConfig).also { str ->
                DSManager.setString(StringKeys.TABLE_CONFIG, str)
            }
        }
    }
}