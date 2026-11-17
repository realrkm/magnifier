package com.magnifier.app.processing

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPresetLabelerTest {

    @Test
    fun `maps electronics and metallic subjects to negative filter`() {
        assertEquals(InspectionFilter.NEGATIVE, SmartPresetLabeler.mapLabelToFilter("Electronic component"))
        assertEquals(InspectionFilter.NEGATIVE, SmartPresetLabeler.mapLabelToFilter("Circuit board"))
        assertEquals(InspectionFilter.NEGATIVE, SmartPresetLabeler.mapLabelToFilter("Metal coin"))
    }

    @Test
    fun `maps organic and textile subjects to high contrast filter`() {
        assertEquals(InspectionFilter.HIGH_CONTRAST, SmartPresetLabeler.mapLabelToFilter("Plant leaf"))
        assertEquals(InspectionFilter.HIGH_CONTRAST, SmartPresetLabeler.mapLabelToFilter("Insect"))
        assertEquals(InspectionFilter.HIGH_CONTRAST, SmartPresetLabeler.mapLabelToFilter("Woven textile"))
    }

    @Test
    fun `maps unknown subjects to normal filter`() {
        assertEquals(InspectionFilter.NORMAL, SmartPresetLabeler.mapLabelToFilter("Sky"))
    }
}
