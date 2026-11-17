package com.magnifier.app.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlin.math.min

/**
 * On-device Google ML Kit Subject Segmentation wrapper that isolates the primary
 * foreground specimen by dimming and desaturating background pixels.
 */
object SubjectIsolator {
    private const val FOREGROUND_THRESHOLD = 0.5f
    private const val BACKGROUND_BRIGHTNESS = 0.25f

    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
        SubjectSegmentation.getClient(options)
    }

    fun isolateSubject(
        bitmap: Bitmap,
        onResult: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            onError("Invalid bitmap for subject isolation")
            return
        }

        val workingBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        if (workingBitmap == null) {
            onError("Unable to read bitmap pixels for subject isolation")
            return
        }

        val image = InputImage.fromBitmap(workingBitmap, 0)
        segmenter.process(image)
            .addOnSuccessListener { result ->
                val mask = result.foregroundConfidenceMask
                if (mask == null) {
                    if (workingBitmap !== bitmap) workingBitmap.recycle()
                    onError("Foreground confidence mask unavailable")
                    return@addOnSuccessListener
                }

                try {
                    val width = workingBitmap.width
                    val height = workingBitmap.height
                    val pixelCount = width * height
                    val pixels = IntArray(pixelCount)
                    workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                    mask.rewind()
                    val count = min(pixelCount, mask.remaining())
                    for (i in 0 until count) {
                        val confidence = mask.get()
                        if (confidence < FOREGROUND_THRESHOLD) {
                            val pixel = pixels[i]
                            val a = (pixel ushr 24) and 0xFF
                            val r = (pixel ushr 16) and 0xFF
                            val g = (pixel ushr 8) and 0xFF
                            val b = pixel and 0xFF
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            val dimmed = (luma * BACKGROUND_BRIGHTNESS).toInt().coerceIn(0, 255)
                            pixels[i] = (a shl 24) or (dimmed shl 16) or (dimmed shl 8) or dimmed
                        }
                    }

                    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    output.setPixels(pixels, 0, width, 0, 0, width, height)
                    onResult(output)
                } finally {
                    if (workingBitmap !== bitmap) {
                        workingBitmap.recycle()
                    }
                }
            }
            .addOnFailureListener { e ->
                if (workingBitmap !== bitmap) {
                    workingBitmap.recycle()
                }
                onError("ML Kit subject segmentation failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }
}
