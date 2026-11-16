package com.magnifier.app.processing

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * On-device super-resolution & edge-preserving clarity enhancer for high-magnification (up to 200x)
 * region-of-interest (ROI) crops.
 */
object SuperResolutionEnhancer {

    /**
     * Computes the centered crop rectangle dimensions for a given [digitalScale] (>= 1f).
     */
    fun computeRoiSize(srcWidth: Int, srcHeight: Int, digitalScale: Float): Pair<Int, Int> {
        val safeScale = digitalScale.coerceAtLeast(1f)
        val roiW = (srcWidth / safeScale).roundToInt().coerceIn(32, srcWidth)
        val roiH = (srcHeight / safeScale).roundToInt().coerceIn(32, srcHeight)
        return roiW to roiH
    }

    /**
     * Extracts the zoomed ROI for [digitalScale], upscales with bilinear/bicubic filtering
     * up to [targetMaxDim], and applies an edge-preserving unsharp mask for crisp detail.
     */
    fun enhanceZoomedRegion(
        source: Bitmap,
        digitalScale: Float,
        targetMaxDim: Int = 2560,
        sharpenStrength: Float = 0.65f
    ): Bitmap {
        val safeScale = digitalScale.coerceAtLeast(1f)
        val (roiW, roiH) = computeRoiSize(source.width, source.height, safeScale)
        val left = ((source.width - roiW) / 2).coerceAtLeast(0)
        val top = ((source.height - roiH) / 2).coerceAtLeast(0)

        val cropped = if (roiW < source.width || roiH < source.height) {
            Bitmap.createBitmap(source, left, top, roiW, roiH)
        } else {
            source
        }

        // Upscale ROI toward targetMaxDim while preserving aspect ratio
        val maxCurrent = max(cropped.width, cropped.height)
        val upscaleFactor = if (safeScale > 1.05f && maxCurrent < targetMaxDim) {
            targetMaxDim.toFloat() / maxCurrent.toFloat()
        } else {
            1f
        }

        val outW = (cropped.width * upscaleFactor).roundToInt().coerceAtLeast(cropped.width)
        val outH = (cropped.height * upscaleFactor).roundToInt().coerceAtLeast(cropped.height)

        val upscaled = if (outW != cropped.width || outH != cropped.height) {
            Bitmap.createScaledBitmap(cropped, outW, outH, true).also {
                if (cropped !== source) cropped.recycle()
            }
        } else {
            cropped
        }

        if (sharpenStrength <= 0.01f) return upscaled

        val pixels = IntArray(outW * outH)
        upscaled.getPixels(pixels, 0, outW, 0, 0, outW, outH)
        val sharpened = sharpenArgbPixels(pixels, outW, outH, sharpenStrength)

        val result = Bitmap.createBitmap(sharpened, outW, outH, Bitmap.Config.ARGB_8888)
        if (upscaled !== source) {
            upscaled.recycle()
        }
        return result
    }

    /**
     * Applies a fast 3x3 unsharp-mask / high-frequency detail boost in ARGB space.
     * Pure Kotlin implementation for deterministic behavior and unit testability.
     */
    fun sharpenArgbPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        strength: Float = 0.65f
    ): IntArray {
        if (width < 3 || height < 3 || strength <= 0f) return pixels.copyOf()
        val out = pixels.copyOf()
        val k = strength.coerceIn(0f, 1.5f)

        for (y in 1 until height - 1) {
            val rowAbove = (y - 1) * width
            val rowCurr = y * width
            val rowBelow = (y + 1) * width

            for (x in 1 until width - 1) {
                val c = pixels[rowCurr + x]
                val a = (c ushr 24) and 0xFF
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF

                val n = pixels[rowAbove + x]
                val s = pixels[rowBelow + x]
                val w = pixels[rowCurr + x - 1]
                val e = pixels[rowCurr + x + 1]

                val avgR = (((n ushr 16) and 0xFF) + ((s ushr 16) and 0xFF) +
                    ((w ushr 16) and 0xFF) + ((e ushr 16) and 0xFF)) shr 2
                val avgG = (((n ushr 8) and 0xFF) + ((s ushr 8) and 0xFF) +
                    ((w ushr 8) and 0xFF) + ((e ushr 8) and 0xFF)) shr 2
                val avgB = ((n and 0xFF) + (s and 0xFF) + (w and 0xFF) + (e and 0xFF)) shr 2

                val newR = min(255, max(0, (r + k * (r - avgR)).roundToInt()))
                val newG = min(255, max(0, (g + k * (g - avgG)).roundToInt()))
                val newB = min(255, max(0, (b + k * (b - avgB)).roundToInt()))

                out[rowCurr + x] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }
        return out
    }
}
