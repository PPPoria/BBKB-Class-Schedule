package com.bbkb.sc.ui.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViews.RemoteCollectionItems
import com.bbkb.sc.R
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.util.ScheduleUtils
import com.poria.base.ext.toDayOfWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TableWidget : AppWidgetProvider() {
    /**
     * 定时更新，或添加新 widget 时调用
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            TableWidgetManager.updateNowTable(context)
        }
    }

    override fun onEnabled(context: Context) = Unit
    override fun onDisabled(context: Context) = Unit
}

internal fun updateWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    list: List<Course> = emptyList(),
) = RemoteViews(context.packageName, R.layout.table_widget).apply {
    setTextViewText(R.id.today_zc, "第${list.first().zc}周")
    setTextViewText(
        R.id.today_xq,
        when (list.first().xq - 1) {
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
    val itemViews = RemoteCollectionItems.Builder().also { builder ->
        list.forEach { course ->
            RemoteViews(
                context.packageName,
                R.layout.item_table_widget_course
            ).also { itemView ->
                itemView.setTextViewText(R.id.name, course.name)
                itemView.setTextViewText(R.id.time, course.run { "${startNode}-${endNode}节" })
                itemView.setTextViewText(R.id.room, course.classroom)
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
        zc = 0,
        xq = 0,
        startNode = 0,
        endNode = 0,
        timeStamp = 0,
        description = "--",
    )

    suspend fun updateNowTable(context: Context) {
        val oneDay = ScheduleUtils.ONE_DAY_TIMESTAMP
        val curTime = System.currentTimeMillis()
        val zc = ScheduleUtils.getZC(curTime)
        if (zc != -1) {
            val courses = withContext(Dispatchers.IO) {
                CourseDB.get().dao().getByZC(zc).first()
            }.filter {
                curTime in it.timeStamp..(it.timeStamp + oneDay)
            }.ifEmpty { // 空则添加一个空课程用来提示用户
                noneCourse.copy(
                    zc = zc,
                    xq = curTime.toDayOfWeek() - 1,
                    name = "暂无课程",
                    classroom = "--"
                ).let { listOf(it) }
            }
            updateTable(context, courses)
        }
    }

    private fun updateTable(context: Context, list: List<Course>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TableWidget::class.java))
        for (id in ids) {
            updateWidget(context, appWidgetManager, id, list)
        }
    }
}