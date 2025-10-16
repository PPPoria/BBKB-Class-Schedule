package com.bbkb.sc.ui.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.bbkb.sc.R
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.schedule.database.Course
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.poria.base.ext.toDayOfWeek
import com.poria.base.store.DSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TableWidget : AppWidgetProvider() {
    /**
     * 定时更新，或添加新 widget 时调用
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新所有 widget
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    json: String? = null,
) = RemoteViews(context.packageName, R.layout.table_widget).apply {
    val list = (json ?: runBlocking {
        DSManager.getString(
            StringKeys.TODAY_COURSE_LIST_JSON,
            ""
        ).first()
    }).let { json ->
        if (json.isNotEmpty()) {
            val gson = GsonBuilder().serializeNulls().create()
            listOf(
                TableWidgetManager.noneCourse,
                TableWidgetManager.noneCourse
            ).also { gson.toJson(it) }
            val list: List<Course> = gson.fromJson(
                json,
                object : TypeToken<List<Course>>() {}.type
            )
            list
        } else TableWidgetManager.noneCourse.copy(
            name = "还未绑定学校",
        ).let { listOf(it) }
    }.sortedBy { it.startNode }
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
    for (i in list.indices) {
        setViewVisibility(TableWidgetManager.cellIds[i], View.VISIBLE)
        setTextViewText(TableWidgetManager.nameIds[i], list[i].name)
        setTextViewText(TableWidgetManager.nodeIds[i], list[i].run { "$startNode-$endNode 节" })
        setTextViewText(TableWidgetManager.roomIds[i], list[i].classroom)
    }
    for (i in list.size until TableWidgetManager.cellIds.size) {
        setViewVisibility(TableWidgetManager.cellIds[i], View.GONE)
    }
}.let { appWidgetManager.updateAppWidget(appWidgetId, it) }

object TableWidgetManager {
    val noneCourse = Course(
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
    val cellIds = listOf(
        R.id.cell_1,
        R.id.cell_2,
        R.id.cell_3,
        R.id.cell_4,
        R.id.cell_5,
        R.id.cell_6,
        R.id.cell_7,
        R.id.cell_8,
        R.id.cell_9,
        R.id.cell_10,
        R.id.cell_11,
        R.id.cell_12,
        R.id.cell_13,
        R.id.cell_14,
        R.id.cell_15,
        R.id.cell_16,
        R.id.cell_17,
        R.id.cell_18,
        R.id.cell_19,
        R.id.cell_20,
    )
    val nameIds = listOf(
        R.id.name_1,
        R.id.name_2,
        R.id.name_3,
        R.id.name_4,
        R.id.name_5,
        R.id.name_6,
        R.id.name_7,
        R.id.name_8,
        R.id.name_9,
        R.id.name_10,
        R.id.name_11,
        R.id.name_12,
        R.id.name_13,
        R.id.name_14,
        R.id.name_15,
        R.id.name_16,
        R.id.name_17,
        R.id.name_18,
        R.id.name_19,
        R.id.name_20,
    )
    val nodeIds = listOf(
        R.id.node_1,
        R.id.node_2,
        R.id.node_3,
        R.id.node_4,
        R.id.node_5,
        R.id.node_6,
        R.id.node_7,
        R.id.node_8,
        R.id.node_9,
        R.id.node_10,
        R.id.node_11,
        R.id.node_12,
        R.id.node_13,
        R.id.node_14,
        R.id.node_15,
        R.id.node_16,
        R.id.node_17,
        R.id.node_18,
        R.id.node_19,
        R.id.node_20,
    )
    val roomIds = listOf(
        R.id.room_1,
        R.id.room_2,
        R.id.room_3,
        R.id.room_4,
        R.id.room_5,
        R.id.room_6,
        R.id.room_7,
        R.id.room_8,
        R.id.room_9,
        R.id.room_10,
        R.id.room_11,
        R.id.room_12,
        R.id.room_13,
        R.id.room_14,
        R.id.room_15,
        R.id.room_16,
        R.id.room_17,
        R.id.room_18,
        R.id.room_19,
        R.id.room_20,
    )

    fun updateTable(context: Context, courses: List<Course>) {
        val gson = GsonBuilder().serializeNulls().create()
        val json = gson.toJson(courses)
        CoroutineScope(Dispatchers.IO).launch {
            DSManager.setString(
                StringKeys.TODAY_COURSE_LIST_JSON,
                json
            )
        }
        updateTable(context, json)
    }

    private fun updateTable(context: Context, json: String? = null) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TableWidget::class.java))
        for (id in ids) {
            updateWidget(context, appWidgetManager, id, json)
        }
    }
}