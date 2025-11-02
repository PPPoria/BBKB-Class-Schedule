package com.bbkb.sc.schedule.database

import androidx.room.Entity

@Entity(tableName = "problems")
data class Problem(
    val id: Long,
    var category: String,
    var content: String,
    var answer: String,
)
