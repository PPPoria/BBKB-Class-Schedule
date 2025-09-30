package com.poria.base.base

import android.graphics.Color
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.poria.base.R
import kotlinx.coroutines.launch

abstract class BaseActivity<out T : ViewBinding> : AppCompatActivity() {
    lateinit var systemBarPadding: IntArray
    val binding by lazy { onViewBindingCreate() }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBarPadding = intArrayOf(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            v.setPadding(0, 0, 0, 0)
            initWindowInsets()
            insets
        }
        initView()
        initListener()
        lifecycleScope.launch {
            refreshDataInScope()
            observeDataInScope()
        }
    }

    override fun onRestart() {
        super.onRestart()
        lifecycleScope.launch {
            refreshDataInScope()
        }
    }

    abstract fun onViewBindingCreate(): T
    open fun initWindowInsets(l: Int = 0, t: Int = 1, r: Int = 2, b: Int = 3) {
        binding.root.setPadding(
            systemBarPadding[l],
            systemBarPadding[t],
            systemBarPadding[r],
            systemBarPadding[b]
        )
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    open fun initView() {}
    open fun initListener() {}
    open suspend fun refreshDataInScope() {}
    open suspend fun observeDataInScope() {}

    fun setLightStatusBar(isLight: Boolean = true) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = isLight
    }

    fun setLightNavigationBar(isLight: Boolean = true) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars = isLight
    }

    // 要放在initWindowInsets()中的super.initWindowInsets()之后调用才能正常全屏显示
    fun setImmersiveLayout(isImmersive: Boolean = true) {
        if (isImmersive) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            binding.root.setPadding(0, 0, 0, 0)
        } else {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.systemBars())
            }
            binding.root.setPadding(
                systemBarPadding[0],
                systemBarPadding[1],
                systemBarPadding[2],
                systemBarPadding[3]
            )
        }
    }

    /*
    * 开启StrictMode（严格模式）
    * debug版本下使用
    * release版本请停止调用！！！
    * release版本请停止调用！！！
    * release版本请停止调用！！！
    */
    fun enableStrictMode(activityClass: Class<out BaseActivity<*>>,instanceLimit: Int = 1) {
        //开启Thread策略模式
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder().detectNetwork() //监测主线程使用网络io
                .detectCustomSlowCalls() //监测自定义运行缓慢函数
                .detectDiskReads() // 检测在UI线程读磁盘操作
                .detectDiskWrites() // 检测在UI线程写磁盘操作
                .penaltyLog() //写入日志
                .penaltyDialog() //监测到上述状况时弹出对话框
                .build()
        )
        //开启VM策略模式
        StrictMode.setVmPolicy(
            VmPolicy.Builder().detectLeakedSqlLiteObjects() //监测sqlite泄露
                .detectLeakedClosableObjects() //监测没有关闭IO对象
                .setClassInstanceLimit(activityClass, instanceLimit) // 设置某个类的同时处于内存中的实例上限，可以协助检查内存泄露
                .detectActivityLeaks()
                .penaltyLog() //写入日志
                .penaltyDeath() //出现上述情况异常终止
                .build()
        )
    }
}