package com.bbkb.sc.schedule.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bbkb.sc.util.StringListConverter

@Entity(tableName = "note_categories")
@TypeConverters(StringListConverter::class)
data class NoteCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var timeStamp: Long = 0,// 记录最后一次修改时间
    var courseNames: List<String> = emptyList(),// 一个笔记种类可以关联多个课程
)
