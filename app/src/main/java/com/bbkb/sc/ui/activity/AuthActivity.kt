package com.bbkb.sc.ui.activity

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.lifecycleScope
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.ActivityAuthBinding
import com.bbkb.sc.datastore.LongKeys
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.ui.dialog.LoadingDialog
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.schedule.gripper.Gripper
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.util.SCLog
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.store.DSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

private const val TAG = "AuthActivity"

class AuthActivity : BaseActivity<ActivityAuthBinding>() {
    override fun onViewBindingCreate() = ActivityAuthBinding.inflate(layoutInflater)
    private val schoolId by lazy { intent.getIntExtra(KEY_SCHOOL_ID, 0) }
    private val sd by lazy { School.dataList.find { schoolId == it.id }!! }
    private val gripper: Gripper by lazy { Gripper.getGripperBySchoolId(schoolId) }
    private val updateZC by lazy {
        // 0表示更新全部
        intent.getIntExtra(KEY_UPDATE_ZC, 0)
            .coerceIn(0, sd.weekNum)
    }
    private lateinit var dialog: LoadingDialog
    private lateinit var counter: Counter
    private val courseList = ArrayList<Course>()
    private var authSuccess = false

    override fun initView() {
        if (SCApp.isDarkTheme) {
            setLightStatusBar(false)
            setLightNavigationBar(false)
        }
        setLightStatusBar(true)
        setLightNavigationBar(true)
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
                if (!authSuccess) {
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
    private fun onAuth(isSuccess: Boolean) {
        // 修改状态
        if (!isSuccess) {
            authSuccess = false
            SCLog.debug(TAG, "auth = false")
            return
        }
        authSuccess = true
        SCLog.debug(TAG, "auth = true")
        // 弹出等待弹窗
        dialog = LoadingDialog().also {
            it.title = getString(R.string.importing)
            it.show(supportFragmentManager, it.title)
        }
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                // 等待前往目标页面
                for (js in gripper.getStepsJsAfterAuth()) {
                    withContext(Dispatchers.Main) {
                        binding.webView.evaluateJavascript(js, null)
                    }
                    delay(666L)
                }
                // 抓取课程信息
                delay(5_000L)
                // 更新全部；如果没有更新单周方法亦更新全部
                if (
                    updateZC == 0 ||
                    gripper.getZCCourseJs(updateZC).isEmpty() ||
                    gripper.getZCCourseJs(updateZC).isBlank()
                ) {
                    val jsList = gripper.getAllCoursesJs()
                    counter = Counter(jsList.size)
                    counter.onFinish = {
                        CoroutineScope(Dispatchers.Default).launch {
                            dialog.dismiss()
                            onImported(true)
                        }
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
                        // 保存课程信息
                        CoroutineScope(Dispatchers.IO).launch {
                            DSManager.setLong(LongKeys.FIRST_ZC_MONDAY_TIME_STAMP, stamp)
                            DSManager.setString(StringKeys.SCHOOL_NAME, gripper.schoolName)
                            CourseDB.get().dao().also {
                                it.deleteAll()
                                it.insert(courseList)
                            }
                        }
                    }
                    // 开始抓取课程信息
                    for (js in gripper.getAllCoursesJs()) {
                        withContext(Dispatchers.Main) {
                            binding.webView.evaluateJavascript(js) { data ->
                                courseList.addAll(gripper.decodeCourseData(data))
                                counter.next()
                            }
                        }
                        delay(400L)
                    }
                } else { // 更新单周以及其下一周（如果有的话）
                    counter = Counter(2)
                    counter.onFinish = {
                        lifecycleScope.launch {
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
                    }
                    withContext(Dispatchers.Main) {
                        binding.webView.evaluateJavascript(
                            gripper.getZCCourseJs(updateZC)
                        ) { data ->
                            courseList.addAll(gripper.decodeCourseData(data))
                            counter.next()
                        }
                    }
                    delay(400L)
                    if (updateZC < sd.weekNum) {
                        withContext(Dispatchers.Main) {
                            binding.webView.evaluateJavascript(
                                gripper.getZCCourseJs(updateZC + 1)
                            ) { data ->
                                courseList.addAll(gripper.decodeCourseData(data))
                                counter.next()
                            }
                        }
                        delay(400L)
                    } else counter.next()
                }
            }.onFailure {
                SCLog.error(TAG, it)
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    onImported(false)
                }
            }
        }
    }

    // 课表导入完毕
    private fun onImported(isSuccess: Boolean) = lifecycleScope.launch {
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

    companion object {
        const val KEY_SCHOOL_ID = "school_id"
        const val KEY_UPDATE_ZC = "update_zc"
    }
}