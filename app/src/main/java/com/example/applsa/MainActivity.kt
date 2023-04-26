package com.example.applsa

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.applsa.databinding.ActivityMainBinding
import com.example.applsa.detectionutils.CustomAnalyzer
import com.example.applsa.detectionutils.ObjectDetection
import com.example.applsa.detectionutils.ObjectDetectorProcessor
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    private var cameraProvider: ProcessCameraProvider? = null

    private var cameraSelector: CameraSelector? = null

    private var previewUseCase: Preview? = null

    private var analysisUseCase: ImageAnalysis? = null

    private var needUpdateGraphicOverlayImageSourceInfo = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            )[CameraXViewModel::class.java]
                .processCameraProvider
                .observe(this) { provider: ProcessCameraProvider? ->
                    cameraProvider = provider
                    bindUseCases()
                }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun bindUseCases() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        bindPreviewUseCase()
        bindAnalysisUseCase()
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }
        previewUseCase = Preview.Builder().build()

        previewUseCase?.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
        cameraProvider?.bindToLifecycle(this, cameraSelector!!, previewUseCase)
    }
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun bindAnalysisUseCase() {
        analysisUseCase = ImageAnalysis.Builder()
            .setTargetResolution(Size(640,480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        needUpdateGraphicOverlayImageSourceInfo = true

        val imageProcessor =  ObjectDetectorProcessor(this,
            ObjectDetection.getCustomDetectorOption("custom_models/object_labeler.tflite")
        )

        needUpdateGraphicOverlayImageSourceInfo = true

        val analyzer = ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {
                val isImageFlipped = false
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    viewBinding.graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                } else {
                    viewBinding.graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }
            try {
                imageProcessor.processImageProxy(imageProxy, viewBinding.graphicOverlay)
            } catch (e: MlKitException) {
                Log.e("Log", "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }

        analysisUseCase?.setAnalyzer(ContextCompat.getMainExecutor(this),analyzer)
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                CAMERA,
                RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}