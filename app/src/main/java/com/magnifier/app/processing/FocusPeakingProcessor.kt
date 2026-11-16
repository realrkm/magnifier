package com.magnifier.app.processing

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs

/**
 * Computes a lightweight Sobel edge-magnitude map on a luma plane for real-time focus peaking.
 */
object FocusPeakingProcessor {
    private const val PEAK_COLOR: Int = 0xD900E5FF.toInt() // Translucent cyan highlight
    private const val TRANSPARENT: Int = 0x00000000

    /**
     * Detects sharp edges in an 8-bit [luma] buffer of size [width] x [height].
     * Pixels whose Sobel gradient exceeds [threshold] are colored with [PEAK_COLOR].
     */
    fun computePeakingPixels(
        luma: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width,
        threshold: Int = 90
    ): IntArray {
        val out = IntArray(width * height)
        if (width < 3 || height < 3) return out

        for (y in 1 until height - 1) {
            val rowAbove = (y - 1) * rowStride
            val rowCurr = y * rowStride
            val rowBelow = (y + 1) * rowStride
            val outRow = y * width

            for (x in 1 until width - 1) {
                val tl = luma[rowAbove + x - 1].toInt() and 0xFF
                val tc = luma[rowAbove + x].toInt() and 0xFF
                val tr = luma[rowAbove + x + 1].toInt() and 0xFF
                val ml = luma[rowCurr + x - 1].toInt() and 0xFF
                val mr = luma[rowCurr + x + 1].toInt() and 0xFF
                val bl = luma[rowBelow + x - 1].toInt() and 0xFF
                val bc = luma[rowBelow + x].toInt() and 0xFF
                val br = luma[rowBelow + x + 1].toInt() and 0xFF

                val gx = -tl + tr - 2 * ml + 2 * mr - bl + br
                val gy = -tl - 2 * tc - tr + bl + 2 * bc + br
                val magnitude = abs(gx) + abs(gy)

                out[outRow + x] = if (magnitude >= threshold) PEAK_COLOR else TRANSPARENT
            }
        }
        return out
    }
}

/**
 * CameraX [ImageAnalysis.Analyzer] that emits rotated focus-peaking overlay bitmaps
 * while [isEnabled] returns true.
 */
class FocusPeakingAnalyzer(
    private val isEnabled: () -> Boolean,
    private val onPeakingBitmap: (Bitmap?) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastFrameMs = 0L

    override fun analyze(image: ImageProxy) {
        try {
            if (!isEnabled()) {
                return
            }
            val now = System.currentTimeMillis()
            // Cap analysis to ~20 fps to keep CPU/battery cool during long inspections
            if (now - lastFrameMs < 50L) return
            lastFrameMs = now

            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val pixels = FocusPeakingProcessor.computePeakingPixels(
                luma = data,
                width = image.width,
                height = image.height,
                rowStride = plane.rowStride,
                threshold = 110
            )

            val rawBitmap = Bitmap.createBitmap(
                pixels,
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )

            val rotation = image.imageInfo.rotationDegrees
            val finalBitmap = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, false)
                    .also { rawBitmap.recycle() }
            } else {
                rawBitmap
            }

            onPeakingBitmap(finalBitmap)
        } finally {
            image.close()
        }
    }
}
