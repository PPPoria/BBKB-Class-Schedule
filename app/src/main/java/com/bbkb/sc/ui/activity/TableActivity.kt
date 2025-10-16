package com.bbkb.sc.ui.activity

import android.content.res.ColorStateList
import android.content.res.Configuration
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.ActivityTableBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.schedule.TableConfig
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TableActivity : BaseActivity<ActivityTableBinding>() {
    override fun onViewBindingCreate() = ActivityTableBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val tableFragmentTag = "table_fragment"
    private val tableConfigFragmentTag = "filter_fragment"

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

    override fun addFragment() {
        supportFragmentManager.commit {
            val bundle = bundleOf(
                KEY_MODE to intent.getIntExtra(KEY_MODE, MODE_NORMAL),
                KEY_NOTE_CATEGORY_ID to intent.getLongExtra(KEY_NOTE_CATEGORY_ID, 0L)
            )
            setReorderingAllowed(true)
            add<TableFragment>(
                R.id.table_fragment_container,
                tag = tableFragmentTag,
                args = bundle
            )
            add<TableConfigFragment>(
                R.id.table_config_fragment_container,
                tag = tableConfigFragmentTag,
            )
        }
    }

    override fun initListener() = with(binding) {
        onBackPressedDispatcher.addCallback {
            if (filterBg.isVisible) filterBg.isVisible = false
            else finish()
        }
        moreBtn.setOnClickListenerWithClickAnimation { _ ->
            filterBg.also { it.isVisible = !it.isVisible }
        }
        filterBg.setOnClickListener { filterBg.isVisible = false }
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
        val tableConfig = withContext(Dispatchers.Default) {
            ScheduleUtils.getTableConfig()
        }
        val path = DSManager.getString(
            StringKeys.TABLE_BACKGROUND_IMG_PATH,
            ""
        ).first()
        old.copy(
            curZC = curZC,
            tableZC = tableZC,
            tableConfig = tableConfig,
            bgImgPath = path
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                System.currentTimeMillis().toDateFormat().run {
                    "$year/$month/$day"
                }.also { binding.todayDate.text = it }
                "第${data.curZC}周".also { binding.todayZc.text = it }
                if (data.bgImgPath.isNotEmpty())
                    setAttrHasBgImg(data)
                else if (SCApp.isDarkTheme) setAttrDark(data)
                else setAttrLight(data)
            }
        }
    }

    private fun setAttrLight(data: MData) {
        binding.bgImg.isVisible = false
        binding.bgMask.isVisible = false
        binding.main.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
        binding.todayDate.setTextColor(getColor(R.color.black))
        binding.todayZc.setTextColor(getColor(R.color.black))
    }

    private fun setAttrDark(data: MData) {
        binding.bgImg.isVisible = false
        binding.bgMask.isVisible = false
    }

    private fun setAttrHasBgImg(data: MData) {
        binding.bgImg.isVisible = true
        binding.bgMask.isVisible = true
        Glide.with(this@TableActivity)
            .load(data.bgImgPath)
            .centerCrop()
            .into(binding.bgImg)
    }

    data class MData(
        val schoolData: School.SchoolData,
        val curZC: Int = 0,
        val tableZC: Int = 0,
        val tableConfig: TableConfig = TableConfig(),
        val bgImgPath: String = "",
        val preCourses: List<Course> = emptyList(),
        val curCourses: List<Course> = emptyList(),
        val nextCourses: List<Course> = emptyList(),
    )

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_NOTE_CATEGORY_ID = "category_id"
        const val MODE_NORMAL = 0
        const val MODE_ADD_RELATED_NOTE = 1
    }
}