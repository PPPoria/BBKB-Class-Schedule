package com.bbkb.sc.ui.dialog

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.DialogCourseDetailBinding
import com.bbkb.sc.databinding.ItemRelatedNoteBinding
import com.bbkb.sc.schedule.ScheduleUtils
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.schedule.database.Remark
import com.bbkb.sc.schedule.database.RemarkDB
import com.bbkb.sc.ui.activity.NoteCategoryListActivity
import com.bbkb.sc.ui.activity.NoteItemListActivity
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CourseDetailDialog : BaseDialog<DialogCourseDetailBinding>() {
    override fun onViewBindingCreate() = DialogCourseDetailBinding.inflate(layoutInflater)
    var course: Course? = null
    private var remarkData: Remark? = null
    private val adapter by lazy {
        SingleBindingAdapter<ItemRelatedNoteBinding, RelatedItemData>(
            itemLayoutId = R.layout.item_related_note,
            vbBind = ItemRelatedNoteBinding::bind
        ) { binding, _, item, _ ->
            val category = item.category
            binding.name.text = category.name
            binding.itemContainer.setOnClickListener {
                Intent(
                    requireActivity(),
                    NoteItemListActivity::class.java
                ).also {
                    it.putExtra(NoteItemListActivity.KEY_CATEGORY_ID, category.id)
                    startActivity(it)
                }
            }
            binding.itemContainer.setOnLongClickListener {
                val popup = PopupMenu(binding.root.context, binding.root)
                popup.menuInflater.inflate(com.poria.base.R.menu.menu_note_popup, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        com.poria.base.R.id.action_delete -> ConfirmDialog().also {
                            it.title = "确认移除与\"${category.name}\"的关联?"
                            it.content = getString(R.string.remove_related_note)
                            it.confirmBgColor = SCApp.app.getColor(R.color.tertiary)
                            it.confirmTextColor = SCApp.app.getColor(R.color.white)
                            it.onConfirm = { removeRelatedNote(category) }
                            it.show(
                                requireActivity().supportFragmentManager,
                                "remove_related_dialog"
                            )
                        }
                    }
                    true
                }
                popup.show()
                true
            }
        }
    }

    override fun initWindowInsets(window: Window, gravity: Int, width: Int, height: Int) {
        super.initWindowInsets(
            window,
            Gravity.CENTER,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun initView() = binding.apply {
        val course = course ?: run {
            dismiss()
            return
        }
        name.text = course.name
        major.text = course.major
        teacher.text = course.teacher
        zc.text = course.zc.toString()
        xq.text = course.xq.toString()
        node.text = course.run { "$startNode-$endNode" }
        classroom.text = course.classroom
        description.text = course.description
        relatedNoteList.let {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.adapter = adapter
        }
        CoroutineScope(Dispatchers.IO).launch {
            remarkData =
                ScheduleUtils.getRemarkByCourseName(course.name)
            withContext(Dispatchers.Main) {
                remark.setText(remarkData!!.content)
            }
        }
    }.let { }

    override fun initListener() = with(binding) {
        remark.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                CoroutineScope(Dispatchers.IO).launch {
                    RemarkDB.get().dao().update(
                        remarkData?.also {
                            it.content = et.toString()
                        } ?: return@launch
                    )
                }
            }
        })
        addRelatedNoteBtn.setOnClickListener {
            Intent(
                requireActivity(),
                NoteCategoryListActivity::class.java
            ).also {
                it.putExtra(
                    NoteCategoryListActivity.KEY_MODE,
                    NoteCategoryListActivity.MODE_ADD_RELATED_COURSE
                )
                it.putExtra(
                    NoteCategoryListActivity.KEY_COURSE_NAME,
                    course?.name ?: return@setOnClickListener
                )
                startActivity(it)
            }
        }
    }.let { }

    private fun removeRelatedNote(category: NoteCategory) {
        lifecycleScope.launch {
            adapter.data = adapter.data
                .filter { it.category.id != category.id }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val removedName = course?.name ?: return@launch
            val newCourseNames = category.courseNames.toMutableList()
            newCourseNames.removeIf { it == removedName }
            NoteCategoryDB.get().dao().update(category.copy(courseNames = newCourseNames))
        }
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.IO).launch {
            val related = NoteCategoryDB.get().dao().getAll().first().filter {
                it.courseNames.contains(course?.name ?: return@launch)
            }.map { RelatedItemData(it) }
            withContext(Dispatchers.Main) { adapter.data = related }
        }
    }

    data class RelatedItemData(
        var category: NoteCategory,
    )
}