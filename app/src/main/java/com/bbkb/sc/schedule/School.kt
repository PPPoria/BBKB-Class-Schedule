package com.bbkb.sc.schedule

import com.bbkb.sc.R

object School {
    val dataList = listOf(
        SchoolData(
            1, "GDUT", R.drawable.logo_gdut,
            20, 12, 3
        ),
        SchoolData(
            2, "SGU", R.drawable.logo_sgu,
            20, 11, 2
        ),
    ).sortedBy { it.name }

    data class SchoolData(
        val id: Int,
        val name: String, // 英文缩写
        val iconId: Int,
        val weekNum: Int, // 一学期总周数
        val nodesPerDay: Int, // 每天课节数
        val nodesInEvening: Int, // 夜间课节数
    )
}