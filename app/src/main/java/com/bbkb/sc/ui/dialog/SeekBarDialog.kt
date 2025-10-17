package com.bbkb.sc.ui.dialog

import android.content.DialogInterface
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.bbkb.sc.databinding.DialogSeekBarBinding
import com.poria.base.base.BaseDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SeekBarDialog : BaseDialog<DialogSeekBarBinding>() {
    override fun onViewBindingCreate() = DialogSeekBarBinding.inflate(layoutInflater)
    var title: String? = null
    var minValue = Int.MAX_VALUE
    var maxValue = Int.MAX_VALUE
    var curValue = Int.MIN_VALUE
    var onValueChangedListener: ((Int) -> Unit)? = null
    var onDismissListener: ((Int) -> Unit)? = null

    override fun initView() {
        if (minValue == Int.MAX_VALUE && maxValue == Int.MAX_VALUE &&
            curValue == Int.MIN_VALUE && title == null
        ) {
            dismiss()
            return
        }
        binding.title.text = title
        binding.seekBar.apply {
            max = maxValue
            min = minValue
            curValue = curValue.coerceIn(minValue, maxValue)
            progress = curValue
        }
        binding.value.text = curValue.toString()
    }

    override fun initListener() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                curValue = progress
                lifecycleScope.launch {
                    binding.value.text = progress.toString()
                    onValueChangedListener?.invoke(progress)
                }
            }
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke(curValue)
    }
}