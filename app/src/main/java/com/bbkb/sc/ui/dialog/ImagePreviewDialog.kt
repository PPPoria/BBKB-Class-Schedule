package com.bbkb.sc.ui.dialog

import android.app.ActionBar.LayoutParams
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.Gravity
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.databinding.DialogImagePreviewBinding
import com.bbkb.sc.util.FileManager
import com.bbkb.sc.util.SCToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.poria.base.base.BaseDialog
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest


class ImagePreviewDialog : BaseDialog<DialogImagePreviewBinding>() {
    override fun onViewBindingCreate() = DialogImagePreviewBinding.inflate(layoutInflater)
    var pathList: List<String> = emptyList()
    var position: Int = 0
    var changedPathsCallback: ((List<String>) -> Unit)? = null
    private val flow by lazy {
        MutableStateFlow(
            MData(
                rotation = 0f,
                imagePosition = position,
                imagePathList = pathList
            )
        )
    }
    private val rotationMap = HashMap<String, Float>()

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
        imageView.setOnClickListener { dismiss() }
        preBtn.setOnClickListenerWithClickAnimation {
            flow.value.run {
                val newPosition = (imagePosition - 1 + imagePathList.size) % imagePathList.size
                copy(
                    rotation = rotationMap[imagePathList[newPosition]] ?: 0f,
                    imagePosition = newPosition,
                )
            }.also { flow.value = it }
        }
        nextBtn.setOnClickListenerWithClickAnimation {
            flow.value.run {
                val newPosition = (imagePosition + 1) % imagePathList.size
                copy(
                    rotation = rotationMap[imagePathList[newPosition]] ?: 0f,
                    imagePosition = newPosition,
                )
            }.also { flow.value = it }
        }
        downloadBtn.setOnClickListenerWithClickAnimation {
            lifecycleScope.launch {
                val path = flow.value.run {
                    imagePathList[imagePosition]
                }
                val uri = FileManager.saveInnerImageToGallery(path)
                if (uri != null)
                    SCToast.show("图片已保存到相册${uri.path}")
                else SCToast.show("保存失败，请稍后再试")
            }
        }
        rotateBtn.setOnClickListenerWithClickAnimation {
            flow.value.run {
                copy(
                    rotation =
                        if (rotation == 270f) 0f
                        else rotation + 90f,
                )
            }.also { flow.value = it }
        }
        deleteBtn.setOnClickListenerWithClickAnimation {
            lifecycleScope.launch {
                val deletePosition = flow.value.imagePosition
                val path = flow.value.imagePathList[deletePosition]
                val result = FileManager.deleteInnerImage(path)
                if (result) {
                    val newList = flow.value.imagePathList.toMutableList()
                    newList.removeAt(deletePosition)
                    flow.value.copy(
                        imagePosition =
                            if (newList.isNotEmpty())
                                deletePosition.coerceIn(0, newList.lastIndex)
                            else -1,
                        imagePathList = newList
                    ).also { flow.value = it }
                } else SCToast.show("删除失败，请稍后再试")
            }
        }
    }.let { }

    override suspend fun refreshDataInScope() {
        preDealPathAndPosition()
    }

    private fun preDealPathAndPosition() {
        if (pathList.isEmpty()) {
            dismiss()
            return
        }
        position = position.coerceIn(0, pathList.lastIndex)
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { data ->
                if (data.imagePathList.isEmpty()) {
                    dismiss()
                    return@collect
                }
                rotationMap[data.imagePathList[data.imagePosition]] = data.rotation
                Glide.with(this@ImagePreviewDialog)
                    .load(data.imagePathList[data.imagePosition])
                    .transition(DrawableTransitionOptions.withCrossFade(100)) // 淡入淡出
                    .skipMemoryCache(false)          // 允许内存缓存
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .let {
                        if (data.rotation != 0f) {
                            it.transform(RotateTransformation(data.rotation))
                        } else it
                    }
                    .into(binding.imageView)
                binding.num.text = StringBuilder()
                    .append(data.imagePosition + 1)
                    .append("/")
                    .append(data.imagePathList.size)
                    .toString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        changedPathsCallback?.invoke(
            flow.value.imagePathList
        )
    }

    data class MData(
        val rotation: Float = 0f,
        val imagePosition: Int = 0, // 用于指定列表的图片
        val imagePathList: List<String> = emptyList(), // 用于直接显示
    )

    class RotateTransformation(private val angle: Float) : BitmapTransformation() {

        override fun transform(pool: BitmapPool, src: Bitmap, width: Int, height: Int): Bitmap {
            val matrix = Matrix().apply { postRotate(angle) }
            return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }

        override fun equals(other: Any?) = other is RotateTransformation && angle == other.angle
        override fun hashCode() = angle.hashCode()
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(angle.toString().toByteArray())
        }
    }
}