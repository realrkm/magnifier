package com.magnifier.app.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FocusPeakingProcessorTest {

    @Test
    fun `flat luma region produces transparent pixels`() {
        val width = 10
        val height = 10
        val flatLuma = ByteArray(width * height) { 128.toByte() }

        val pixels = FocusPeakingProcessor.computePeakingPixels(
            luma = flatLuma,
            width = width,
            height = height,
            threshold = 90
        )

        assertEquals(width * height, pixels.size)
        assertEquals(0, pixels.count { it != 0 })
    }

    @Test
    fun `sharp step edge triggers focus peaking highlight`() {
        val width = 10
        val height = 10
        val luma = ByteArray(width * height) { index ->
            val x = index % width
            if (x < 5) 0 else 255.toByte()
        }

        val pixels = FocusPeakingProcessor.computePeakingPixels(
            luma = luma,
            width = width,
            height = height,
            threshold = 90
        )

        // Pixels along the x=4/5 boundary (for y in 1..height-2) should be highlighted
        assertNotEquals(0, pixels[5 * width + 4])
        assertNotEquals(0, pixels[5 * width + 5])
        // Far from edge should remain transparent
        assertEquals(0, pixels[5 * width + 1])
        assertEquals(0, pixels[5 * width + 8])
    }
}
