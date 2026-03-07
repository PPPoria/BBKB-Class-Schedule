package com.bbkb.sc.schedule.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "problems")
data class Problem(
    @PrimaryKey
    val id: Long,
    var category: String,
    var content: String,
    var answer: String,
)
