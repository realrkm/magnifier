package com.magnifier.app.processing

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

data class DetectedObjectBox(
    val normalizedRect: RectF,
    val label: String?
)

/**
 * On-device Google ML Kit Object Detection wrapper for locating and classifying
 * specimens within a magnified frame.
 */
object ObjectDetectorHelper {
    private val detector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    fun detectObjects(
        bitmap: Bitmap,
        onResult: (List<DetectedObjectBox>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            onError("Invalid bitmap for object detection")
            return
        }

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val boxes = detectedObjects.map { obj ->
                    val box = obj.boundingBox
                    val normalizedRect = RectF(
                        (box.left / width).coerceIn(0f, 1f),
                        (box.top / height).coerceIn(0f, 1f),
                        (box.right / width).coerceIn(0f, 1f),
                        (box.bottom / height).coerceIn(0f, 1f)
                    )
                    val topLabel = obj.labels
                        .maxByOrNull { it.confidence }
                        ?.text
                        ?.takeIf { it.isNotBlank() }
                    DetectedObjectBox(
                        normalizedRect = normalizedRect,
                        label = topLabel
                    )
                }
                onResult(boxes)
            }
            .addOnFailureListener { e ->
                onError("ML Kit object detection failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }
}
