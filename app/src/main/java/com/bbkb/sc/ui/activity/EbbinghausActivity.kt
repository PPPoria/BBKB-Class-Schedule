package com.bbkb.sc.ui.activity

import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityEbbinghausBinding
import com.bbkb.sc.databinding.ItemProblemCardBinding
import com.bbkb.sc.schedule.database.Problem
import com.bbkb.sc.schedule.database.ProblemDB
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseActivity
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.launch

class EbbinghausActivity : BaseActivity<ActivityEbbinghausBinding>() {
    override fun onViewBindingCreate() = ActivityEbbinghausBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val snapHelper by lazy { PagerSnapHelper() }
    private val adapter by lazy {
        SingleBindingAdapter(
            itemLayoutId = R.layout.item_problem_card,
            vbBind = ItemProblemCardBinding::bind,
            itemId = { it.id },
            onBindView = { binding, position, item, _ ->
                binding.initItem(position, item)
            }
        )
    }

    override fun initView() {
        binding.rv.also {
            it.layoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            it.adapter = adapter
            snapHelper.attachToRecyclerView(it)
        }
    }

    private fun ItemProblemCardBinding.initItem(position: Int, item: Problem) {

    }

    override fun initListener() {
        binding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val snapView = snapHelper.findSnapView(rv.layoutManager) ?: return
                    val position = rv.getChildAdapterPosition(snapView)
                    // position 即为当前“焦点”页
                    vm.latest?.copy(
                        position = position
                    )?.also { vm.update(it) }
                }
            }
        })
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: MData()
        vm.update(old)
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                vm.flow.collect { data ->
                    adapter.data = data.allProblems.filter {
                        it.category in data.categories
                    }
                    binding.rv.smoothScrollToPosition(data.position)
                }
            }
            launch {
                ProblemDB.getInstance().dao().getAll().collect {
                    vm.update(MData(allProblems = it))
                }
            }
        }
    }

    data class MData(
        val lastUpdated: Long = System.currentTimeMillis(),
        val allProblems: List<Problem> = emptyList(),
        val categories: List<String> = emptyList(),
        val position: Int = 0
    )
}