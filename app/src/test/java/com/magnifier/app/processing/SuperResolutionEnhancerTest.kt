package com.magnifier.app.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperResolutionEnhancerTest {

    @Test
    fun `computeRoiSize scales down proportionally and clamps to minimum`() {
        val (w1, h1) = SuperResolutionEnhancer.computeRoiSize(4000, 3000, 10f)
        assertEquals(400, w1)
        assertEquals(300, h1)

        val (wExtreme, hExtreme) = SuperResolutionEnhancer.computeRoiSize(4000, 3000, 200f)
        assertTrue(wExtreme >= 32)
        assertTrue(hExtreme >= 32)
    }

    @Test
    fun `sharpenArgbPixels increases local contrast on step edge`() {
        val width = 5
        val height = 5
        val pixels = IntArray(width * height) { idx ->
            val x = idx % width
            if (x < 2) 0xFF404040.toInt() else 0xFFC0C0C0.toInt()
        }

        val sharpened = SuperResolutionEnhancer.sharpenArgbPixels(pixels, width, height, strength = 0.8f)

        val beforeDarkEdge = (pixels[2 * width + 1] ushr 16) and 0xFF
        val afterDarkEdge = (sharpened[2 * width + 1] ushr 16) and 0xFF
        assertTrue(afterDarkEdge <= beforeDarkEdge)

        val beforeBrightEdge = (pixels[2 * width + 2] ushr 16) and 0xFF
        val afterBrightEdge = (sharpened[2 * width + 2] ushr 16) and 0xFF
        assertTrue(afterBrightEdge >= beforeBrightEdge)
    }
}
