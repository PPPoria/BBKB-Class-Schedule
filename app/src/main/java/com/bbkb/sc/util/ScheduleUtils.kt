package com.bbkb.sc.util

import com.bbkb.sc.datastore.LongKeys
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.schedule.database.Remark
import com.bbkb.sc.schedule.database.RemarkDB
import com.google.gson.Gson
import com.poria.base.ext.toDateFormat
import com.poria.base.store.DSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object ScheduleUtils {
    const val ONE_DAY_TIMESTAMP = 86400000L
    const val ONE_WEEK_TIMESTAMP = 604800000L

    // 周次
    suspend fun getZC(timeStamp: Long): Int {
        return withContext(Dispatchers.Default) {
            DSManager.getLong(
                LongKeys.FIRST_ZC_MONDAY_TIME_STAMP,
                timeStamp
            ).first().let { first ->
                if (timeStamp <= first) -1
                else ((timeStamp - first) / ONE_WEEK_TIMESTAMP + 1).toInt()
            }
        }
    }

    // 学年学期，例如 (2025, 2) 表示 2025 学年的第二学期
    fun getXnXq(timeStamp: Long): Pair<Int, Int> {
        return timeStamp.toDateFormat().run {
            if (month <= 7) (year - 1 to 2)
            else (year to 1)
        }
    }

    suspend fun getTableConfig(): TableConfig = Gson().let {
        val flow = DSManager.getString(
            StringKeys.TABLE_CONFIG,
            it.toJson(TableConfig())
        )
        it.fromJson(
            withContext(Dispatchers.Default) {
                flow.first()
            },
            TableConfig::class.java
        )
    }

    suspend fun getRemarkByCourseName(courseName: String): Remark = withContext(Dispatchers.IO) {
        val flow = RemarkDB
            .get()
            .dao().getByCourseName(courseName)
        flow.first().let { list ->
            if (list.isEmpty()) Remark(courseName, "").also {
                RemarkDB.get().dao().insert(it)
            } else if (list.size > 1)
                throw IllegalStateException("remark's primary key is not unique")
            else list.first()
        }
    }

    fun calculateFirstZCMondayTimeStamp(st: Long, zc: Int, xq: Int): Long {
        val offset = (zc - 1) * ONE_WEEK_TIMESTAMP + (xq - 1) * ONE_DAY_TIMESTAMP
        return st - offset
    }
}