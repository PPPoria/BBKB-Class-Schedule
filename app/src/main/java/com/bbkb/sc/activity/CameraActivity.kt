package com.bbkb.sc.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.bbkb.sc.databinding.ActivityCameraBinding
import com.bbkb.sc.util.SCLog
import com.bbkb.sc.util.SCToast
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraActivity : BaseActivity<ActivityCameraBinding>() {
    override fun onViewBindingCreate() = ActivityCameraBinding.inflate(layoutInflater)
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

    override fun initView() {

    }

    override fun initListener() = with(binding) {
        cameraSwapBtn.setOnClickListenerWithClickAnimation {
            triggerCamera()
        }
    }.let { }

    override suspend fun refreshDataInScope() {
        withContext(Dispatchers.Main) {
            checkPermissions()
        }
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
                    preview
                )
            } catch (exc: Exception) {
                SCLog.error(TAG, exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}