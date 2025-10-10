package com.bbkb.sc.ui.dialog

import android.app.ActionBar.LayoutParams
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.Window
import androidx.core.view.isGone
import com.bbkb.sc.databinding.DialogConfirmBinding
import com.poria.base.base.BaseDialog

class ConfirmDialog : BaseDialog<DialogConfirmBinding>() {
    override fun onViewBindingCreate() = DialogConfirmBinding.inflate(layoutInflater)
    var title: String? = null
    var content: String? = null
    var confirm: String? = null
    var cancel: String? = null
    var confirmBgColor: Int? = null
    var cancelBgColor: Int? = null
    var confirmTextColor: Int? = null
    var cancelTextColor: Int? = null
    var onConfirm: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    override fun initWindowInsets(window: Window, gravity: Int, width: Int, height: Int) {
        super.initWindowInsets(
            window,
            Gravity.CENTER,
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    override fun initView() = with(binding) {
        if (title == null && content == null) {
            dismiss()
            return@with
        }
        tvTitle.isGone = title?.let {
            tvTitle.text = it
            false
        } ?: true
        tvContent.isGone = content?.let {
            tvContent.text = it
            false
        } ?: true
        confirm?.let { confirmBtn.text = it }
        cancel?.let { cancelBtn.text = it }
        confirmBgColor?.let { confirmBtn.backgroundTintList = ColorStateList.valueOf(it) }
        cancelBgColor?.let { cancelBtn.backgroundTintList = ColorStateList.valueOf(it) }
        confirmTextColor?.let { confirmBtn.setTextColor(it) }
        cancelTextColor?.let { cancelBtn.setTextColor(it) }
    }.let { }

    override fun initListener() = with(binding) {
        confirmBtn.setOnClickListener {
            onConfirm?.invoke()
            dismiss()
        }
        cancelBtn.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }
    }.let { }

}