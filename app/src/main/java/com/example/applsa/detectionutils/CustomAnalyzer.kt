package com.example.applsa.detectionutils

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector

class CustomAnalyzer(private val objectDetector: ObjectDetector, private val successListener: (
    Task<List<DetectedObject>>)-> Unit) : ImageAnalysis.Analyzer {

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            objectDetector.process(image)
                .addOnFailureListener {
                    imageProxy.close()
                }
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        successListener(it)
                    } else {
                        val exception = it.exception
                        exception?.printStackTrace()
                    }
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}