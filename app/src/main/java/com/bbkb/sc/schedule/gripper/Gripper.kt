package com.bbkb.sc.schedule.gripper

import com.bbkb.sc.schedule.database.Course

abstract class Gripper {
    abstract val schoolName: String // 学校的名称
    abstract val authUrl: String // 官网的地址
    abstract fun getCheckAuthJs(): String // 如果已认证，JS请返回"true"
    abstract fun getStepsJsAfterAuth(): List<String>
    abstract fun getAllCoursesJs(): List<String>
    abstract fun getZCCourseJs(zc: Int): String
    abstract fun decodeCourseData(data: String): List<Course>

    companion object {
        fun getGripperBySchoolId(schoolId: Int) = when (schoolId) {
            1 -> GDUTGripper()
            2 -> SGUGripper()
            else -> throw IllegalArgumentException("Invalid school id: $schoolId")
        }
    }
}