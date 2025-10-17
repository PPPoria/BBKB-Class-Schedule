package com.bbkb.sc.ui.fragment

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.FragmentTableConfigBinding
import com.bbkb.sc.databinding.ItemTableOptionBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.isNightModeYes
import com.bbkb.sc.schedule.TableAttr
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.ui.activity.TableActivity
import com.bbkb.sc.ui.dialog.SeekBarDialog
import com.bbkb.sc.util.FileManager
import com.poria.base.adapter.SingleBindingAdapter
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
    private val options = TableAttr.latest.run {
        listOf(
            Option(
                1,
                R.string.x_axis_day_of_week_text_size_scale,
                xAxisDayOfWeekTextSizeScale.toPercentage()
            ),
            Option(2, R.string.x_axis_date_text_size_scale, xAxisDateTextSizeScale.toPercentage()),
            Option(3, R.string.x_axis_height_scale, xAxisHeightScale.toPercentage()),
            Option(
                4,
                R.string.y_axis_node_number_text_size_scale,
                yAxisNodeNumberTextSizeScale.toPercentage()
            ),
            Option(5, R.string.y_axis_time_text_size_scale, yAxisTimeTexSizeStScale.toPercentage()),
            Option(6, R.string.y_axis_width_scale, yAxisWidthScale.toPercentage()),
            Option(7, R.string.course_name_text_size_scale, courseNameTextSizeScale.toPercentage()),
            Option(
                8,
                R.string.course_teacher_and_room_text_size_scale,
                courseTeacherAndRoomTextSizeScale.toPercentage()
            ),
            Option(9, R.string.table_height_scale, tableHeightScale.toPercentage()),
            Option(10, R.string.table_bg_img_mask_color, tableBgImgMaskColor),
            Option(11, R.string.table_bg_img_mask_alpha, tableBgImgMaskAlpha.toPercentage()),
            Option(12, R.string.course_color_h_hue_offset, courseColorHHueOffset),
            Option(13, R.string.course_color_s_hue_base, courseColorSHueBase),
            Option(14, R.string.course_color_l_hue_base, courseColorLHueBase),
        )
    }
    private val sizeOptionsAdapter by lazy {
        SingleBindingAdapter<ItemTableOptionBinding, Option>(
            R.layout.item_table_option,
            ItemTableOptionBinding::bind,
        ) { binding, position, item, adapter ->
            binding.optionText.text = getString(item.nameId)
            binding.optionValue.text = item.value.toString()
            val onValueChanged: ((Int) -> Unit) = {
                item.value = it
                binding.optionValue.text = it.toString()
            }
            binding.bg.setOnClickListener {
                showSeekBarDialog(
                    item.id, 0 to 200,
                    item.value, onValueChanged
                )
            }
        }
    }
    private val colorOptionsAdapter by lazy {
        SingleBindingAdapter<ItemTableOptionBinding, Option>(
            R.layout.item_table_option,
            ItemTableOptionBinding::bind,
        ) { binding, position, item, adapter ->
            binding.optionText.text = getString(item.nameId)
            binding.optionValue.text = item.value.toString()
            val onValueChanged: ((Int) -> Unit) = {
                item.value = it
                binding.optionValue.text = it.toString()
            }
            binding.bg.setOnClickListener {
                showSeekBarDialog(
                    item.id,
                    when (item.id) {
                        10L -> return@setOnClickListener // 有bug，暂时不用
                        12L -> 0 to 359
                        else -> 0 to 100
                    },
                    item.value, onValueChanged
                )
            }
        }
    }

    private fun Float.toPercentage() = (this * 100).toInt()

    override fun initView() {
        binding.tableSizeRv.apply {
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context)
            adapter = sizeOptionsAdapter
            sizeOptionsAdapter.data = options.filter { it.id < 10 }
        }
        binding.courseColorRv.apply {
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context)
            adapter = colorOptionsAdapter
            colorOptionsAdapter.data = options.filter { it.id >= 10 }
        }
    }

    override fun initListener() {
        initFilterListener()
        initBgImgBtnListener()
    }

    private fun showSeekBarDialog(
        optionId: Long, ran: Pair<Int, Int>, value: Int,
        onValueChanged: ((Int) -> Unit)?
    ) {
        val dialog = SeekBarDialog().apply {
            title = this@TableConfigFragment.getString(options.find { it.id == optionId }!!.nameId)
            minValue = ran.first
            maxValue = ran.second
            curValue = value
            onValueChangedListener = {
                onValueChanged?.invoke(it)
                updateTableAttr(optionId, it)
            }
            onDismissListener = {
                this@TableConfigFragment.binding.main.isVisible = true
            }
        }
        binding.main.isVisible = false
        dialog.show(
            this@TableConfigFragment.requireActivity().supportFragmentManager,
            "seek_bar_dialog"
        )
    }

    private fun updateTableAttr(optionId: Long, value: Int) {
        vm.latest?.let { data ->
            val oldAttr = data.tableAttr
            when (optionId) {
                1L -> oldAttr.copy(xAxisDayOfWeekTextSizeScale = value.toFloat() / 100)
                2L -> oldAttr.copy(xAxisDateTextSizeScale = value.toFloat() / 100)
                3L -> oldAttr.copy(xAxisHeightScale = value.toFloat() / 100)
                4L -> oldAttr.copy(yAxisNodeNumberTextSizeScale = value.toFloat() / 100)
                5L -> oldAttr.copy(yAxisTimeTexSizeStScale = value.toFloat() / 100)
                6L -> oldAttr.copy(yAxisWidthScale = value.toFloat() / 100)
                7L -> oldAttr.copy(courseNameTextSizeScale = value.toFloat() / 100)
                8L -> oldAttr.copy(courseTeacherAndRoomTextSizeScale = value.toFloat() / 100)
                9L -> oldAttr.copy(tableHeightScale = value.toFloat() / 100)
                /*10L -> oldAttr.copy(tableBgImgMaskColor = value)*/ // 有bug，暂时不用
                11L -> oldAttr.copy(tableBgImgMaskAlpha = value.toFloat() / 100)
                12L -> oldAttr.copy(courseColorHHueOffset = value)
                13L -> oldAttr.copy(courseColorSHueBase = value)
                14L -> oldAttr.copy(courseColorLHueBase = value)
                else -> null
            }?.let { vm.update(data.copy(tableAttr = it)) }
        }
    }

    private fun initFilterListener() = with(binding) {
        ignoreSaturdayBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            TableConfig.latest.let {
                it.copy(ignoreSaturday = !it.ignoreSaturday)
            }.let {
                TableConfig.latest = it
                data.copy(lastUpdateTime = System.currentTimeMillis())
            }.also { vm.update(it) }
        }
        ignoreSundayBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            TableConfig.latest.let {
                it.copy(ignoreSunday = !it.ignoreSunday)
            }.let {
                TableConfig.latest = it
                data.copy(lastUpdateTime = System.currentTimeMillis())
            }.also { vm.update(it) }
        }
        ignoreEveningBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            TableConfig.latest.let {
                it.copy(ignoreEvening = !it.ignoreEvening)
            }.let {
                TableConfig.latest = it
                data.copy(lastUpdateTime = System.currentTimeMillis())
            }.also { vm.update(it) }
        }
        nameFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val data = vm.latest ?: return
                TableConfig.latest.copy(
                    nameFilter = et.toString()
                ).let {
                    TableConfig.latest = it
                    data.copy(lastUpdateTime = System.currentTimeMillis())
                }.also { vm.update(it) }
            }
        })
        majorFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val data = vm.latest ?: return
                TableConfig.latest.copy(
                    majorFilter = et.toString()
                ).let {
                    TableConfig.latest = it
                    data.copy(lastUpdateTime = System.currentTimeMillis())
                }.also { vm.update(it) }
            }
        })
    }

    private fun initBgImgBtnListener() = with(binding) {
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
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect {
                tableConfigMenuUi(TableConfig.latest)
            }
        }
    }

    // 筛选项的UI更新
    private fun tableConfigMenuUi(tableConfig: TableConfig) {
        binding.main.setOnClickListener { /* do nothing */ }
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

    data class Option(
        val id: Long,
        val nameId: Int,
        var value: Int,
    )
}
