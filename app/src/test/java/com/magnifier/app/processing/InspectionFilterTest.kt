package com.magnifier.app.processing

import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionFilterTest {

    @Test
    fun `all filters return 20-element color matrix`() {
        InspectionFilter.entries.forEach { filter ->
            assertEquals(20, filter.toColorMatrixArray().size)
        }
    }

    @Test
    fun `negative filter inverts RGB channels and preserves alpha`() {
        val matrix = InspectionFilter.NEGATIVE.toColorMatrixArray()
        assertEquals(-1f, matrix[0], 0.001f)
        assertEquals(255f, matrix[4], 0.001f)
        assertEquals(-1f, matrix[6], 0.001f)
        assertEquals(255f, matrix[9], 0.001f)
        assertEquals(-1f, matrix[12], 0.001f)
        assertEquals(255f, matrix[14], 0.001f)
        assertEquals(1f, matrix[18], 0.001f)
    }
}
