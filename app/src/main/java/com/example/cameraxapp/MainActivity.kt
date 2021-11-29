package com.example.cameraxapp

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {


    lateinit var fileDire: File
    lateinit var executorService: ExecutorService
    var capture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val random = Random()
        textView.setBackgroundColor(Color.rgb(random.nextInt(),random.nextInt(),random.nextInt()))

        capture = ImageCapture.Builder()
//            .setFlashMode(ImageCapture.FLASH_MODE_ON)
            .build()

        if (checkPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSION, 1)
        }

        takePhoto.setOnClickListener { takePhoto() }

        fileDire = getFileDir()

        executorService = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderListenableFuture.addListener(Runnable {
            val cameraProvider = cameraProviderListenableFuture.get()

            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(preview.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, capture)
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = capture ?: return

        val photoFile = File(fileDire, "${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}.jpg")

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                showToast("Captured")
//                showToast("${Uri.fromFile(photoFile)}")
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                showToast("Failed")
            }
        })
    }

    private fun checkPermission() = PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getFileDir() : File{
        val dir = externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraXPhoto").apply { mkdir() }
        }
        return if (dir != null && dir.exists()) dir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (checkPermission()) {
                startCamera()
            } else {
                showToast("Permission Not Granted...")
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val PERMISSION = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    fun showToast(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
}