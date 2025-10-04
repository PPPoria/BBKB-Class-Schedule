package com.bbkb.sc.dialog

import android.app.ActionBar.LayoutParams
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.databinding.DialogImagePreviewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.poria.base.base.BaseDialog
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import kotlinx.coroutines.flow.MutableStateFlow


class ImagePreviewDialog : BaseDialog<DialogImagePreviewBinding>() {
    override fun onViewBindingCreate() = DialogImagePreviewBinding.inflate(layoutInflater)
    var pathList: List<String> = emptyList()
    var position: Int = 0
    private val flow by lazy {
        MutableStateFlow(
            MData(
                rotation = 0f,
                imagePosition = position,
                imagePathList = pathList
            )
        )
    }

    override fun initWindowInsets(window: Window, gravity: Int, width: Int, height: Int) {
        super.initWindowInsets(
            window,
            Gravity.CENTER,
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
    }

    override fun initView() {

    }

    override fun initListener() = with(binding) {
        preBtn.setOnClickListenerWithClickAnimation {
            flow.value.run {
                val newPosition = (imagePosition - 1) % imagePathList.size
                copy(
                    rotation = 0f,
                    imagePosition = newPosition,
                )
            }.also { flow.value = it }
        }
        nextBtn.setOnClickListenerWithClickAnimation {
            flow.value.run {
                val newPosition = (imagePosition + 1) % imagePathList.size
                copy(
                    rotation = 0f,
                    imagePosition = newPosition,
                )
            }.also { flow.value = it }
        }
    }.let { }

    override suspend fun refreshDataInScope() {
        preDealPathAndPosition()
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { data ->
                Glide.with(this@ImagePreviewDialog)
                    .load(data.imagePathList[data.imagePosition])
                    .transition(DrawableTransitionOptions.withCrossFade(100)) // 淡入淡出
                    .skipMemoryCache(false)          // 允许内存缓存
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.imageView)
                binding.num.text = StringBuilder()
                    .append(data.imagePosition + 1)
                    .append("/")
                    .append(data.imagePathList.size)
                    .toString()
            }
        }
    }

    private fun preDealPathAndPosition() {
        if (pathList.isEmpty()) {
            dismiss()
            return
        }
        position = position.coerceIn(0, pathList.lastIndex)
    }

    data class MData(
        val rotation: Float = 0f,
        val imagePosition: Int = 0, // 用于指定列表的图片
        val imagePathList: List<String> = emptyList(), // 用于直接显示
    )
}