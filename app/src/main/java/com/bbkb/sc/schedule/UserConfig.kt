package com.bbkb.sc.schedule

object UserConfig {

    data class TableConfig(
        var ignoreSaturday: Boolean = false,
        var ignoreSunday: Boolean = false,
        var ignoreEvening: Boolean = false,
        var nameFilter: String = "",
        var majorFilter: String = "",
    )
}