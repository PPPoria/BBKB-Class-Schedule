package com.poria.base.base

import android.graphics.Color
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

abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {
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

    fun setImmersiveLayout(isImmersive: Boolean = true) {
        if (isImmersive) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}