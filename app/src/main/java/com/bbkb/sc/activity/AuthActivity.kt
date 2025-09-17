package com.bbkb.sc.activity

import android.annotation.SuppressLint
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
import com.bbkb.sc.schedule.data.Course
import com.bbkb.sc.schedule.data.CourseDB
import com.bbkb.sc.schedule.Gripper
import com.bbkb.sc.util.SCLog
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.store.DSHelper
import com.poria.base.util.ThreadPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayList

private const val TAG = "AuthActivity"

class AuthActivity : BaseActivity<ActivityAuthBinding>() {
    private val courseList = ArrayList<Course>()
    private lateinit var gripper: Gripper
    private var authSuccess = false

    override fun onViewBindingCreate() = ActivityAuthBinding.inflate(layoutInflater)

    override fun initView() {
        setLightStatusBar(false)
        setLightStatusBar(false)
        Gripper.getGripperBySchoolId(
            intent.getIntExtra("school_id", 0)
        ).also { gripper = it }
        showAuthView()
    }

    // 开启一个浏览器，模拟用户操作
    @SuppressLint("SetJavaScriptEnabled")
    private fun showAuthView() = binding.webView.apply {
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
                    this@apply.evaluateJavascript(gripper.getCheckAuthJs()) {
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
        if (!isSuccess) {
            authSuccess = false
            SCLog.debug(TAG, "auth = false")
            return@execute
        }
        val mh = android.os.Handler(Looper.getMainLooper())
        // 修改状态
        authSuccess = true
        SCLog.debug(TAG, "auth = true")
        val dialog = LoadingDialog()
            .also {
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
            for (js in gripper.getAllCoursesJs()) {
                mh.post {
                    binding.webView.evaluateJavascript(js) { data ->
                        courseList.addAll(gripper.decodeCourseData(data))
                    }
                }
                Thread.sleep(400L)
            }
            for (course in courseList) {
                SCLog.debug(TAG, course.run {
                    "$id: {name=$name, major=$major, zc=$zc, xq=$xq, startNode=$startNode, endNode=$endNode}"
                })
            }
            // 保存课程信息
            CoroutineScope(Dispatchers.IO).launch {
                val oneDay = 86_400_000L
                val oneWeek = oneDay * 7
                val stamp = courseList.first().let {
                    val offset = (it.zc - 1) * oneWeek + (it.xq - 1) * oneDay
                    it.timeStamp - offset
                }
                DSHelper.setLong(LongKeys.FIRST_ZC_MONDAY_TIME_STAMP, stamp)
                DSHelper.setString(StringKeys.SCHOOL_NAME, gripper.schoolName)
                CourseDB.get().dao().also {
                    it.deleteAll()
                    it.insert(courseList)
                }
            }
            mh.post {
                dialog.dismiss()
                onImported(true)
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
}