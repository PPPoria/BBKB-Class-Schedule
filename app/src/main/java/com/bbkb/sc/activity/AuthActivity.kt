package com.bbkb.sc.activity

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityAuthBinding
import com.bbkb.sc.datastore.LongKeys
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.dialog.LoadingDialog
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.schedule.Gripper
import com.bbkb.sc.schedule.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.util.SCLog
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.store.DSManager
import com.poria.base.util.ThreadPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList

private const val TAG = "AuthActivity"

class AuthActivity : BaseActivity<ActivityAuthBinding>() {
    override fun onViewBindingCreate() = ActivityAuthBinding.inflate(layoutInflater)
    private val schoolId: Int by lazy {
        intent.getIntExtra("school_id", 0)
    }
    private val sd by lazy {
        School.dataList.find { schoolId == it.id }!!
    }
    private val gripper: Gripper by lazy {
        Gripper.getGripperBySchoolId(schoolId)
    }
    private val updateZC: Int by lazy {
        // 0表示更新全部
        intent.getIntExtra("update_zc", 0)
            .coerceIn(0, sd.weekNum)
    }
    private lateinit var dialog: LoadingDialog
    private lateinit var counter: Counter
    private val courseList = ArrayList<Course>()
    private var authSuccess = false

    override fun initView() {
        setLightStatusBar(false)
        setLightStatusBar(false)
        showAuthView()
    }

    // 开启一个浏览器，模拟用户操作
    @SuppressLint("SetJavaScriptEnabled")
    private fun showAuthView() = with(binding.webView) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                if (url == gripper.authUrl && !authSuccess) {
                    this@with.evaluateJavascript(gripper.getCheckAuthJs()) {
                        if (it.contains("true")) onAuth(true)
                        else onAuth(false)
                    }
                }
            }
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        loadUrl(gripper.authUrl)
    }

    // 授权完毕
    private fun onAuth(isSuccess: Boolean) = ThreadPool.execute {
        // 修改状态
        if (!isSuccess) {
            authSuccess = false
            SCLog.debug(TAG, "auth = false")
            return@execute
        }
        val mh = Handler(Looper.getMainLooper())
        authSuccess = true
        SCLog.debug(TAG, "auth = true")

        // 弹出等待弹窗
        dialog = LoadingDialog().also {
            it.title = getString(R.string.importing)
            it.show(supportFragmentManager, it.title)
        }
        runCatching {
            // 等待前往目标页面
            for (js in gripper.getStepsJsAfterAuth()) {
                mh.post {
                    binding.webView.evaluateJavascript(js, null)
                }
                Thread.sleep(666L)
            }
            // 抓取课程信息
            Thread.sleep(5_000L)
            if (updateZC == 0) { // 更新全部
                counter = Counter(sd.weekNum)
                counter.onFinish = {
                    Handler(Looper.getMainLooper()).post {
                        dialog.dismiss()
                        onImported(true)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        // 保存课程信息
                        // 这里先判断是否有课表，如果没有课表，则不保存
                        if (courseList.isEmpty()) {
                            SCToast.show(getString(R.string.schedule_is_empty))
                            throw IllegalStateException("courseList is empty")
                        }
                        val stamp = courseList.first().run {
                            ScheduleUtils.calculateFirstZCMondayTimeStamp(
                                timeStamp, zc, xq
                            )
                        }
                        runBlocking {
                            DSManager.setLong(LongKeys.FIRST_ZC_MONDAY_TIME_STAMP, stamp)
                            DSManager.setString(StringKeys.SCHOOL_NAME, gripper.schoolName)
                        }
                        CourseDB.get().dao().also {
                            it.deleteAll()
                            it.insert(courseList)
                        }
                    }
                    for (course in courseList) {
                        SCLog.debug(TAG, course.run {
                            "$id: {name=$name, major=$major, zc=$zc, xq=$xq, startNode=$startNode, endNode=$endNode}"
                        })
                    }
                }

                // 开始抓取课程信息
                for (js in gripper.getAllCoursesJs()) {
                    mh.post {
                        binding.webView.evaluateJavascript(js) { data ->
                            courseList.addAll(gripper.decodeCourseData(data))
                            counter.next()
                        }
                    }
                    Thread.sleep(400L)
                }
            } else { // 更新单周以及其下一周（如果有的话）
                counter = Counter(2)
                counter.onFinish = {
                    Handler(Looper.getMainLooper()).post {
                        dialog.dismiss()
                        onImported(true)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        CourseDB.get().dao().also {
                            it.deleteByZC(updateZC)
                            if (updateZC < sd.weekNum) {
                                it.deleteByZC(updateZC + 1)
                            }
                            it.insert(courseList)
                        }
                    }
                    for (course in courseList) {
                        SCLog.debug(TAG, course.run {
                            "$id: {name=$name, major=$major, zc=$zc, xq=$xq, startNode=$startNode, endNode=$endNode}"
                        })
                    }
                }

                mh.post {
                    binding.webView.evaluateJavascript(
                        gripper.getZCCourseJs(updateZC)
                    ) { data ->
                        courseList.addAll(gripper.decodeCourseData(data))
                        counter.next()
                    }
                }
                Thread.sleep(400L)
                if (updateZC < sd.weekNum) {
                    mh.post {
                        binding.webView.evaluateJavascript(
                            gripper.getZCCourseJs(updateZC + 1)
                        ) { data ->
                            courseList.addAll(gripper.decodeCourseData(data))
                            counter.next()
                        }
                    }
                    Thread.sleep(400L)
                } else counter.next()
            }
        }.onFailure {
            SCLog.error(TAG, it)
            mh.post {
                dialog.dismiss()
                onImported(false)
            }
        }
    }

    // 课表导入完毕
    private fun onImported(isSuccess: Boolean) = MainScope().launch {
        if (!isSuccess) {
            authSuccess = false
            SCLog.debug(TAG, "auth = false")
            SCToast.show("请稍后再试")
            delay(1_000L)
            finish()
            return@launch
        }
        SCToast.show("课表导入成功")
        delay(1_000L)
        finish()
    }

    class Counter(
        private val target: Int,
    ) {
        private var count = 0
        var callback: ((Int) -> Unit)? = null
        var onFinish: (() -> Unit)? = null

        fun next() {
            count++
            callback?.invoke(count)
            if (count == target) {
                onFinish?.invoke()
            }
        }
    }
}