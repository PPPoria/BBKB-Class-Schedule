package com.bbkb.sc.schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,// 课程名
    var teacher: String,// 教师名
    var major: String,// 专业
    var classroom: String,// 教室
    var zc: Int,// 周次，从1开始
    var xq: Int,// 星期几
    var startNode: Int,// 开始节数，从1开始
    var endNode: Int,// 结束节数
    var timeStamp: Long,// 上课日期，时间戳表示
    var remark: String = "暂无备注",// 备注
    var description: String = "暂无描述",// 课程描述
)
