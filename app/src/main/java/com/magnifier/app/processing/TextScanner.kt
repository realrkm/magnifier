package com.magnifier.app.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Supported on-device ML Kit OCR script models.
 */
enum class OcrScript(val label: String) {
    LATIN("Latin"),
    CHINESE("Chinese"),
    JAPANESE("Japanese"),
    KOREAN("Korean"),
    DEVANAGARI("Devanagari")
}

/**
 * On-device Google ML Kit Text Recognition wrapper for reading microscopic serial numbers,
 * IC chip markings, and fine print across multiple scripts.
 */
object TextScanner {
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val chineseRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    private val japaneseRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    private val koreanRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }
    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    private fun getRecognizer(script: OcrScript): TextRecognizer = when (script) {
        OcrScript.LATIN -> latinRecognizer
        OcrScript.CHINESE -> chineseRecognizer
        OcrScript.JAPANESE -> japaneseRecognizer
        OcrScript.KOREAN -> koreanRecognizer
        OcrScript.DEVANAGARI -> devanagariRecognizer
    }

    fun recognizeText(
        bitmap: Bitmap,
        script: OcrScript = OcrScript.LATIN,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        getRecognizer(script).process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text.trim())
            }
            .addOnFailureListener { e ->
                onError("ML Kit OCR (${script.label}) failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }
}
