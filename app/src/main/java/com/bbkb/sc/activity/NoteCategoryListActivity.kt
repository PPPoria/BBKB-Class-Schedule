package com.bbkb.sc.activity

import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityNoteCategoryListBinding
import com.bbkb.sc.databinding.ItemNoteCategoryBinding
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseActivity
import com.poria.base.ext.toDateFormat
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NoteCategoryListActivity : BaseActivity<ActivityNoteCategoryListBinding>() {
    override fun onViewBindingCreate() = ActivityNoteCategoryListBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val adapter by lazy {
        SingleBindingAdapter<ItemNoteCategoryBinding, NoteCategory>(
            itemLayoutId = R.layout.item_note_category,
            vbBind = ItemNoteCategoryBinding::bind,
            itemId = { it.id.toLong() }
        ) { parcel ->
            with(parcel.binding) {
                title.text = parcel.item.name
                date.text = System.currentTimeMillis().toDateFormat().toString()
            }
        }
    }

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
        with(binding.rv) {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = this@NoteCategoryListActivity.adapter
        }
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest() ?: MData(emptyList())
        /*old.copy(
            categories = withContext(Dispatchers.IO) {
                NoteCategoryDB.get().dao().getAll().first()
            }
        ).also { vm.update(it) }*/
        old.copy(
            categories = withContext(Dispatchers.Default) {
                mutableListOf<NoteCategory>().also { list ->
                    for (i in 0 until 50) {
                        NoteCategory(
                            id = i,
                            name = "课".repeat((4..30).random()),
                            courseNames = emptyList()
                        ).also { list.add(it) }
                    }
                }
            }
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                adapter.data = data.categories
            }
        }
    }

    data class MData(
        val categories: List<NoteCategory>
    )
}