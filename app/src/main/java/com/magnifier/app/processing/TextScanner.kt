package com.magnifier.app.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * On-device Google ML Kit Text Recognition wrapper for reading microscopic serial numbers,
 * IC chip markings, and fine print.
 */
object TextScanner {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun recognizeText(
        bitmap: Bitmap,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                onResult(text)
            }
            .addOnFailureListener { e ->
                onError("ML Kit OCR failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }
}
