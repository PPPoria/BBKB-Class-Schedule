package com.bbkb.sc.ui.fragment

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.databinding.FragmentTableBinding
import com.bbkb.sc.datastore.LongKeys
import com.bbkb.sc.schedule.TableAttr
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.ui.activity.TableActivity
import com.bbkb.sc.ui.activity.TableActivity.Companion.KEY_MODE
import com.bbkb.sc.ui.activity.TableActivity.Companion.KEY_NOTE_CATEGORY_ID
import com.bbkb.sc.ui.activity.TableActivity.Companion.MODE_ADD_RELATED_NOTE
import com.bbkb.sc.ui.activity.TableActivity.Companion.MODE_NORMAL
import com.bbkb.sc.ui.dialog.CourseDetailDialog
import com.bbkb.sc.ui.widget.TableView
import com.bbkb.sc.util.ScheduleUtils
import com.poria.base.base.BaseFragment
import com.poria.base.ext.dp2px
import com.poria.base.ext.genColor
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TableFragment : BaseFragment<FragmentTableBinding>() {
    override fun onViewBindingCreate() = FragmentTableBinding.inflate(layoutInflater)
    private val vm by activityViewModels<SingleVM<TableActivity.MData>>()
    private val mode by lazy { requireArguments().getInt(KEY_MODE, MODE_NORMAL) }
    private val addedCategoryId by lazy {
        requireArguments().getLong(KEY_NOTE_CATEGORY_ID, 0L)
    }
    private val coursesCacheMap = HashMap<Int, List<Course>>()
    private val xAxisCacheMap = HashMap<Int, List<TableView.XItem>>()
    private val yAxis by lazy {
        val sd = vm.latest?.schoolData ?: return@lazy emptyList()
        ArrayList<TableView.YItem>().apply {
            for (i in sd.nodeTimeList.indices) {
                (i + 1).toString().let {
                    TableView.YItem(
                        id = unitId,
                        nodeNumber = it,
                        time = sd.nodeTimeList[i]
                    )
                }.let { add(it) }
            }
        }
    }
    private val mondayTimeStampList by lazy {
        ArrayList<Long>().apply {
            val first = runBlocking {
                DSManager.getLong(
                    LongKeys.FIRST_ZC_MONDAY_TIME_STAMP,
                    System.currentTimeMillis()
                ).first()
            }
            for (i in -1 until 40) {
                add(i * ScheduleUtils.ONE_WEEK_TIMESTAMP + first)
            }
        }
    }
    private var unitId: Long = 0
        get() = field++

    override fun initView() {
        with(binding.table) {
            onClickCell = this@TableFragment::onClickTableItem
            onScrollToPre = { onScrollToTablePre() }
            onScrollToNext = { onScrollToTableNext() }
            onScrollListener = this@TableFragment::onScrolling
        }
    }

    override fun initListener() = Unit

    override suspend fun refreshDataInScope() {
        vm.latest?.let { old ->
            val tableZC = old.tableZC
            val preZC = (tableZC - 2 + old.schoolData.weekNum) %
                    old.schoolData.weekNum + 1
            val nextZC = (tableZC) % old.schoolData.weekNum + 1
            old.copy(
                preCourses = getCoursesByZC(preZC),
                curCourses = getCoursesByZC(tableZC),
                nextCourses = getCoursesByZC(nextZC)
            )
        }?.let { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                val preZC = (data.tableZC - 2 + data.schoolData.weekNum) %
                        data.schoolData.weekNum + 1
                val nextZC = (data.tableZC) % data.schoolData.weekNum + 1
                "第${data.tableZC}周".also { binding.tableZcText.text = it }
                "第${preZC}周".also { binding.tablePreZcText.text = it }
                "第${nextZC}周".also { binding.tableNextZcText.text = it }
                binding.tableZcText.translationX = 0f
                binding.tablePreZcText.translationX = 0f
                binding.tableNextZcText.translationX = 0f
                updateTableData(
                    preZC = preZC,
                    tableZC = data.tableZC,
                    nextZC = nextZC,
                    preCourses = data.preCourses,
                    curCourses = data.curCourses,
                    nextCourses = data.nextCourses,
                    tableAttr = data.tableAttr,
                )
            }
        }
    }

    private fun updateTableData(
        preZC: Int,
        tableZC: Int,
        nextZC: Int,
        preCourses: List<Course>,
        curCourses: List<Course>,
        nextCourses: List<Course>,
        tableAttr: TableAttr = TableAttr.latest,
        tableConfig: TableConfig = TableConfig.latest
    ) = lifecycleScope.launch {
        val sd = vm.latest?.schoolData ?: return@launch
        val filterToCells: (List<Course>) -> List<TableView.Cell> = { courses ->
            courses.asSequence().filter {
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
            }.map { course ->
                TableView.Cell(
                    id = course.id,
                    title = course.name,
                    content = course.run {
                        "$teacher\n$classroom"
                    },
                    color = tableAttr.run {
                        course.name.genColor(
                            courseColorHHueOffset,
                            courseColorSHueBase,
                            courseColorLHueBase
                        )
                    },
                    row = course.run { startNode to endNode },
                    column = course.run { xq to xq }
                )
            }.toList()
        }
        val preCells = filterToCells(preCourses)
        val curCells = filterToCells(curCourses)
        val nextCells = filterToCells(nextCourses)
        val rows = when (tableConfig.ignoreEvening) {
            true -> sd.nodesPerDay - sd.nodesInEvening
            else -> sd.nodesPerDay
        }
        val columns = when {
            tableConfig.ignoreSaturday && tableConfig.ignoreSunday -> 5
            tableConfig.ignoreSaturday || tableConfig.ignoreSunday -> 6
            else -> 7
        }
        val zcToXAxisAndHighlightXId: suspend (Int) -> Pair<List<TableView.XItem>, Long> = {
            val (ori, id) = getXAxisAndHighlightXIdByZC(it)
            ori.toMutableList().apply {
                if (tableConfig.ignoreSunday) removeAt(6)
                if (tableConfig.ignoreSaturday) removeAt(5)
            } to id
        }
        val preXAxisAndHighlightXId = zcToXAxisAndHighlightXId(preZC)
        val curXAxisAndHighlightXId = zcToXAxisAndHighlightXId(tableZC)
        val nextXAxisAndHighlightXId = zcToXAxisAndHighlightXId(nextZC)
        binding.table.updateData(
            rows = rows,
            columns = columns,
            preCells = preCells,
            curCells = curCells,
            nextCells = nextCells,
            preXAxis = preXAxisAndHighlightXId.first,
            curXAxis = curXAxisAndHighlightXId.first,
            nextXAxis = nextXAxisAndHighlightXId.first,
            yAxis = yAxis,
            highlightXIds = listOf(
                preXAxisAndHighlightXId.second,
                curXAxisAndHighlightXId.second,
                nextXAxisAndHighlightXId.second
            ),
            attr = tableAttr,
        )
    }

    private suspend fun getCoursesByZC(zc: Int): List<Course> {
        if (coursesCacheMap.containsKey(zc)) return coursesCacheMap[zc]!!
        return withContext(Dispatchers.IO) {
            CourseDB.get().dao()
                .getByZC(zc)
                .first()
        }.also { coursesCacheMap[zc] = it }
    }

    private suspend fun getXAxisAndHighlightXIdByZC(zc: Int): Pair<List<TableView.XItem>, Long> {
        val monday = mondayTimeStampList[zc]
        val curTime = System.currentTimeMillis()
        val oneDay = ScheduleUtils.ONE_DAY_TIMESTAMP
        val oneWeek = ScheduleUtils.ONE_WEEK_TIMESTAMP
        val xAxis = if (xAxisCacheMap.containsKey(zc)) xAxisCacheMap[zc]!!
        else withContext(Dispatchers.Default) {
            val list = ArrayList<TableView.XItem>()
            for (i in 0 until 7) {
                when (i) {
                    0 -> "星期一"
                    1 -> "星期二"
                    2 -> "星期三"
                    3 -> "星期四"
                    4 -> "星期五"
                    5 -> "星期六"
                    6 -> "星期日"
                    else -> ""
                }.let { dayOfWeek ->
                    val date = (monday + oneDay * i)
                        .toDateFormat().run { "$month.${this@run.day}" }
                    TableView.XItem(
                        id = unitId,
                        dayOfWeek = dayOfWeek,
                        date = date
                    )
                }.let { list.add(it) }
            }
            list
        }.also { xAxisCacheMap[zc] = it }
        val id = if (curTime in monday..(monday + oneWeek)) {
            xAxis[((curTime - monday) / oneDay).toInt()].id
        } else -1L
        return xAxis to id
    }

    private fun onClickTableItem(cell: TableView.Cell) {
        if (mode == MODE_NORMAL) {
            val course = vm.latest?.curCourses?.find {
                it.id == cell.id
            } ?: return
            CourseDetailDialog().also {
                it.course = course
                it.show(
                    requireActivity().supportFragmentManager,
                    "course_detail_dialog"
                )
            }
        } else if (mode == MODE_ADD_RELATED_NOTE) {
            CoroutineScope(Dispatchers.IO).launch {
                val category = NoteCategoryDB.get().dao().getById(addedCategoryId).first()
                val courseNames = category.courseNames.toMutableList()
                courseNames.add(cell.title)
                NoteCategoryDB.get().dao().update(category.copy(courseNames = courseNames))
                withContext(Dispatchers.Main) {
                    requireActivity().finish()
                }
            }
        }
    }

    private fun onScrollToTablePre() = lifecycleScope.launch {
        val old = vm.latest ?: return@launch
        val zc = ((old.tableZC - 2 + old.schoolData.weekNum) %
                old.schoolData.weekNum + 1)
        val courses = getCoursesByZC(
            ((zc - 2 + old.schoolData.weekNum) %
                    old.schoolData.weekNum + 1)
        )
        old.copy(
            tableZC = zc,
            preCourses = courses,
            curCourses = old.preCourses,
            nextCourses = old.curCourses
        ).also { vm.update(it) }
    }

    private fun onScrollToTableNext() = lifecycleScope.launch {
        val old = vm.latest ?: return@launch
        val zc = (old.tableZC) % old.schoolData.weekNum + 1
        val courses = getCoursesByZC((zc) % old.schoolData.weekNum + 1)
        old.copy(
            tableZC = zc,
            preCourses = old.curCourses,
            curCourses = old.nextCourses,
            nextCourses = courses
        ).also { vm.update(it) }
    }

    private fun onScrolling(offset: Float, width: Float) {
        val mWidth = dp2px(100f)
        val mOffset = offset / width * mWidth
        binding.tableZcText.translationX = mOffset
        binding.tablePreZcText.translationX = mOffset
        binding.tableNextZcText.translationX = mOffset
    }
}