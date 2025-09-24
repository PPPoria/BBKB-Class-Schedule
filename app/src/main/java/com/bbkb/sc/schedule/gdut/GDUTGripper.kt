package com.bbkb.sc.schedule.gdut

import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.Gripper
import com.bbkb.sc.util.SCLog
import com.google.gson.GsonBuilder
import com.poria.base.ext.DateFormat
import com.poria.base.ext.toTimeStamp

private const val TAG = "GDUTGripper"

class GDUTGripper : Gripper() {
    override val schoolName = "GDUT"
    override val authUrl = "https://jxfw.gdut.edu.cn/login!welcome.action"

    override fun getCheckAuthJs() = """
(function() {
    const hasText = /我的桌面/.test(document.body.textContent);
    if (hasText) return "true";
    else return "false";
})();
    """.trimIndent()

    override fun getStepsJsAfterAuth() = mutableListOf(
        "selectedM3(this,'xsgrkbcx!xsgrkbMain.action','课表查询')",
    )

    override fun getAllCoursesJs(): List<String> {
        val courses = mutableListOf<String>()
        for (i in 1..20) courses.add(
            getZCCourseJs(i)
        )
        return courses
    }

    override fun getZCCourseJs(zc: Int) =
        """
(function(){
    const url = `https://jxfw.gdut.edu.cn/xsgrkbcx!getKbRq.action?xnxqdm=202501&zc=${zc}`;
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url, false); // 第三个参数 false = 同步
    // 同步模式下设置请求头
    xhr.setRequestHeader('Accept', 'application/json, text/javascript, */*; q=0.01');
    xhr.setRequestHeader('Referer', `https://jxfw.gdut.edu.cn/xsgrkbcx!xskbList.action?xnxqdm=202501&zc=${zc}`);
    xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    // 发送（这里会阻塞直到返回）
    xhr.send();
    return xhr.responseText;
})();
        """.trimIndent()

    override fun decodeCourseData(data: String): List<Course> {
        val gson = GsonBuilder().serializeNulls().create()
        SCLog.debug(TAG, "test gson: ${gson.toJson(Item(kcmc = "test"))}")
        val items = data
            .replace("\\\"", "\"")
            .replace("[", "")
            .replace("]", "")
            .let { raw ->
                val itemsStartIndexList = mutableListOf<Int>()
                var i = -1
                while (true) {
                    i = raw.indexOf("{", i + 1)
                    if (i == -1) break
                    itemsStartIndexList.add(i)
                }
                itemsStartIndexList.add(raw.length)
                mutableListOf<Item>().also { list ->
                    for (index in 0..itemsStartIndexList.size - 2) {
                        raw.substring(
                            itemsStartIndexList[index],
                            itemsStartIndexList[index + 1] - 1
                        ).also {
                            list.add(gson.fromJson(it, Item::class.java))
                        }
                    }
                }
            }
        val beginDate = items[items.size - 7].rq
            ?.split("-")?.map {
                it.toInt()
            } ?: return emptyList()
        return ArrayList<Course>().apply {
            for (i in 0..items.size - 8) {
                val item = items[i]
                val dm = item.jcdm2!!
                    .split(",")
                    .map { it.toInt() }
                Course(
                    name = item.kcmc!!,
                    teacher = item.teaxms!!,
                    major = item.jxbmc!!,
                    classroom = item.jxcdmc!!,
                    zc = item.zc!!.toInt(),
                    xq = item.xq!!.toInt(),
                    startNode = dm.first(),
                    endNode = dm.last(),
                    timeStamp = DateFormat(
                        year = beginDate[0],
                        month = beginDate[1],
                        day = beginDate[2],
                    ).toTimeStamp(),
                    description = item.sknrjj!!
                ).also { add(it) }
            }
        }
    }

    data class Item(
        val dgksdm: String? = null,
        val kcbh: String? = null,
        val kcmc: String? = null,
        val teaxms: String? = null,
        val jxbdm: String? = null,
        val xnxqdm: String? = null,
        val jxbmc: String? = null,
        val zc: String? = null,
        val jcdm: String? = null,
        val jcdm2: String? = null,
        val xq: String? = null,
        val jxcdmc: String? = null,
        val sknrjj: String? = null,
        val teadms: String? = null,
        val jxcddm: String? = null,
        val kcdm: String? = null,
        val zxs: String? = null,
        val xs: String? = null,
        val pkrs: String? = null,
        val kxh: String? = null,
        val flfzmc: String? = null,
        val jxhjmc: String? = null,
        val tkbz: String? = null,
        val xqmc: String? = null,
        val rq: String? = null
    )
}