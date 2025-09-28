package com.bbkb.sc.schedule.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bbkb.sc.util.StringListConverter

@Entity(tableName = "note_items")
@TypeConverters(StringListConverter::class)
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var timeStamp: Long,// 确定年月日，同日的优先级由priority决定
    var priority: Int,// 数字越大优先级越低
    var title: String,
    var notes: List<String> = emptyList(),
    var picturePaths: List<String> = emptyList()
)
