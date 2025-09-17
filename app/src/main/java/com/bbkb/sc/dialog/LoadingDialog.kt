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
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.DialogLoadingBinding
import com.poria.base.base.BaseDialog

class LoadingDialog : BaseDialog<DialogLoadingBinding>() {
    var title: String = SCApp.app.getString(R.string.please_wait)

    override fun onViewBindingCreate() = DialogLoadingBinding.inflate(layoutInflater)

    override fun initWindowInsets(window: Window, gravity: Int, width: Int, height: Int) {
        super.initWindowInsets(
            window,
            Gravity.CENTER,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun initView() {
        isCancelable = false
        binding.title.text = title
    }

}