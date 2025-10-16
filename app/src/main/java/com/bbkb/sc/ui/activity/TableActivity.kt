package com.bbkb.sc.ui.activity

import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityTableBinding
import com.bbkb.sc.datastore.StringKeys
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.School
import com.bbkb.sc.schedule.TableConfig
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.ui.fragment.TableConfigFragment
import com.bbkb.sc.ui.fragment.TableFragment
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.store.DSManager
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TableActivity : BaseActivity<ActivityTableBinding>() {
    override fun onViewBindingCreate() = ActivityTableBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val tableFragmentTag = "table_fragment"
    private val tableConfigFragmentTag = "filter_fragment"

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
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
            getString(StringKeys.SCHOOL_NAME).first()
        }.let { name ->
            School.dataList.find { it.name == name }
        }.also {
            if (it == null) {
                SCToast.show(getString(R.string.please_bind_school))
                return
            }
        }.let { MData(schoolData = it!!.copy()) }
        val curZC = ScheduleUtils.getZC(System.currentTimeMillis())
        val tableZC = if (old.tableZC == 0) curZC else old.tableZC
        old.copy(
            curZC = curZC,
            tableZC = tableZC,
            tableConfig = withContext(Dispatchers.IO) {
                ScheduleUtils.getTableConfig()
            }
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                System.currentTimeMillis().toDateFormat().run {
                    "$year/$month/$day"
                }.also { binding.todayDate.text = it }
                "第${data.curZC}周".also { binding.todayZc.text = it }
            }
        }
    }

    data class MData(
        val schoolData: School.SchoolData,
        val curZC: Int = 0,
        val tableZC: Int = 0,
        val tableConfig: TableConfig = TableConfig(),
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