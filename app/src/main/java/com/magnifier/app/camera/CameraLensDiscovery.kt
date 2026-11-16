package com.magnifier.app.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/**
 * Discovers rear-facing logical and physical camera lenses (Main, Macro, Ultra-Wide, Tele)
 * using Camera2 characteristics.
 */
object CameraLensDiscovery {

    fun discoverRearLenses(context: Context): List<CameraLensInfo> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()

        val lenses = mutableListOf<CameraLensInfo>()
        val seenFocalBuckets = mutableSetOf<String>()

        for (cameraId in manager.cameraIdList) {
            val chars = runCatching { manager.getCameraCharacteristics(cameraId) }.getOrNull()
                ?: continue
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

            val lensInfo = buildLensInfo(cameraId, chars, isPhysicalOnly = false)
            lenses.add(lensInfo)
            seenFocalBuckets.add("${lensInfo.label}:${lensInfo.focalLengthMm}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (physicalId in chars.physicalCameraIds) {
                    if (physicalId == cameraId) continue
                    val pChars = runCatching { manager.getCameraCharacteristics(physicalId) }.getOrNull()
                        ?: continue
                    val pInfo = buildLensInfo(physicalId, pChars, isPhysicalOnly = true)
                    val bucket = "${pInfo.label}:${pInfo.focalLengthMm}"
                    if (seenFocalBuckets.add(bucket)) {
                        lenses.add(pInfo)
                    }
                }
            }
        }

        // Prefer Macro/Main first, then Ultra-Wide, then Tele
        return lenses
            .distinctBy { it.cameraId }
            .sortedWith(
                compareBy<CameraLensInfo> { it.isPhysicalOnly }
                    .thenBy { lensSortOrder(it.label) }
                    .thenBy { it.focalLengthMm }
            )
    }

    private fun buildLensInfo(
        cameraId: String,
        chars: CameraCharacteristics,
        isPhysicalOnly: Boolean
    ): CameraLensInfo {
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val focalLength = focalLengths?.firstOrNull() ?: 4.0f
        val minFocusDiopters = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
        val supportsManualFocus = minFocusDiopters > 0f &&
            afModes.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF)

        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val maxTorchLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasFlash) {
            chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } else {
            1
        }

        val label = classifyLensLabel(focalLength, minFocusDiopters)

        return CameraLensInfo(
            cameraId = cameraId,
            label = label,
            focalLengthMm = focalLength,
            minFocusDistanceDiopters = minFocusDiopters,
            supportsManualFocus = supportsManualFocus,
            hasFlash = hasFlash,
            maxTorchStrengthLevel = maxTorchLevel,
            isPhysicalOnly = isPhysicalOnly
        )
    }

    internal fun classifyLensLabel(focalLengthMm: Float, minFocusDiopters: Float): String {
        // High diopter minimum focus distance (>= 15 diopters -> <= ~6.6cm) indicates macro capability
        return when {
            minFocusDiopters >= 15f && focalLengthMm <= 4.5f -> "Macro"
            focalLengthMm < 2.6f -> "0.6x Wide"
            focalLengthMm > 7.5f -> "Tele"
            else -> "Main"
        }
    }

    private fun lensSortOrder(label: String): Int = when (label) {
        "Main" -> 0
        "Macro" -> 1
        "0.6x Wide" -> 2
        "Tele" -> 3
        else -> 4
    }
}
