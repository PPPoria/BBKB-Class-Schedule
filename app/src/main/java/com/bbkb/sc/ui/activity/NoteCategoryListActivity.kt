package com.bbkb.sc.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.ActivityNoteCategoryListBinding
import com.bbkb.sc.databinding.ItemNoteCategoryBinding
import com.bbkb.sc.ui.dialog.ConfirmDialog
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.schedule.database.NoteItemDB
import com.bbkb.sc.util.FileManager
import com.bbkb.sc.util.SCToast
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("DefaultLocale")
class NoteCategoryListActivity : BaseActivity<ActivityNoteCategoryListBinding>() {
    override fun onViewBindingCreate() = ActivityNoteCategoryListBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val mode by lazy { intent.getIntExtra(KEY_MODE, MODE_NORMAL) }
    private val addedCourseName by lazy { intent.getStringExtra(KEY_COURSE_NAME) }
    private val whiteStateList = ColorStateList.valueOf("#FFFFFF".toColorInt())
    private val grayStateList = ColorStateList.valueOf(SCApp.app.getColor(R.color.gray_shade))
    private val adapter by lazy {
        SingleBindingAdapter<ItemNoteCategoryBinding, NoteCategory>(
            itemLayoutId = R.layout.item_note_category,
            vbBind = ItemNoteCategoryBinding::bind,
            itemId = { it.id }
        ) { binding, _, item, _ ->
            val curDate = System.currentTimeMillis().toDateFormat()
            binding.title.apply {
                text = item.name.ifEmpty {
                    getString(R.string.note_name_unname)
                }
                setTextColor(
                    if (item.name.isEmpty()) getColor(R.color.gray)
                    else getColor(R.color.black)
                )
            }
            binding.date.text = item.timeStamp.toDateFormat().run {
                if (year == curDate.year &&
                    month == curDate.month &&
                    day == curDate.day
                ) "${String.format("%02d", hour)}:${String.format("%02d", minute)}"
                else if (year == curDate.year) "${month}月${day}日"
                else "${year}年${month}月${day}日"
            }
            // 关键字高亮
            vm.latest?.keywords?.also {
                if (it.isNotEmpty()) {
                    val spanStr = SpannableString(item.name)
                    val start = item.name.indexOf(it, 0, true)
                    if (start >= 0) {
                        val end = start + it.length
                        spanStr.setSpan(
                            ForegroundColorSpan(getColor(R.color.quaternary)),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.title.text = spanStr
                    }
                }
            }

            /**
             * 点击事件
             * 根据不同的mode，执行不同操作
             */
            binding.bg.isEnabled = if (mode == MODE_ADD_RELATED_COURSE) {
                !item.courseNames.contains(addedCourseName)
            } else true
            binding.bg.backgroundTintList = if (binding.bg.isEnabled) {
                whiteStateList
            } else grayStateList
            binding.bg.setOnClickListener {
                if (mode == MODE_NORMAL) { // 进入笔记列表
                    Intent(
                        this@NoteCategoryListActivity,
                        NoteItemListActivity::class.java
                    ).also {
                        it.putExtra(NoteItemListActivity.KEY_CATEGORY_ID, item.id)
                        startActivity(it)
                    }
                } else if (mode == MODE_ADD_RELATED_COURSE) { // 添加关联课程，然后退出
                    val courseNames = item.courseNames.toMutableList()
                    if (courseNames.contains(addedCourseName)) {
                        // 已经添加过该课程，则提示
                        // 但是上面已经做了筛选，应该不会出现这种情况
                        SCToast.show("该课程已经添加到该笔记")
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        courseNames.add(addedCourseName ?: return@launch)
                        val new = item.copy(courseNames = courseNames)
                        NoteCategoryDB.get().dao().update(new)
                        withContext(Dispatchers.Main) { finish() }
                    }
                }
            }

            // 长按弹出菜单
            binding.bg.setOnLongClickListener {
                val popup = PopupMenu(binding.root.context, binding.root)
                popup.menuInflater.inflate(com.poria.base.R.menu.menu_note_popup, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        com.poria.base.R.id.action_delete -> ConfirmDialog().also {
                            it.title = "确认删除\"${item.name}\"?"
                            it.content = getString(R.string.remove_related_note)
                            it.confirmBgColor = getColor(R.color.tertiary)
                            it.confirmTextColor = getColor(R.color.white)
                            it.onConfirm = { deleteCategory(item) }
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

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
        binding.title.text = when (mode) {
            MODE_ADD_RELATED_COURSE -> getString(R.string.select_note_need_to_relate)
            else -> getString(R.string.all_note)
        }
        with(binding.rv) {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = this@NoteCategoryListActivity.adapter
        }
    }

    override fun initListener() = with(binding) {
        onBackPressedDispatcher.addCallback {
            if (searchEdit.text.toString().isEmpty()) finish()
            else searchEdit.setText("")
        }
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                lifecycleScope.launch {
                    binding.spaceEdit.text = et
                }
                val old = vm.latest ?: return
                old.copy(
                    keywords = et.toString()
                ).also { vm.update(it) }
            }
        })
        addBtn.setOnClickListenerWithClickAnimation {
            CoroutineScope(Dispatchers.IO).launch {
                val (name, courseNames) = if (
                    mode == MODE_ADD_RELATED_COURSE &&
                    addedCourseName != null
                ) addedCourseName!! to listOf(addedCourseName!!)
                else getString(R.string.note_name_unname) to emptyList()
                val newId = NoteCategory(
                    name = name,
                    timeStamp = System.currentTimeMillis(),
                    courseNames = courseNames
                ).let { NoteCategoryDB.get().dao().insert(it) }
                Intent(
                    this@NoteCategoryListActivity,
                    NoteItemListActivity::class.java
                ).also {
                    it.putExtra(NoteItemListActivity.KEY_CATEGORY_ID, newId)
                    startActivity(it)
                }
                // 如果是添加关联课程模式，则创建完笔记后退出当前页面
                if (mode == MODE_ADD_RELATED_COURSE) finish()
            }
        }
    }.let { }

    private fun deleteCategory(category: NoteCategory) {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                NoteCategoryDB.get().dao().delete(category)
                withContext(Dispatchers.Main) {
                    adapter.data = adapter.data.filter {
                        it.id != category.id
                    }
                }
            }
            val items = NoteItemDB.get()
                .dao().getByCategoryId(category.id).first()
            for (item in items) {
                launch {
                    for (path in item.picturePaths) {
                        FileManager.deleteInnerImageFromGallery(path)
                    }
                }
            }
        }
    }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: MData()
        old.copy().also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                vm.flow.collect { data ->
                    showList(data.categories, data.keywords)
                    binding.noNoteTips.isGone = data.categories.isNotEmpty()
                }
            }
            launch {
                val categoriesFlow = NoteCategoryDB.get().dao().getAll()
                categoriesFlow.collect { list ->
                    vm.latest?.copy(
                        categories = list
                    )?.let { vm.update(it) }
                }
            }
        }
    }

    private fun showList(origin: List<NoteCategory>, keywords: String) {
        adapter.data = origin.filter {
            it.name.contains(
                keywords,
                ignoreCase = true
            )
        }.sortedBy { -1 * it.timeStamp }
    }

    data class MData(
        val keywords: String = "",
        val categories: List<NoteCategory> = emptyList()
    )

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_COURSE_NAME = "course_name"
        const val MODE_NORMAL = 0
        const val MODE_ADD_RELATED_COURSE = 1
    }
}