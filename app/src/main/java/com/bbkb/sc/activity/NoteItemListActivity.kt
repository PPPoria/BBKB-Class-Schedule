package com.bbkb.sc.activity

import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityNoteItemListBinding
import com.bbkb.sc.databinding.ItemNoteItemBinding
import com.bbkb.sc.databinding.ItemNotePictureBinding
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.schedule.database.NoteItem
import com.bbkb.sc.schedule.database.NoteItemDB
import com.bumptech.glide.Glide
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteItemListActivity : BaseActivity<ActivityNoteItemListBinding>() {
    override fun onViewBindingCreate() = ActivityNoteItemListBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val categoryId by lazy { intent.getLongExtra("category_id", 0L) }
    private val newPictureAdapter: () -> SingleBindingAdapter<ItemNotePictureBinding, String> = {
        SingleBindingAdapter(
            itemLayoutId = R.layout.item_note_picture,
            vbBind = ItemNotePictureBinding::bind
        ) { binding, _, item ->
            Glide.with(this@NoteItemListActivity)
                .load(item)
                .into(binding.imageView)
            binding.imageView.setOnClickListener {

            }
        }
    }
    private val itemAdapter by lazy {
        SingleBindingAdapter<ItemNoteItemBinding, NoteItem>(
            itemLayoutId = R.layout.item_note_item,
            vbBind = ItemNoteItemBinding::bind,
            itemId = { it.id }
        ) { binding, _, item ->
            // 显示时间
            val curDate = System.currentTimeMillis().toDateFormat()
            binding.date.text = item.timeStamp.toDateFormat().run {
                if (year == curDate.year &&
                    month == curDate.month &&
                    day == curDate.day
                ) "${hour}:${minute}"
                else if (year == curDate.year) "${month}月${day}日"
                else "${year}年${month}月${day}日"
            }
            // 显示标题
            binding.title.setText(item.title)
            binding.title.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun afterTextChanged(et: Editable?) {
                    vm.latest()?.items?.find {
                        it.id == item.id
                    }?.title = et.toString()
                }
            })
            // 显示图片
            binding.itemBg.setOnClickListener {
                binding.unfoldBtn.rotation =
                    if (binding.unfoldBtn.rotation == 90f) -90f
                    else 90f
                if (binding.unfoldBtn.rotation == 90f) {
                    binding.contentLayout.isGone = true
                } else {
                    binding.contentLayout.isGone = false
                    binding.pictureList.layoutManager = GridLayoutManager(
                        this@NoteItemListActivity,
                        when (item.picturePaths.size) {
                            in 16..Int.MAX_VALUE -> 4
                            in 9..15 -> 3
                            in 4..8 -> 2
                            else -> 1
                        }
                    )
                    newPictureAdapter().also {
                        binding.pictureList.adapter = it
                        it.data = item.picturePaths
                    }
                }
            }
        }
    }

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
        with(binding.rv) {
            layoutManager = LinearLayoutManager(this@NoteItemListActivity)
            adapter = itemAdapter
        }
    }

    override fun initListener() = with(binding) {
        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val old = vm.latest() ?: return
                old.copy(
                    category = old.category.copy(
                        name = et.toString()
                    )
                ).also { vm.update(it) }
            }
        })
        addBtn.setOnClickListenerWithClickAnimation {
            CoroutineScope(Dispatchers.IO).launch {
                val new = NoteItem(
                    categoryId = categoryId,
                    timeStamp = System.currentTimeMillis(),
                    priority = 0,
                    title = getString(R.string.note_item_title_unname),
                    notes = emptyList(),
                    picturePaths = emptyList()
                )
                NoteItemDB.get().dao().insert(new)
            }
        }
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest() ?: MData(
            category = NoteCategory(
                id = categoryId,
                name = "Loading...",
                timeStamp = System.currentTimeMillis(),
                courseNames = emptyList()
            ),
            itemsFlow = emptyFlow(),
            items = emptyList()
        )
        val flow = NoteItemDB.get().dao().getByCategoryId(categoryId)
        old.copy(
            category = withContext(Dispatchers.IO) {
                NoteCategoryDB.get().dao().getById(categoryId).first()
            },
            itemsFlow = flow,
            items = withContext(Dispatchers.IO) {
                flow.first()
            }
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                vm.flow.collect { data ->
                    showCategoryAndNoteInfo(data.category)
                }
            }
            launch {
                vm.latest()?.itemsFlow?.collect { list ->
                    showNoteItems(list)
                }
            }
        }
    }

    private fun showCategoryAndNoteInfo(category: NoteCategory) {
        // 防止循环设置导致UI锁死
        if (binding.name.text.toString() != category.name) {
            binding.name.setText(category.name)
        }
        "${category.name.length}/60".also {
            binding.nameLengthLimit.text = it
        }
    }

    private fun showNoteItems(items: List<NoteItem>) {
        itemAdapter.data = items.sortedBy { -1 * it.timeStamp }
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch {
            vm.latest()?.run {
                NoteCategoryDB.get().dao().update(
                    category.copy(
                        name = category.name.ifEmpty { getString(R.string.note_name_unname) },
                        timeStamp = System.currentTimeMillis()
                    )
                )
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            vm.latest()?.run {
                NoteItemDB.get().dao().update(
                    items.map {
                        it.copy(
                            title = it.title.ifEmpty { getString(R.string.note_item_title_unname) },
                        )
                    }
                )
            }
        }
    }

    data class MData(
        val category: NoteCategory,
        val itemsFlow: Flow<List<NoteItem>>,
        val items: List<NoteItem>
    )
}