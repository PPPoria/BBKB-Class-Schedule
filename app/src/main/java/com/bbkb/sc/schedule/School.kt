package com.bbkb.sc.schedule

import com.bbkb.sc.R
import com.bbkb.sc.datastore.StringKeys
import com.poria.base.store.DSManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object School {
    val dataList = listOf(
        SchoolData(
            1, "GDUT", R.drawable.logo_gdut,
            20, 12, 3,
            listOf(
                "08:30\n09:15",
                "09:20\n10:05",
                "10:25\n11:10",
                "11:15\n12:00",
                "13:50\n14:35",
                "14:40\n15:25",
                "15:30\n16:15",
                "16:30\n17:15",
                "17:20\n18:05",
                "18:30\n19:15",
                "19:20\n20:05",
                "20:10\n20:55",
            )
        ),
        SchoolData(
            2, "SGU", R.drawable.logo_sgu,
            20, 11, 2,
            listOf(
                "08:00\n08:45",
                "08:55\n09:40",
                "10:00\n10:45",
                "10:55\n11:40",
                "13:00\n14:00",
                "14:40\n15:25",
                "15:35\n16:20",
                "16:30\n17:15",
                "17:25\n18:10",
                "19:30\n20:15",
                "20:25\n21:10",
            )
        ),
    ).sortedBy { it.name }

    val curSchoolData: SchoolData?
        get() = runBlocking {
            val name = DSManager.getString(StringKeys.SCHOOL_NAME).first()
            return@runBlocking dataList.find { it.name == name }
        }


    data class SchoolData(
        val id: Int,
        val name: String, // 英文缩写
        val iconId: Int,
        val weekNum: Int, // 一学期总周数
        val nodesPerDay: Int, // 每天课节数
        val nodesInEvening: Int, // 夜间课节数
        val nodeTimeList: List<String> = emptyList() // 课节时间列表
    )
}