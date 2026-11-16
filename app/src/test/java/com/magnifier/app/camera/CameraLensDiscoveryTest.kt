package com.magnifier.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraLensDiscoveryTest {

    @Test
    fun `classifies lenses by focal length and minimum focus distance`() {
        assertEquals("Macro", CameraLensDiscovery.classifyLensLabel(focalLengthMm = 3.2f, minFocusDiopters = 20f))
        assertEquals("0.6x Wide", CameraLensDiscovery.classifyLensLabel(focalLengthMm = 1.8f, minFocusDiopters = 5f))
        assertEquals("Tele", CameraLensDiscovery.classifyLensLabel(focalLengthMm = 12.0f, minFocusDiopters = 2f))
        assertEquals("Main", CameraLensDiscovery.classifyLensLabel(focalLengthMm = 4.7f, minFocusDiopters = 10f))
    }

    @Test
    fun `minFocusDistanceCm converts diopters to centimeters`() {
        val macro = CameraLensInfo(
            cameraId = "2",
            label = "Macro",
            focalLengthMm = 3.2f,
            minFocusDistanceDiopters = 25f,
            supportsManualFocus = true,
            hasFlash = false,
            maxTorchStrengthLevel = 1
        )
        assertEquals(4.0f, macro.minFocusDistanceCm!!, 0.01f)

        val fixed = macro.copy(minFocusDistanceDiopters = 0f)
        assertNull(fixed.minFocusDistanceCm)
    }
}
