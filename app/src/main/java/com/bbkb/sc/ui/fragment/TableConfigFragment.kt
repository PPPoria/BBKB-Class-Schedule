package com.bbkb.sc.ui.fragment

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.FragmentTableConfigBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.isNightModeYes
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.ui.activity.TableActivity
import com.bbkb.sc.util.FileManager
import com.google.gson.Gson
import com.poria.base.base.BaseFragment
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID


class TableConfigFragment : BaseFragment<FragmentTableConfigBinding>() {
    override fun onViewBindingCreate() = FragmentTableConfigBinding.inflate(layoutInflater)
    private val vm by activityViewModels<SingleVM<TableActivity.MData>>()
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        callback = { uri ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (uri == null) return@launch
                val path = requireContext().contentResolver.openInputStream(uri)?.use { ins ->
                    val file =
                        File(requireContext().filesDir, "multi_pics/${UUID.randomUUID()}.jpg")
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { out -> ins.copyTo(out) }
                    file.absolutePath   // 内部路径
                } ?: return@launch
                CoroutineScope(Dispatchers.Default).launch {
                    val oldPath = DSManager.getString(
                        StringKeys.TABLE_BACKGROUND_IMG_PATH,
                        ""
                    ).first()
                    if (oldPath.isNotEmpty() && oldPath.isNotBlank()) {
                        FileManager.deleteInnerImage(oldPath)
                    }
                    DSManager.setString(StringKeys.TABLE_BACKGROUND_IMG_PATH, path)
                    withContext(Dispatchers.Main) {
                        activity?.recreate()
                    }
                }
            }
        }
    )

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
        changeBackgroundImgBtn.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }
        cancelBackgroundImgBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                val oldPath = DSManager.getString(
                    StringKeys.TABLE_BACKGROUND_IMG_PATH,
                    ""
                ).first()
                if (oldPath.isNotEmpty() && oldPath.isNotBlank()) {
                    FileManager.deleteInnerImage(oldPath)
                } else return@launch
                DSManager.setString(StringKeys.TABLE_BACKGROUND_IMG_PATH, "")
                withContext(Dispatchers.Main) {
                    activity?.recreate()
                }
            }
        }
    }.let { }

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
                if (requireActivity().isNightModeYes()) {
                    it.setTextColor(SCApp.app.getColor(R.color.black))
                    it.backgroundTintList = ColorStateList.valueOf(
                        SCApp.app.getColor(R.color.gray)
                    )
                } else {
                    it.setTextColor(SCApp.app.getColor(R.color.gray))
                    it.backgroundTintList = ColorStateList.valueOf(
                        SCApp.app.getColor(R.color.white_dim)
                    )
                }
            }
        }
        binding.ignoreSundayBtn.also {
            if (tableConfig.ignoreSunday) {
                it.setTextColor(SCApp.app.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.primary)
                )
            } else {
                if (requireActivity().isNightModeYes()) {
                    it.setTextColor(SCApp.app.getColor(R.color.black))
                    it.backgroundTintList = ColorStateList.valueOf(
                        SCApp.app.getColor(R.color.gray)
                    )
                } else {
                    it.setTextColor(SCApp.app.getColor(R.color.gray))
                    it.backgroundTintList = ColorStateList.valueOf(
                        SCApp.app.getColor(R.color.white_dim)
                    )
                }
            }
        }
        binding.ignoreEveningBtn.also {
            if (tableConfig.ignoreEvening) {
                it.setTextColor(SCApp.app.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    SCApp.app.getColor(R.color.primary)
                )
            } else {
                if (requireActivity().isNightModeYes()) {
                    it.setTextColor(SCApp.app.getColor(R.color.black))
                    it.backgroundTintList = ColorStateList.valueOf(
                        SCApp.app.getColor(R.color.gray)
                    )
                } else {
                    it.setTextColor(SCApp.app.getColor(R.color.gray))
                    it.backgroundTintList = ColorStateList.valueOf(
                        SCApp.app.getColor(R.color.white_dim)
                    )
                }
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