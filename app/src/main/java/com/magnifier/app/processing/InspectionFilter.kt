package com.magnifier.app.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Real-time and snapshot inspection color filters for loupe/microscope use.
 */
enum class InspectionFilter(val label: String) {
    NORMAL("Normal"),
    HIGH_CONTRAST("Contrast"),
    NEGATIVE("Invert"),
    GRAYSCALE("Mono");

    /**
     * 4x5 row-major RGBA color matrix compatible with both Android and Compose ColorMatrix.
     */
    fun toColorMatrixArray(): FloatArray = when (this) {
        NORMAL -> floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        HIGH_CONTRAST -> {
            val scale = 1.65f
            val translate = (-0.5f * scale + 0.5f) * 255f
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        }

        NEGATIVE -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )

        GRAYSCALE -> floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /**
     * Applies this filter to [source] and returns a new [Bitmap] (or [source] if [NORMAL]).
     */
    fun applyToBitmap(source: Bitmap): Bitmap {
        if (this == NORMAL) return source
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(toColorMatrixArray()))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
