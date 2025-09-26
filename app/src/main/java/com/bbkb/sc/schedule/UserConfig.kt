package com.bbkb.sc.schedule

object UserConfig {


}

data class TableConfig(
    val ignoreSaturday: Boolean = false,
    val ignoreSunday: Boolean = false,
    val ignoreEvening: Boolean = false,
    val nameFilter: String = "",
    val majorFilter: String = "",
)