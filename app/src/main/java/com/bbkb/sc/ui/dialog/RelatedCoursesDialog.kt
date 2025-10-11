package com.bbkb.sc.ui.dialog

import android.content.Intent
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.DialogRelatedCoursesBinding
import com.bbkb.sc.databinding.ItemRelatedNoteBinding
import com.bbkb.sc.util.ScheduleUtils
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.CourseDB
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.bbkb.sc.ui.activity.TableActivity
import com.bbkb.sc.util.SCToast
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


class RelatedCoursesDialog : BaseDialog<DialogRelatedCoursesBinding>() {
    override fun onViewBindingCreate() = DialogRelatedCoursesBinding.inflate(layoutInflater)
    var categoryId: Long? = null
    private var category: NoteCategory? = null
    private val adapter by lazy {
        SingleBindingAdapter<ItemRelatedNoteBinding, RelatedItemData>(
            itemLayoutId = R.layout.item_related_note,
            vbBind = ItemRelatedNoteBinding::bind
        ) { binding, _, item, _ ->
            val name = item.courseName
            binding.name.text = name
            binding.itemContainer.setOnClickListener {
                lifecycleScope.launch {
                    val courseList = withContext(Dispatchers.IO) {
                        CourseDB.get().dao().getByName(name).first()
                    }.ifEmpty {
                        SCToast.show("找不到课程: $name")
                        removeRelatedCourse(name)
                        return@launch
                    }
                    val curZc = ScheduleUtils.getZC(System.currentTimeMillis())
                    val course = courseList.let { list ->
                        var course: Course = list.first()
                        for (c in list) {
                            if (abs(c.zc - curZc) < abs(course.zc - curZc))
                                course = c
                        }
                        course
                    }
                    CourseDetailDialog().also {
                        it.course = course
                        it.show(requireActivity().supportFragmentManager, "course_detail_dialog")
                    }
                }
            }
            binding.itemContainer.setOnLongClickListener {
                val popup = PopupMenu(binding.root.context, binding.root)
                popup.menuInflater.inflate(com.poria.base.R.menu.menu_note_popup, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        com.poria.base.R.id.action_delete -> ConfirmDialog().also {
                            it.title = "确认移除与\"${name}\"的关联?"
                            it.content = getString(R.string.remove_related_course)
                            it.confirmBgColor = SCApp.app.getColor(R.color.tertiary)
                            it.confirmTextColor = SCApp.app.getColor(R.color.white)
                            it.onConfirm = { removeRelatedCourse(name) }
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

    override fun initView() = with(binding) {
        if (categoryId == null) {
            dismiss()
            return@initView
        }
        relatedCourseList.let {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.adapter = adapter
        }
    }.let { }

    override fun initListener() = with(binding) {
        addRelatedCourseBtn.setOnClickListener {
            Intent(
                requireActivity(),
                TableActivity::class.java
            ).also {
                it.putExtra(
                    TableActivity.KEY_MODE,
                    TableActivity.MODE_ADD_RELATED_NOTE
                )
                it.putExtra(
                    TableActivity.KEY_NOTE_CATEGORY_ID,
                    categoryId ?: return@also
                )
                startActivity(it)
            }
        }
    }

    private fun removeRelatedCourse(name: String) {
        val courseNames = category?.courseNames?.toMutableList() ?: return
        courseNames.removeIf { it == name }
        category!!.courseNames = courseNames
        lifecycleScope.launch {
            adapter.data = courseNames.map { RelatedItemData(it) }
        }
        CoroutineScope(Dispatchers.IO).launch {
            NoteCategoryDB.get().dao().update(category!!)
        }
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.IO).launch {
            category = NoteCategoryDB.get().dao().getById(categoryId!!).first()
            val related = category?.courseNames ?: return@launch
            adapter.data = related.map { RelatedItemData(it) }
        }
    }

    data class RelatedItemData(
        var courseName: String,
    )
}