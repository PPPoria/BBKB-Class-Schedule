package com.bbkb.sc.ui.activity

import android.content.Intent
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.ActivityTableBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.isNightModeYes
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.schedule.TableAttr
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.ui.fragment.TableConfigFragment
import com.bbkb.sc.ui.fragment.TableFragment
import com.bbkb.sc.util.SCToast
import com.bumptech.glide.Glide
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TableActivity : BaseActivity<ActivityTableBinding>() {
    override fun onViewBindingCreate() = ActivityTableBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val whoStartMe by lazy { intent.getStringExtra(SplashActivity.KEY_TAG_WHO_START_ME) }
    val path = bgImgPath
    override val baseFragmentTagList: List<String>
        get() = listOf("table_fragment", "filter_fragment")

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        initHasBgImgMode()
        super.onCreate(savedInstanceState)
    }

    private fun initHasBgImgMode() {
        if (path.isNotEmpty()) {
            // 如果有背景图片，则显示强制黑夜模式
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else if (isNightModeYes() != SCApp.app.isNightModeYes()) {
            // 如果背景图片不存在，且当前模式与系统模式不同，选择跟随系统模式
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    override fun initWindowInsets(l: Int, t: Int, r: Int, b: Int) {
        super.initWindowInsets(l, t, r, b)
        binding.root.setPadding(0, 0, 0, 0)
        binding.mainLayout.setPadding(
            systemBarPadding[l],
            systemBarPadding[t],
            systemBarPadding[r],
            systemBarPadding[b]
        )
    }

    override fun initView() {
        if (path.isNotEmpty()) {
            Glide.with(this)
                .load(path)
                .centerCrop()
                .into(binding.bgImg)
        }
    }

    override fun addFragment() {
        supportFragmentManager.commit {
            val bundle = bundleOf(
                KEY_MODE to intent.getIntExtra(KEY_MODE, MODE_NORMAL),
                KEY_NOTE_CATEGORY_ID to intent.getLongExtra(KEY_NOTE_CATEGORY_ID, 0L)
            )
            setReorderingAllowed(true)
            add<TableFragment>(
                R.id.table_fragment_container,
                tag = baseFragmentTagList[0],
                args = bundle
            )
            add<TableConfigFragment>(
                R.id.table_config_fragment_container,
                tag = baseFragmentTagList[1],
            )
        }
    }

    override fun initListener() = with(binding) {
        onBackPressedDispatcher.addCallback {
            if (tableMenuBg.isVisible) tableMenuBg.isVisible = false
            else {
                if (whoStartMe == SplashActivity.TAG) {
                    startActivity(Intent(this@TableActivity, MainActivity::class.java))
                }
                finish()
            }
        }
        moreBtn.setOnClickListenerWithClickAnimation {
            tableMenuBg.also { it.isVisible = !it.isVisible }
        }
        tableMenuBg.setOnClickListener { tableMenuBg.isVisible = false }
        tableMenuLayout.setOnClickListener { tableMenuBg.isVisible = false }
    }.let { }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: DSManager.run {
            val name = getString(StringKeys.SCHOOL_NAME).first()
            val sd = School.dataList.find { it.name == name }
            if (sd == null) {
                SCToast.show(getString(R.string.please_bind_school))
                return
            }
            MData(schoolData = sd.copy())
        }
        val curZC = ScheduleUtils.getZC(System.currentTimeMillis())
        val tableZC = if (old.tableZC == 0) curZC else old.tableZC
        old.copy(
            curZC = curZC,
            tableZC = tableZC,
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                System.currentTimeMillis().toDateFormat().run {
                    "$year/$month/$day"
                }.also { binding.todayDate.text = it }
                "第${data.curZC}周".also { binding.todayZc.text = it }
                if (bgImgPath.isNotEmpty()) { // 有图片情况下，才允许调整遮罩透明度
                    binding.bgImg.isVisible = true
                    binding.bgMask.isVisible = true
                    binding.bgMask.alpha = data.tableAttr.tableBgImgMaskAlpha
                }
            }
        }
    }

    data class MData(
        val lastUpdateTime: Long = System.currentTimeMillis(),
        val schoolData: School.SchoolData,
        val curZC: Int = 0,
        val tableZC: Int = 0,
        val tableAttr: TableAttr = TableAttr.latest,
        val preCourses: List<Course> = emptyList(),
        val curCourses: List<Course> = emptyList(),
        val nextCourses: List<Course> = emptyList(),
    )

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_NOTE_CATEGORY_ID = "category_id"
        const val MODE_NORMAL = 0
        const val MODE_ADD_RELATED_NOTE = 1
        val bgImgPath: String
            get() = runBlocking {
                DSManager.getString(
                    StringKeys.TABLE_BACKGROUND_IMG_PATH,
                    ""
                ).first()
            }
    }
}