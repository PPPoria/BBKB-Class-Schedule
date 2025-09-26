package com.bbkb.sc.dialog

import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import com.bbkb.sc.databinding.DialogCourseDetailBinding
import com.bbkb.sc.schedule.ScheduleUtils
import com.bbkb.sc.schedule.database.Course
import com.bbkb.sc.schedule.database.Remark
import com.bbkb.sc.schedule.database.RemarkDB
import com.poria.base.base.BaseDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CourseDetailDialog : BaseDialog<DialogCourseDetailBinding>() {
    override fun onViewBindingCreate() = DialogCourseDetailBinding.inflate(layoutInflater)
    lateinit var course: Course
    private lateinit var remark: Remark

    override fun initWindowInsets(window: Window, gravity: Int, width: Int, height: Int) {
        super.initWindowInsets(
            window,
            Gravity.CENTER,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun initView() = binding.apply {
        name.text = course.name
        major.text = course.major
        teacher.text = course.teacher
        zc.text = course.zc.toString()
        xq.text = course.xq.toString()
        node.text = course.run { "$startNode-$endNode" }
        classroom.text = course.classroom
        description.text = course.description
        CoroutineScope(Dispatchers.IO).launch {
            this@CourseDetailDialog.remark =
                ScheduleUtils.getRemarkByCourseName(course.name)
            withContext(Dispatchers.Main) {
                remark.setText(this@CourseDetailDialog.remark.content)
            }
        }
    }.let { }

    override fun onPause() {
        remark.content = binding.remark.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            remark.copy().also {
                RemarkDB.get().dao().update(it)
            }
        }
        super.onPause()
    }
}