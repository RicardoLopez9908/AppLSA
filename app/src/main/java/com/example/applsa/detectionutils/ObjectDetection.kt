package com.example.applsa.detectionutils

import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

object ObjectDetection {

    fun getCustomDetectorOption(path: String): CustomObjectDetectorOptions {
        val localModel = LocalModel.Builder()
            .setAssetFilePath(path)
            .build()

        // Live detection and tracking
        return CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()
    }

}