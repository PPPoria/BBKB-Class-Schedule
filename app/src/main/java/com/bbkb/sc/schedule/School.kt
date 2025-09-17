package com.bbkb.sc.schedule

import com.bbkb.sc.R
import com.bbkb.sc.schedule.gdut.GDUTGripper

object School {
    val dataList = listOf(
        SchoolData(1, "GDUT", R.drawable.logo_gdut, 20, 12, 3)
    )

    data class SchoolData(
        val id: Int,
        val name: String,
        val iconId: Int,
        val weekNum: Int,
        val nodesPerDay: Int,
        val nodesInEvening: Int,
    )
}