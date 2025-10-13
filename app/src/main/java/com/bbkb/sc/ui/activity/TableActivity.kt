package com.bbkb.sc.ui.activity

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.ui.widget.TableView
import com.bbkb.sc.databinding.ActivityTableBinding
import com.bbkb.sc.datastore.LongKeys
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.ui.dialog.CourseDetailDialog
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.util.SCToast
import com.google.gson.Gson
import com.poria.base.base.BaseActivity
import com.poria.base.ext.DateFormat
import com.poria.base.ext.genMacaronColor
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.ext.toTimeStamp
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TableActivity : BaseActivity<ActivityTableBinding>() {
    override fun onViewBindingCreate() = ActivityTableBinding.inflate(layoutInflater)
    private val mode by lazy { intent.getIntExtra(KEY_MODE, MODE_NORMAL) }
    private val addedCategoryId by lazy { intent.getLongExtra(KEY_NOTE_CATEGORY_ID, 0L) }
    private val vm by viewModels<SingleVM<MData>>()

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
    }

    override fun initListener() = with(binding) {
        onBackPressedDispatcher.addCallback {
            if (filterBg.isVisible) filterBg.isVisible = false
            else finish()
        }
        moreBtn.setOnClickListenerWithClickAnimation { _ ->
            filterBg.also { it.isVisible = !it.isVisible }
        }
        filterBg.setOnClickListener { filterBg.isVisible = false }
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
        tablePreBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            lifecycleScope.launch {
                data.copy(
                    tableZC = (data.tableZC - 1 + data.schoolData.weekNum)
                            % data.schoolData.weekNum,
                    courses = withContext(Dispatchers.IO) {
                        CourseDB.get().dao()
                            .getByZC(data.tableZC - 1)
                            .first()
                    }
                ).also { vm.update(it) }
            }
        }
        tableNextBtn.setOnClickListenerWithClickAnimation {
            val data = vm.latest ?: return@setOnClickListenerWithClickAnimation
            lifecycleScope.launch {
                data.copy(
                    tableZC = (data.tableZC + 1) % data.schoolData.weekNum,
                    courses = withContext(Dispatchers.IO) {
                        CourseDB.get().dao()
                            .getByZC(data.tableZC + 1)
                            .first()
                    }
                ).also { vm.update(it) }
            }
        }
    }.let { }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: DSManager.run {
            getString(StringKeys.SCHOOL_NAME).first()
        }.let { name ->
            School.dataList.find { it.name == name }
        }.also {
            if (it == null) {
                SCToast.show(getString(R.string.please_bind_school))
                return
            }
        }.let { MData(schoolData = it!!.copy()) }
        val curZC = ScheduleUtils.getZC(System.currentTimeMillis())
        val tableZC = if (old.tableZC == 0) curZC else old.tableZC
        old.copy(
            curZC = curZC,
            tableZC = tableZC,
            courses = withContext(Dispatchers.IO) {
                CourseDB.get().dao().getByZC(tableZC).first()
            },
            tableConfig = withContext(Dispatchers.IO) {
                ScheduleUtils.getTableConfig()
            }
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                tableConfigMenuUi(data.tableConfig)
                System.currentTimeMillis().toDateFormat().run {
                    "$year/$month/$day"
                }.also { binding.todayDate.text = it }
                "第${data.curZC}周".also { binding.todayZc.text = it }
                "第${data.tableZC}周".also { binding.zc.text = it }
                showTable(
                    data.tableZC,
                    data.courses,
                    data.tableConfig
                )
            }
        }
    }

    // 筛选项的UI更新
    private fun tableConfigMenuUi(tableConfig: TableConfig) {
        binding.ignoreSaturdayBtn.also {
            if (tableConfig.ignoreSaturday) {
                it.setTextColor(this@TableActivity.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(this@TableActivity.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.white_dim)
                )
            }
        }
        binding.ignoreSundayBtn.also {
            if (tableConfig.ignoreSunday) {
                it.setTextColor(this@TableActivity.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(this@TableActivity.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.white_dim)
                )
            }
        }
        binding.ignoreEveningBtn.also {
            if (tableConfig.ignoreEvening) {
                it.setTextColor(this@TableActivity.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(this@TableActivity.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.white_dim)
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

    private fun showTable(
        tableZC: Int,
        courses: List<Course>,
        tableConfig: TableConfig
    ) = lifecycleScope.launch {
        val sd = vm.latest?.schoolData ?: return@launch
        val cells = courses.asSequence().filter {
            if (tableConfig.ignoreSaturday) it.xq != 6
            else true
        }.filter {
            if (tableConfig.ignoreSunday) it.xq != 7
            else true
        }.filter {
            if (tableConfig.ignoreEvening) it.startNode < (sd.nodesPerDay - sd.nodesInEvening)
            else true
        }.filter {
            tableConfig.nameFilter.isEmpty() || it.name.contains(tableConfig.nameFilter)
        }.filter {
            tableConfig.majorFilter.isEmpty() || it.major.contains(tableConfig.majorFilter)
        }.mapIndexed { index, course ->
            TableView.Cell(
                id = index,
                title = course.name,
                content = course.run {
                    "$teacher\n$classroom"
                },
                color = course.name.genMacaronColor(),
                row = course.run { startNode to endNode },
                column = course.run { xq to xq }
            )
        }.toList()
        val oneDay = 86_400_000L
        val oneWeek = oneDay * 7
        val tableZcMondayTimeStamp =
            if (courses.isNotEmpty()) {
                courses.first().let {
                    val offset = (it.xq - 1) * oneDay
                    it.timeStamp - offset
                }
            } else {
                runBlocking {
                    DSManager.getLong(
                        LongKeys.FIRST_ZC_MONDAY_TIME_STAMP,
                        System.currentTimeMillis()
                    ).first().let {
                        (tableZC - 1) * oneWeek + it
                    }
                }
            }
        val rows =
            if (tableConfig.ignoreEvening) sd.nodesPerDay - sd.nodesInEvening
            else sd.nodesPerDay
        val columns =
            if (tableConfig.ignoreSaturday && tableConfig.ignoreSunday) 5
            else if (tableConfig.ignoreSaturday || tableConfig.ignoreSunday) 6
            else 7
        val xAxis = ArrayList<String>().also {
            for (i in 0 until 7) {
                it.add(
                    "${
                        when (i) {
                            0 -> "星期一"
                            1 -> "星期二"
                            2 -> "星期三"
                            3 -> "星期四"
                            4 -> "星期五"
                            5 -> "星期六"
                            6 -> "星期日"
                            else -> ""
                        }
                    }\n${
                        (tableZcMondayTimeStamp + oneDay * i)
                            .toDateFormat().run { "$month.$day" }
                    }"
                )
            }
            if (tableConfig.ignoreSunday) it.removeAt(6)
            if (tableConfig.ignoreSaturday) it.removeAt(5)
        }
        val yAxis = ArrayList<String>().also {
            for (i in 0 until rows) {
                it.add(
                    (i + 1).toString()
                )
            }
        }
        binding.table.update(
            rows = rows,
            columns = columns,
            cells = cells,
            xAxis = xAxis,
            yAxis = yAxis,
            highlightX = vm.latest?.run {
                if (tableZC == curZC) System.currentTimeMillis()
                    .toDateFormat()
                    .let { today ->
                        val todayTimeStamp = DateFormat(
                            year = today.year,
                            month = today.month,
                            day = today.day
                        ).toTimeStamp()
                        val offset = (todayTimeStamp - tableZcMondayTimeStamp) / oneDay
                        (offset + 1).toInt()
                    }
                else 0
            } ?: 0,
            listener = this@TableActivity::onClickTableItem
        )
    }

    private fun onClickTableItem(cell: TableView.Cell) {
        if (vm.latest == null) return
        if (mode == MODE_NORMAL) {
            val course = vm.latest!!.courses.find {
                it.name == cell.title
            }!!
            CourseDetailDialog().also {
                it.course = course
                it.show(supportFragmentManager, "course_detail_dialog")
            }
        } else if (mode == MODE_ADD_RELATED_NOTE) {
            CoroutineScope(Dispatchers.IO).launch {
                val category = NoteCategoryDB.get().dao().getById(addedCategoryId).first()
                val courseNames = category.courseNames.toMutableList()
                courseNames.add(cell.title!!)
                NoteCategoryDB.get().dao().update(category.copy(courseNames = courseNames))
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun saveTableConfigInBackground(tableConfig: TableConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            Gson().toJson(tableConfig).also { str ->
                DSManager.setString(StringKeys.TABLE_CONFIG, str)
            }
        }
    }

    data class MData(
        val schoolData: School.SchoolData,
        val curZC: Int = 0,
        val tableZC: Int = 0,
        val courses: List<Course> = emptyList(),
        val tableConfig: TableConfig = TableConfig()
    )

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_NOTE_CATEGORY_ID = "category_id"
        const val MODE_NORMAL = 0
        const val MODE_ADD_RELATED_NOTE = 1
    }
}