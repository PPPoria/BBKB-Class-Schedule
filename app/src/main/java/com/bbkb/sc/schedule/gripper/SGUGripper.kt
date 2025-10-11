package com.bbkb.sc.schedule.gripper

import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.util.SCLog
import com.google.gson.GsonBuilder
import com.poria.base.ext.DateFormat
import com.poria.base.ext.toTimeStamp

private const val TAG = "SGUGripper"

class SGUGripper : Gripper() {
    override val schoolName: String = "SGU"
    override val authUrl: String = "https://cas.sgu.edu.cn/lyuapServer/login"

    override fun getCheckAuthJs(): String = """
(function() {
    const hasTrueText = /学生主页/.test(document.body.textContent);
    const hasFalseText = /为了获得更好的用户体验，/.test(document.body.textContent);
    if (hasTrueText && !hasFalseText) return "true";
    else return "false";
})();
        """.trimIndent()

    override fun getStepsJsAfterAuth(): List<String> = mutableListOf(
        "",
        """
(function() { 
    location.href = 'https://cas.sgu.edu.cn/lyuapServer/login?service='
        + encodeURIComponent('http://jwc.sgu.edu.cn/sso.jsp');
})();
        """.trimIndent(),
        "",
        """
(function() { 
    location.href = 'http://jwc.sgu.edu.cn/jsxsd/jxzl/jxzl_query?Ves632DSdyV=NEW_XSD_WDZM';
})();
        """.trimIndent(),
    )

    override fun getAllCoursesJs(): List<String> = mutableListOf(
        """
(function() {
    const table = document.querySelector('table#kbtable');
    const rows = Array.from(table.rows);
    const data = rows.map(row =>
      Array.from(row.cells).map(cell => cell.innerText.trim())
    );
    return data;
})();
        """.trimIndent(),
        "",
        """
(function() { 
    location.href = 'http://jwc.sgu.edu.cn/jsxsd/xskb/xskb_list.do';
})();
        """.trimIndent(),
        "",
        """
(function() {
    const table = document.querySelector('table#kbtable');
    const rows = Array.from(table.rows);
    const data = rows.map(row =>
      Array.from(row.cells).map(cell => cell.innerText.trim())
    );
    return data;
})();
        """.trimIndent()
    )

    override fun getZCCourseJs(zc: Int): String = ""

    private val oneDayTimeStamp = 24 * 60 * 60 * 1000L
    private val oneWeekTimeStamp = 7 * oneDayTimeStamp
    private var firstDayTimeStamp = 0L

    override fun decodeCourseData(data: String): List<Course> {
        if (data.contains("null")) return emptyList()
        val gson = GsonBuilder().serializeNulls().create()
        // 不要删掉这行，否则 gson.fromJson 会报错
        SCLog.debug(TAG, "test gson: ${gson.toJson(OriData(listOf(listOf("test"))))}")
        val oriList = "{oriList: $data}".let {
            gson.fromJson(it, OriData::class.java)
        }.oriList
        if (oriList[0][0].contains("月份")) {
            val (year, month) = oriList[1][0].run {
                substring(
                    0,
                    indexOf("年")
                ).toInt() to substring(
                    indexOf("年") + 1,
                    indexOf("月")
                ).toInt()
            }
            val day = oriList[1][2].toInt()
            firstDayTimeStamp = DateFormat(
                year = year,
                month = month,
                day = day
            ).toTimeStamp()
            return emptyList()
        }
        val courseList = mutableListOf<Course>()
        // i 是大节， j 是星期
        for (i in 1 until oriList.size - 1) {
            for (j in 1 until oriList[i].size) {
                val content = oriList[i][j]
                if (content.isEmpty() || content.isBlank()) continue
                val strList = content.split("\n")
                val name = strList[0]
                val teacher = strList[1]
                val classroom = strList[3]
                val description = strList[4]
                val node = when (i) {
                    1 -> (1 to 2)
                    2 -> (3 to 4)
                    3 -> (5 to 5)
                    4 -> (6 to 7)
                    5 -> (8 to 9)
                    else -> (10 to 11)
                }
                strList[2].replace("(周)", "").split(",").filter {
                    it.isNotBlank() && it.isNotEmpty()
                }.forEach {
                    if (it.contains("-")) {
                        val (start, end) = it.split("-").let { ss ->
                            ss[0].toInt() to ss[1].toInt()
                        }
                        for (k in start..end) Course(
                            name = name,
                            teacher = teacher,
                            major = "",
                            classroom = classroom,
                            zc = k,
                            xq = j,
                            startNode = node.first,
                            endNode = node.second,
                            timeStamp = firstDayTimeStamp + (k - 1) * oneWeekTimeStamp + (j - 1) * oneDayTimeStamp,
                            description = description
                        ).also { c -> courseList.add(c) }
                    } else {
                        Course(
                            name = name,
                            teacher = teacher,
                            major = "",
                            classroom = classroom,
                            zc = it.toInt(),
                            xq = j,
                            startNode = node.first,
                            endNode = node.second,
                            timeStamp = firstDayTimeStamp + (it.toInt() - 1) * oneWeekTimeStamp + (j - 1) * oneDayTimeStamp,
                            description = description
                        ).also { c -> courseList.add(c) }
                    }
                }
            }
        }
        return courseList
    }

    data class OriData(
        val oriList: List<List<String>>
    )
}