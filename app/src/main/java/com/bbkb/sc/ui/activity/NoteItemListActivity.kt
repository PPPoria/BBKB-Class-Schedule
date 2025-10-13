package com.bbkb.sc.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityNoteItemListBinding
import com.bbkb.sc.databinding.ItemNoteItemBinding
import com.bbkb.sc.databinding.ItemNotePictureBinding
import com.bbkb.sc.ui.dialog.ConfirmDialog
import com.bbkb.sc.ui.dialog.ImagePreviewDialog
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.schedule.database.NoteItem
import com.bbkb.sc.schedule.database.NoteItemDB
import com.bbkb.sc.ui.dialog.RelatedCoursesDialog
import com.bbkb.sc.util.FileManager
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@SuppressLint("DefaultLocale")
class NoteItemListActivity : BaseActivity<ActivityNoteItemListBinding>() {
    override fun onViewBindingCreate() = ActivityNoteItemListBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val categoryId by lazy { intent.getLongExtra(KEY_CATEGORY_ID, 0L) }
    private val newPictureAdapter: SingleBindingAdapter<ItemNotePictureBinding, NoteItem>
        get() {
            return SingleBindingAdapter(
                itemLayoutId = R.layout.item_note_picture,
                vbBind = ItemNotePictureBinding::bind
            ) { binding, position, item, adapter ->
                Glide.with(this@NoteItemListActivity)
                    .load(item.picturePaths.reversed()[position])
                    .into(binding.imageView)
                binding.imageView.setOnClickListener {
                    ImagePreviewDialog().apply {
                        this.position = position
                        pathList = item.picturePaths.reversed()
                        changedPathsCallback = { paths ->
                            // 图片数量变化，说明有删除操作
                            if (paths.size != adapter.data.size) {
                                item.picturePaths = paths.reversed()
                                // 触发Rv更新
                                adapter.data = item.picturePaths.map { item }
                                // 保存到数据库
                                CoroutineScope(Dispatchers.IO).launch {
                                    NoteItemDB.get().dao().update(item)
                                }
                            }
                        }
                        show(supportFragmentManager, "ImagePreviewDialog")
                    }
                }
            }
        }
    private val itemAdapter by lazy {
        SingleBindingAdapter<ItemNoteItemBinding, NoteItem>(
            itemLayoutId = R.layout.item_note_item,
            vbBind = ItemNoteItemBinding::bind,
            itemId = { it.id }
        ) { binding, _, item, _ ->
            /*显示时间*/
            val curDate = System.currentTimeMillis().toDateFormat()
            binding.date.text = item.timeStamp.toDateFormat().run {
                if (year == curDate.year &&
                    month == curDate.month &&
                    day == curDate.day
                ) "${String.format("%02d", hour)}:${String.format("%02d", minute)}"
                else if (year == curDate.year) "${month}月${day}日"
                else "${year}年${month}月${day}日"
            }

            /*显示标题，监听标题变化*/
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

            /*显示描述*/
            val updateDescription = {
                binding.description.text = StringBuilder().run {
                    append(item.noteContent.length).append("字")
                    append(" ")
                    append(item.picturePaths.size).append("图")
                    toString()
                }
            }
            updateDescription()

            /*触发展开、收起详细内容监听器*/
            val pictureAdapter = newPictureAdapter
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
                    adapter = pictureAdapter
                    pictureAdapter.data = item.picturePaths.map { item }
                }
            }

            /*文字内容变化监听*/
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
                        withContext(Dispatchers.Main) {
                            updateDescription()
                        }
                    }
                }
            })

            /*
            * 拍摄按钮监听
            * 相册选取按钮监听
            */
            val updatePicture: (paths: List<String>) -> Unit = { paths ->
                // 更新图片列表
                item.picturePaths = item.picturePaths
                    .toMutableList().also { list ->
                        list.addAll(paths)
                    }
                binding.pictureList.layoutManager = GridLayoutManager(
                    this@NoteItemListActivity,
                    when (item.picturePaths.size) {
                        in 16..Int.MAX_VALUE -> 4
                        in 9..15 -> 3
                        in 4..8 -> 2
                        else -> 1
                    }
                )
                pictureAdapter.data = item.picturePaths.map { item }
                // 更新描述
                updateDescription()
                // 保存到数据库
                CoroutineScope(Dispatchers.IO).launch {
                    NoteItemDB.get().dao().update(item)
                }
            }
            binding.fromCameraBtn.setOnClickListener {
                onInsertPictureSuccess = updatePicture
                Intent(
                    this@NoteItemListActivity,
                    CameraActivity::class.java
                ).also { cameraLauncher.launch(it) }
            }
            binding.fromGalleryBtn.setOnClickListener {
                onInsertPictureSuccess = updatePicture
                pickImagesLauncher.launch("image/*")
            }

            /*长按弹出菜单*/
            binding.itemBg.setOnLongClickListener {
                val popup = PopupMenu(binding.root.context, binding.root)
                popup.menuInflater.inflate(com.poria.base.R.menu.menu_note_popup, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        com.poria.base.R.id.action_delete -> ConfirmDialog().also {
                            it.title = "确认删除\"${item.title}\"?"
                            it.content = getString(R.string.delete_note)
                            it.confirmBgColor = getColor(R.color.tertiary)
                            it.confirmTextColor = getColor(R.color.white)
                            it.onConfirm = { deleteItem(item) }
                            it.show(supportFragmentManager, "delete_dialog")
                        }
                    }
                    true
                }
                popup.show()
                true
            }
        }
    }
    private var onInsertPictureSuccess: (paths: List<String>) -> Unit = {}
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        callback = { result ->
            if (result.resultCode == RESULT_OK) {
                val path = result.data?.getStringExtra("path")
                    ?: return@registerForActivityResult
                onInsertPictureSuccess(listOf(path))
            }
        }
    )
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
        callback = { uriList ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (uriList.isEmpty()) return@launch
                val pathList = uriList.mapNotNull {
                    contentResolver.openInputStream(it)?.use { ins ->
                        val file = File(filesDir, "multi_pics/${UUID.randomUUID()}.jpg")
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { out -> ins.copyTo(out) }
                        file.absolutePath   // 内部路径
                    }
                }
                withContext(Dispatchers.Main) {
                    onInsertPictureSuccess(pathList)
                }
            }
        }
    )

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
                // 直接提交到数据库，本地保存副本，不走stateFlow
                lifecycleScope.launch(Dispatchers.IO) {
                    vm.latest?.category?.run {
                        NoteCategoryDB.get().dao().update(
                            this@run.also {
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
        showRelatedCoursesBtn.setOnClickListenerWithClickAnimation {
            RelatedCoursesDialog().also {
                it.categoryId = vm.latest?.category?.id ?: return@also
                it.show(supportFragmentManager, "related_courses_dialog")
            }
        }
    }

    private fun deleteItem(item: NoteItem) {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                NoteItemDB.get().dao().delete(item)
                withContext(Dispatchers.Main) {
                    itemAdapter.data = itemAdapter.data.filter { it.id != item.id }
                }
            }
            launch {
                for (path in item.picturePaths) {
                    FileManager.deleteInnerImageFromGallery(path)
                }
            }
        }
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: MData(
            category = withContext(Dispatchers.IO) {
                NoteCategoryDB.get().dao().getById(categoryId).first()
            },
        )
        old.copy().also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                vm.flow.collect { data ->
                    showCategoryAndNoteInfo(data.category)
                    showNoteItems(data.items)
                    binding.noNoteTips.isGone = data.items.isNotEmpty()
                }
            }
            launch {
                val itemsFlow = NoteItemDB.get().dao().getByCategoryId(categoryId)
                itemsFlow.collect { list ->
                    vm.latest?.copy(
                        items = list
                    )?.let { vm.update(it) }
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
            /**
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
        val items: List<NoteItem> = emptyList(),
    )

    companion object {
        const val KEY_CATEGORY_ID = "category_id"
    }
}