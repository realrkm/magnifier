package com.magnifier.app.processing

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.Locale
import kotlin.math.max

data class SmartPresetSuggestion(
    val detectedLabel: String,
    val confidence: Float,
    val suggestedFilter: InspectionFilter,
    val suggestTorch: Boolean
)

/**
 * On-device Google ML Kit Image Labeling wrapper that suggests an [InspectionFilter]
 * and torch state for the current magnified scene.
 */
object SmartPresetLabeler {
    private val negativeKeywords = listOf(
        "electronic", "electronics", "circuit", "pcb", "chip", "microchip",
        "motherboard", "computer", "hardware", "component", "resistor",
        "capacitor", "metal", "coin", "currency", "money", "silver",
        "copper", "gold", "steel", "wire", "solder", "watch", "clock", "gear"
    )

    private val highContrastKeywords = listOf(
        "plant", "leaf", "leaves", "foliage", "flower", "petal", "botany",
        "insect", "bug", "beetle", "ant", "butterfly", "arachnid", "spider",
        "textile", "fabric", "cloth", "knit", "weave", "wool", "cotton",
        "paper", "document", "text", "print", "page", "book", "newspaper",
        "skin", "hair", "wood", "bark", "feather"
    )

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    fun analyzeScene(
        bitmap: Bitmap,
        onResult: (SmartPresetSuggestion?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            onError("Invalid bitmap for scene labeling")
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                val topLabel = labels.maxByOrNull { it.confidence }
                if (topLabel == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val suggestedFilter = mapLabelToFilter(topLabel.text)
                val suggestTorch = isLowLightScene(bitmap)
                onResult(
                    SmartPresetSuggestion(
                        detectedLabel = topLabel.text,
                        confidence = topLabel.confidence,
                        suggestedFilter = suggestedFilter,
                        suggestTorch = suggestTorch
                    )
                )
            }
            .addOnFailureListener { e ->
                onError("ML Kit image labeling failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }

    fun mapLabelToFilter(label: String): InspectionFilter {
        val normalized = label.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return InspectionFilter.NORMAL

        return when {
            negativeKeywords.any { normalized.contains(it) } -> InspectionFilter.NEGATIVE
            highContrastKeywords.any { normalized.contains(it) } -> InspectionFilter.HIGH_CONTRAST
            else -> InspectionFilter.NORMAL
        }
    }

    private fun isLowLightScene(bitmap: Bitmap): Boolean {
        val readableBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
        } else {
            bitmap
        }

        return try {
            val width = readableBitmap.width
            val height = readableBitmap.height
            val stepX = max(1, width / 16)
            val stepY = max(1, height / 16)

            var lumaSum = 0f
            var samples = 0
            var y = 0
            while (y < height) {
                var x = 0
                while (x < width) {
                    val pixel = readableBitmap.getPixel(x, y)
                    val luma = 0.299f * Color.red(pixel) +
                        0.587f * Color.green(pixel) +
                        0.114f * Color.blue(pixel)
                    lumaSum += luma
                    samples++
                    x += stepX
                }
                y += stepY
            }

            samples > 0 && (lumaSum / samples) < 85f
        } finally {
            if (readableBitmap !== bitmap) {
                readableBitmap.recycle()
            }
        }
    }
}
