package com.magnifier.app.camera

/**
 * Describes a discoverable rear camera lens and its optical/illumination capabilities.
 */
data class CameraLensInfo(
    val cameraId: String,
    val label: String,
    val focalLengthMm: Float,
    val minFocusDistanceDiopters: Float,
    val supportsManualFocus: Boolean,
    val hasFlash: Boolean,
    val maxTorchStrengthLevel: Int,
    val isPhysicalOnly: Boolean = false
) {
    /** Approximate minimum focus distance in centimeters, or null if fixed/unknown. */
    val minFocusDistanceCm: Float?
        get() = if (minFocusDistanceDiopters > 0f) 100f / minFocusDistanceDiopters else null
}
