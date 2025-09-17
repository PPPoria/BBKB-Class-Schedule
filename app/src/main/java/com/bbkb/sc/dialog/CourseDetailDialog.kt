package com.bbkb.sc.dialog

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.bbkb.sc.R
import com.bbkb.sc.databinding.DialogCourseDetailBinding
import com.bbkb.sc.schedule.data.Course
import com.poria.base.base.BaseDialog

class CourseDetailDialog : BaseDialog<DialogCourseDetailBinding>() {
    override fun onViewBindingCreate() = DialogCourseDetailBinding.inflate(layoutInflater)
    var updateCourse: (Course) -> Unit = {}
    var course: Course? = null

    override fun initWindowInsets(window: Window, gravity: Int, width: Int, height: Int) {
        super.initWindowInsets(
            window,
            Gravity.CENTER,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun initView() = binding.apply {
        val course = course ?: return@apply
        name.text = course.name
        major.text = course.major
        teacher.text = course.teacher
        zc.text = course.zc.toString()
        xq.text = course.xq.toString()
        node.text = course.run { "$startNode-$endNode" }
        classroom.text = course.classroom
        description.text = course.description
        remark.setText(course.remark)
    }.let { }

    override fun onPause() {
        course?.copy().also {
            it!!.remark = binding.remark.text.toString()
            updateCourse(it)
        }
        super.onPause()
    }
}