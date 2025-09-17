package com.bbkb.sc.activity

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.bbkb.sc.R
import com.bbkb.sc.widget.TableView
import com.bbkb.sc.databinding.ActivityTableBinding
import com.bbkb.sc.datastore.LongKeys
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.dialog.CourseDetailDialog
import com.bbkb.sc.schedule.School
import com.bbkb.sc.schedule.UserConfig
import com.bbkb.sc.schedule.data.Course
import com.bbkb.sc.schedule.data.CourseDB
import com.bbkb.sc.util.SCToast
import com.google.gson.Gson
import com.poria.base.base.BaseActivity
import com.poria.base.ext.genMacaronColor
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.store.DSHelper
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TableActivity : BaseActivity<ActivityTableBinding>() {
    override fun onViewBindingCreate() = ActivityTableBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()

    override fun initWindowInsets(l: Int, t: Int, r: Int, b: Int) {
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
    }

    override fun initView() {
        setLightStatusBar(false)
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
            val data = vm.cur.value ?: return@setOnClickListenerWithClickAnimation
            data.tableConfig.ignoreSaturday = !data.tableConfig.ignoreSaturday
            vm.cur.value = data
        }
        ignoreSundayBtn.setOnClickListenerWithClickAnimation {
            val data = vm.cur.value ?: return@setOnClickListenerWithClickAnimation
            data.tableConfig.ignoreSunday = !data.tableConfig.ignoreSunday
            vm.cur.value = data
        }
        ignoreEveningBtn.setOnClickListenerWithClickAnimation {
            val data = vm.cur.value ?: return@setOnClickListenerWithClickAnimation
            data.tableConfig.ignoreEvening = !data.tableConfig.ignoreEvening
            vm.cur.value = data
        }
        nameFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val data = vm.cur.value ?: return
                data.tableConfig.nameFilter = et.toString()
                vm.cur.value = data
            }
        })
        majorFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val data = vm.cur.value ?: return
                data.tableConfig.majorFilter = et.toString()
                vm.cur.value = data
            }
        })
        tablePreBtn.setOnClickListenerWithClickAnimation {
            val data = vm.cur.value ?: return@setOnClickListenerWithClickAnimation
            if (data.tableZC == 1) {
                SCToast.show("已经是第一周")
                return@setOnClickListenerWithClickAnimation
            }
            MainScope().launch {
                CourseDB.get().dao().getByZC(data.tableZC - 1).first().also {
                    data.tableZC -= 1
                    data.courses = it
                    vm.cur.value = data
                }
            }
        }
        tableNextBtn.setOnClickListenerWithClickAnimation {
            val data = vm.cur.value ?: return@setOnClickListenerWithClickAnimation
            if (data.tableZC == data.schoolData.weekNum) {
                SCToast.show("已经是最后一周")
                return@setOnClickListenerWithClickAnimation
            }
            MainScope().launch {
                CourseDB.get().dao().getByZC(data.tableZC + 1).first().also {
                    data.tableZC += 1
                    data.courses = it
                    vm.cur.value = data
                }
            }
        }
    }.let { }

    override fun initObserver() = vm.cur.observe(this) { data ->
        tableConfigBtnUi(data.tableConfig)
        System.currentTimeMillis().toDateFormat().run {
            "${month}月${day}日"
        }.also { binding.todayDate.text = it }
        "第${data.curZC}周".also { binding.todayZc.text = it }
        "第${data.tableZC}周".also { binding.zc.text = it }
        showTable(
            data.tableZC,
            data.courses,
            data.tableConfig.copy()
        )
    }

    private fun tableConfigBtnUi(tableConfig: UserConfig.TableConfig) {
        binding.ignoreSaturdayBtn.also {
            if (tableConfig.ignoreSaturday) {
                it.setTextColor(this@TableActivity.getColor(R.color.white))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.primary)
                )
            } else {
                it.setTextColor(this@TableActivity.getColor(R.color.black))
                it.backgroundTintList = ColorStateList.valueOf(
                    this@TableActivity.getColor(R.color.gray_shade)
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
                    this@TableActivity.getColor(R.color.gray_shade)
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
                    this@TableActivity.getColor(R.color.gray_shade)
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

    override fun refreshDataWhenOnStart() = MainScope().launch {
        val data = DSHelper.run {
            getString(StringKeys.SCHOOL_NAME).first()
        }.let { name ->
            School.dataList.find { it.name == name }
        }.let {
            if (it == null) {
                SCToast.show("暂未绑定学校\n请前往用户设置中绑定")
                return@launch
            }
            vm.cur.value ?: MData(it.copy())
        }
        getCurZC().also {
            data.curZC = it
            if (data.tableZC == 0) data.tableZC = it
        }
        CourseDB.get().dao().getByZC(data.tableZC).first().also {
            data.courses = it
        }
        getTableConfig().also { tc ->
            data.tableConfig = tc
        }
        vm.cur.value = data
    }.let { }

    private suspend fun getCurZC(): Int {
        val oneWeek = 604800000L
        val cur = System.currentTimeMillis()
        return DSHelper.getLong(
            LongKeys.FIRST_ZC_MONDAY_TIME_STAMP,
            cur
        ).first().let { first ->
            ((cur - first) / oneWeek + 1).toInt()
        }
    }

    private suspend fun getTableConfig() = Gson().let {
        if (vm.cur.value != null) return@let vm.cur.value!!.tableConfig
        it.fromJson(
            DSHelper.getString(
                StringKeys.TABLE_CONFIG,
                it.toJson(UserConfig.TableConfig())
            ).first(),
            UserConfig.TableConfig::class.java
        )
    }

    private fun showTable(
        tableZC: Int,
        courses: List<Course>,
        tableConfig: UserConfig.TableConfig
    ) = MainScope().launch {
        val sd = vm.cur.value?.schoolData ?: return@launch
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
        val mondayTimeStamp =
            if (courses.isNotEmpty()) {
                courses.first().let {
                    val offset = (it.xq - 1) * oneDay
                    it.timeStamp - offset
                }
            } else {
                runBlocking {
                    DSHelper.getLong(
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
        binding.table.update(
            rows = rows,
            columns = columns,
            cells = cells,
            xAxis = mutableListOf<String>().also { list ->
                for (i in 0 until columns) {
                    "${
                        if (i == 5 && tableConfig.ignoreSaturday) 7
                        else i + 1
                    } ${
                        if (i == 5 && tableConfig.ignoreSaturday) {
                            (mondayTimeStamp + (i + 1) * oneDay).toDateFormat().run {
                                "($month.$day)"
                            }
                        } else {
                            (mondayTimeStamp + i * oneDay).toDateFormat().run {
                                "($month.$day)"
                            }
                        }
                    }".also {
                        list.add(it)
                    }
                }
            },
            listener = this@TableActivity::onClickTableItem
        )
    }

    private fun onClickTableItem(cell: TableView.Cell) {
        if (vm.cur.value == null) return
        val course = vm.cur.value!!.courses.find {
            it.name == cell.title
        }!!
        CourseDetailDialog().also {
            it.updateCourse = this::updateCourse
            it.course = course
            it.show(supportFragmentManager, "CourseDetailDialog")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.cur.value?.apply {
            saveTableConfig(tableConfig.copy())
        }
    }

    private fun saveTableConfig(tableConfig: UserConfig.TableConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            Gson().toJson(tableConfig).also { str ->
                DSHelper.setString(StringKeys.TABLE_CONFIG, str)
            }
        }
    }

    private fun updateCourse(course: Course) {
        MainScope().launch {
            vm.cur.value?.apply {
                courses.find { it.id == course.id }
                    ?.remark = course.remark
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            CourseDB.get().dao().update(course)
        }
    }

    data class MData(
        var schoolData: School.SchoolData,
        var curZC: Int = 0,
        var tableZC: Int = 0,
        var courses: List<Course> = emptyList(),
        var tableConfig: UserConfig.TableConfig = UserConfig.TableConfig()
    )
}