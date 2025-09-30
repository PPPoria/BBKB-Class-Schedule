package com.bbkb.sc.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
    private val newPictureAdapter: SingleBindingAdapter<ItemNotePictureBinding, String>
        get() {
            return SingleBindingAdapter(
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

            // 显示标题，监听标题变化
            with(binding.title) {
                setText(item.title)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) =
                        Unit

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                    override fun afterTextChanged(et: Editable?) {
                        // 直接提交到数据库，本地保存副本，不走stateFlow
                        CoroutineScope(Dispatchers.IO).launch {
                            NoteItemDB.get().dao().update(item.also {
                                it.title = et.toString()
                            })
                        }
                    }
                })
            }

            // 触发展开、收起详细内容监听器
            binding.itemBg.setOnClickListenerWithClickAnimation {
                binding.unfoldBtn.rotation =
                    if (binding.unfoldBtn.rotation == 90f) -90f
                    else 90f
                if (binding.unfoldBtn.rotation == 90f) {
                    // 收起详细内容
                    with(binding) {
                        contentLayout.isGone = true
                        itemBg.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                        title.setTextColor(getColor(R.color.black))
                        description.setTextColor(getColor(R.color.gray))
                    }
                    return@setOnClickListenerWithClickAnimation
                }
                // 展开详细内容
                with(binding) {
                    contentLayout.isGone = false
                    itemBg.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                    title.setTextColor(getColor(R.color.white))
                    description.setTextColor(getColor(R.color.white))
                }
                // 显示笔记内容
                binding.contentEdit.setText(item.noteContent)
                // 显示图片列表
                with(binding.pictureList) {
                    layoutManager = GridLayoutManager(
                        this@NoteItemListActivity,
                        when (item.picturePaths.size) {
                            in 16..Int.MAX_VALUE -> 4
                            in 9..15 -> 3
                            in 4..8 -> 2
                            else -> 1
                        }
                    )
                    newPictureAdapter.also {
                        adapter = it
                        it.data = item.picturePaths
                    }
                }
            }

            // 笔记内容变化监听
            binding.contentEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) =
                    Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun afterTextChanged(et: Editable?) {
                    // 直接提交到数据库，本地保存副本，不走stateFlow
                    CoroutineScope(Dispatchers.IO).launch {
                        NoteItemDB.get().dao().update(item.also {
                            it.noteContent = et.toString()
                        })
                    }
                }
            })

            // 拍摄按钮监听
            binding.fromCameraBtn.setOnClickListener {
                Intent(
                    this@NoteItemListActivity,
                    CameraActivity::class.java
                ).also {
                    startActivity(it)
                }
            }
        }
    }

    override fun initView() {
        enableStrictMode(this::class.java, 1)
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
                // 直接提交到数据库，本地保存副本，不走stateFlow
                vm.latest?.run {
                    CoroutineScope(Dispatchers.IO).launch {
                        NoteCategoryDB.get().dao().update(
                            category.also {
                                it.name = et.toString()
                                it.timeStamp = System.currentTimeMillis()
                            }
                        )
                    }
                }
            }
        })
        addBtn.setOnClickListenerWithClickAnimation {
            CoroutineScope(Dispatchers.IO).launch {
                val new = NoteItem(
                    categoryId = categoryId,
                    timeStamp = System.currentTimeMillis(),
                    priority = 0,
                    title = "",
                    noteContent = "",
                    picturePaths = emptyList()
                )
                NoteItemDB.get().dao().insert(new)
            }
        }
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: MData(
            category = NoteCategory(
                id = categoryId,
                name = "Loading...",
                timeStamp = System.currentTimeMillis(),
                courseNames = emptyList()
            ),
            itemsFlow = emptyFlow(),
        )
        old.copy(
            category = withContext(Dispatchers.IO) {
                NoteCategoryDB.get().dao().getById(categoryId).first()
            },
            itemsFlow = NoteItemDB.get().dao().getByCategoryId(categoryId)
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
                vm.latest?.itemsFlow?.collect { list ->
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
        if (items.size == itemAdapter.itemCount) {
            /*
            * 防止重复刷新导致UI锁死
            * 这里的itemAdapter.data = items会导致UI刷新
            * 但是如果item的数量没有变化，说明是item内部的数据更新，
            * 这些内部更新的数据已经在itemAdapter中保存了数据库中的副本
            * 没有必要再次刷新列表
            */
            return
        }
        itemAdapter.data = items.sortedBy { -1 * it.timeStamp }
    }

    data class MData(
        val category: NoteCategory,
        val itemsFlow: Flow<List<NoteItem>>,
    )
}