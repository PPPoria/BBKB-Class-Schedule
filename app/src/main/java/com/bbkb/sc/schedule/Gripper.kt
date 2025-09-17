package com.bbkb.sc.schedule

import com.bbkb.sc.schedule.data.Course
import com.bbkb.sc.schedule.gdut.GDUTGripper

abstract class Gripper {
    abstract val schoolName: String // 学校的名称
    abstract val authUrl: String // 官网的地址
    abstract fun getCheckAuthJs(): String
    abstract fun getStepsJsAfterAuth(): List<String>
    abstract fun getAllCoursesJs(): List<String>
    abstract fun getCourseJs(zc: Int): String
    abstract fun decodeCourseData(data: String): List<Course>

    companion object {
        fun getGripperBySchoolId(schoolId: Int) = when (schoolId) {
            1 -> GDUTGripper()
            else -> throw IllegalArgumentException("Invalid school id: $schoolId")
        }
    }
}