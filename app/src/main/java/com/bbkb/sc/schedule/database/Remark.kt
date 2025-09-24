package com.bbkb.sc.schedule.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remarks")
data class Remark(
    @PrimaryKey
    var courseName: String,
    var content: String,
)
