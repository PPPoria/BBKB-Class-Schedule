package com.bbkb.sc.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bbkb.sc.SCApp
import com.bbkb.sc.databinding.ActivityCameraBinding
import com.bbkb.sc.util.SCLog
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "CameraActivity"

class CameraActivity : BaseActivity<ActivityCameraBinding>() {
    override fun onViewBindingCreate() = ActivityCameraBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private var canRequestAgain = true
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        callback = { isGranted ->
            if (isGranted) {
                triggerCamera()
            } else if (
                canRequestAgain &&
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            ) {
                canRequestAgain = false
                checkPermissions()
            } else {
                SCToast.show("请授予相机权限")
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                ).run {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(this)
                }
            }
        }
    )
    private val cameraProviderFuture by lazy { ProcessCameraProvider.getInstance(this) }
    private val cameraProvider by lazy { cameraProviderFuture.get() }
    private var cameraSelector: CameraSelector? = null
    private val preview by lazy {
        Preview.Builder().build().apply {
            surfaceProvider = binding.previewView.surfaceProvider
        }
    }
    private val imageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    override fun initWindowInsets(l: Int, t: Int, r: Int, b: Int) {
        super.initWindowInsets(l, t, r, b)
        setImmersiveLayout(true)
        binding.cameraControlsLayout.setPadding(
            0,
            systemBarPadding[t],
            0,
            systemBarPadding[b]
        )
    }

    override fun initListener() = with(binding) {
        captureBtn.setOnClickListener {
            val now = vm.latest ?: return@setOnClickListener
            if (now.bitmap != null) {
                saveAndFinish(now.bitmap)
            } else {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(this@CameraActivity),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            CoroutineScope(Dispatchers.Default).launch {
                                val jpeg = image.planes[0].buffer   // 完整 JPEG 字节
                                val jpegBytes = jpeg.let { // 转为字节数组
                                    ByteArray(it.remaining()).also { arr -> it.get(arr) }
                                }
                                val src = BitmapFactory.decodeByteArray(
                                    jpegBytes,
                                    0,
                                    jpegBytes.size
                                )
                                val corrected = src.cropToScreenRatio()
                                image.close()
                                vm.latest?.copy(
                                    bitmap = corrected
                                )?.also { vm.update(it) }
                            }
                        }
                    }
                )
            }
        }
        swapBtn.setOnClickListenerWithClickAnimation { triggerCamera() }
        val cancelFun = {
            vm.latest?.run {
                if (bitmap != null) {
                    copy(
                        bitmap = null
                    ).also { vm.update(it) }
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }
        cancelBtn.setOnClickListenerWithClickAnimation { cancelFun() }
        onBackPressedDispatcher.addCallback { cancelFun() }
    }.let { }

    private fun saveAndFinish(finalBitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(filesDir, "captured_frames/${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK, Intent().apply { putExtra("path", file.absolutePath) })
                finish()
            }
        }
    }

    /*private fun Bitmap.rotate90(): Bitmap {
        val src = this
        val matrix = Matrix().apply { postRotate(90f) }
        val corrected = Bitmap.createBitmap(
            src, 0, 0, src.width, src.height, matrix, false
        )
        return corrected.cropToScreenRatio()
    }*/

    //居中裁剪成屏幕比例（高:宽 = 屏幕高:屏幕宽）
    private fun Bitmap.cropToScreenRatio(context: Context = SCApp.app): Bitmap {
        val windowMetrics = context.getSystemService(WindowManager::class.java)
            .currentWindowMetrics
        val bounds = windowMetrics.bounds // 可视矩形
        val screenRatio = bounds.height().toFloat() / bounds.width().toFloat()
        val bitmapRatio = height.toFloat() / width.toFloat()

        return if (bitmapRatio > screenRatio) {
            // 图片太高，裁掉上下
            val cropHeight = (width * screenRatio).toInt()
            val top = (height - cropHeight) / 2
            Bitmap.createBitmap(this, 0, top, width, cropHeight)
        } else {
            // 图片太宽，裁掉左右
            val cropWidth = (height / screenRatio).toInt()
            val left = (width - cropWidth) / 2
            Bitmap.createBitmap(this, left, 0, cropWidth, height)
        }
    }

    override suspend fun refreshDataInScope() {
        withContext(Dispatchers.Main) {
            checkPermissions()
        }
        vm.update(MData())
    }

    private fun checkPermissions() {
        // 短一点好看
        val equalFun = { a: Any?, b: Any? -> a == b }
        val isGranted = equalFun(
            checkSelfPermission(Manifest.permission.CAMERA),
            PackageManager.PERMISSION_GRANTED
        )
        if (isGranted) triggerCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.flow.collect { data ->
                if (data.bitmap != null) {
                    binding.frameImage.setImageBitmap(data.bitmap)
                    binding.frameImage.isGone = false
                    binding.icCheckmark.isGone = false
                    binding.swapBtn.isGone = true
                } else {
                    binding.frameImage.isGone = true
                    binding.icCheckmark.isGone = true
                    binding.swapBtn.isGone = false
                }
            }
        }
    }

    private fun triggerCamera() {
        cameraProviderFuture.addListener({
            cameraSelector = when (cameraSelector) {
                CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector!!,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                SCLog.error(TAG, exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    data class MData(
        val bitmap: Bitmap? = null
    )
}