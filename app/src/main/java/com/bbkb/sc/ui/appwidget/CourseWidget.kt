package com.bbkb.sc.ui.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViews.RemoteCollectionItems
import com.bbkb.sc.R
import com.bbkb.sc.schedule.School
import com.bbkb.sc.schedule.TableAttr
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.util.ScheduleUtils
import com.poria.base.ext.DateFormat
import com.poria.base.ext.genColor
import com.poria.base.ext.toDateFormat
import com.poria.base.ext.toDayOfWeek
import com.poria.base.ext.toTimeStamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NextCourseWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            TableWidgetManager.updateNext(context)
        }
    }

    override fun onEnabled(context: Context) = Unit
    override fun onDisabled(context: Context) = Unit
}

class TodayCourseWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            TableWidgetManager.updateToday(context)
        }
    }

    override fun onEnabled(context: Context) = Unit
    override fun onDisabled(context: Context) = Unit
}

internal fun updateNext(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    nextCourse: Course?,
    relativeDay: Int = 0,
    zc: Int,
    xq: Int
) = RemoteViews(context.packageName, R.layout.next_widget).apply {
    setTextViewText(R.id.today_zc, "第${zc}周")
    setTextViewText(
        R.id.today_xq,
        when (xq - 1) {
            0 -> "星期一"
            1 -> "星期二"
            2 -> "星期三"
            3 -> "星期四"
            4 -> "星期五"
            5 -> "星期六"
            6 -> "星期日"
            else -> ""
        }
    )
    if (nextCourse == null) {
        setViewVisibility(R.id.no_course_text, View.VISIBLE)
        return@apply
    }
    setViewVisibility(R.id.no_course_text, View.GONE)
    val attr = TableAttr.latest
    val sd = School.curSchoolData ?: return@apply
    val start = sd.nodeTimeList[nextCourse.startNode - 1].substring(0, 5)
    val end = sd.nodeTimeList[nextCourse.endNode - 1].substring(6)
    val color = sd.name.genColor(
        attr.courseColorHHueOffset
    ).let { ColorStateList.valueOf(it) }
    if (relativeDay == 0) setTextViewText(R.id.relative_time, "今天")
    else setTextViewText(R.id.relative_time, "${relativeDay}天后")
    setColorStateList(R.id.course_color, "setBackgroundTintList", color)
    setTextViewText(R.id.time, "$start\n$end")
    setTextViewText(R.id.name, nextCourse.name)
    setTextViewText(R.id.node, nextCourse.run { "${startNode}-${endNode}节" })
    setTextViewText(R.id.room, nextCourse.classroom)
}.let { appWidgetManager.updateAppWidget(appWidgetId, it) }

internal fun updateToday(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    list: List<Course> = emptyList(),
    zc: Int,
    xq: Int
) = RemoteViews(context.packageName, R.layout.today_widget).apply {
    setTextViewText(R.id.today_zc, "第${zc}周")
    setTextViewText(
        R.id.today_xq,
        when (xq - 1) {
            0 -> "星期一"
            1 -> "星期二"
            2 -> "星期三"
            3 -> "星期四"
            4 -> "星期五"
            5 -> "星期六"
            6 -> "星期日"
            else -> ""
        }
    )
    setViewVisibility(R.id.no_course_text, if (list.isEmpty()) View.VISIBLE else View.GONE)
    val attr = TableAttr.latest
    val sd = School.curSchoolData ?: return@apply
    val itemViews = RemoteCollectionItems.Builder().also { builder ->
        list.forEach { course ->
            val start = sd.nodeTimeList[course.startNode - 1].substring(0, 5)
            val end = sd.nodeTimeList[course.endNode - 1].substring(6)
            val color = sd.name.genColor(
                attr.courseColorHHueOffset
            ).let { ColorStateList.valueOf(it) }
            RemoteViews(
                context.packageName,
                R.layout.item_today_widget_course
            ).also { v ->
                v.setColorStateList(R.id.course_color, "setBackgroundTintList", color)
                v.setTextViewText(R.id.time, "$start\n$end")
                v.setTextViewText(R.id.name, course.name)
                v.setTextViewText(R.id.node, course.run { "${startNode}-${endNode}节" })
                v.setTextViewText(R.id.room, course.classroom)
            }.let { builder.addItem(course.id, it) }
        }
    }.setViewTypeCount(1).build()
    setRemoteAdapter(R.id.widget_list_view, itemViews)
}.let { appWidgetManager.updateAppWidget(appWidgetId, it) }

object TableWidgetManager {
    private val noneCourse = Course(
        name = "--",
        teacher = "--",
        major = "--",
        classroom = "--",
        zc = 1,
        xq = 1,
        startNode = 1,
        endNode = 1,
        timeStamp = 0,
        description = "--",
    )

    suspend fun updateAllWidget(context: Context) {
        updateToday(context)
        updateNext(context)
    }

    suspend fun updateToday(context: Context) {
        val oneDay = ScheduleUtils.ONE_DAY_TIMESTAMP
        val curTime = System.currentTimeMillis()
        val zc = ScheduleUtils.getZC(curTime)
        if (zc != -1) {
            val courses = withContext(Dispatchers.IO) {
                CourseDB.get().dao().getByZC(zc).first()
            }.filter {
                curTime in it.timeStamp..(it.timeStamp + oneDay)
            }.sortedBy { it.startNode }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager
                .getAppWidgetIds(ComponentName(context, TodayCourseWidget::class.java))
            for (id in ids) {
                updateToday(context, appWidgetManager, id, courses, zc, curTime.toDayOfWeek())
            }
        }
    }

    suspend fun updateNext(context: Context) {
        val oneDay = ScheduleUtils.ONE_DAY_TIMESTAMP
        val curTime = System.currentTimeMillis()
        val zc = ScheduleUtils.getZC(curTime)
        val xq = curTime.toDayOfWeek()
        if (zc != -1) {
            val today = curTime.toDateFormat()
            val sd = School.curSchoolData ?: return
            val node = sd.nodeTimeList.let {
                for (i in it.indices) {
                    val timeStr = it[i]
                    val startHour = timeStr.substring(0, 2).toInt()
                    val startMinute = timeStr.substring(3, 5).toInt()
                    if (startHour * 60 + startMinute > today.hour * 60 + today.minute) {
                        return@let i + 1
                    }
                }
                return@let 0
            }
            val next = withContext(Dispatchers.IO) {
                CourseDB.get().dao().getAll().first()
            }.filter {
                it.timeStamp > curTime - oneDay
            }.sortedWith( // 多级排序
                compareBy(
                    { it.zc },
                    { it.xq },
                    { it.startNode }
                )
            ).let {
                for (c in it) {
                    if (c.zc != zc || c.xq != xq) return@let c
                    else if (c.startNode > node) return@let c
                }
                return@let null
            }
            val relativeDay = next?.timeStamp?.let {
                (it - today.run { DateFormat(year, month, day).toTimeStamp() }) / oneDay
            }?.toInt() ?: -1
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager
                .getAppWidgetIds(ComponentName(context, NextCourseWidget::class.java))
            for (id in ids) {
                updateNext(context, appWidgetManager, id, next, relativeDay, zc, xq)
            }
        }
    }
}