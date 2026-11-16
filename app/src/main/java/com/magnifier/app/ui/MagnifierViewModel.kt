package com.magnifier.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.magnifier.app.camera.CameraCapabilities
import com.magnifier.app.camera.CameraLensDiscovery
import com.magnifier.app.camera.CameraLensInfo
import com.magnifier.app.processing.InspectionFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MagnifierUiState(
    val lenses: List<CameraLensInfo> = emptyList(),
    val activeLens: CameraLensInfo? = null,
    val capabilities: CameraCapabilities = CameraCapabilities(),
    val zoomRatio: Float = 1f,
    val afLocked: Boolean = false,
    val manualFocusEnabled: Boolean = false,
    val focusDistanceDiopters: Float = 0f,
    val torchEnabled: Boolean = false,
    val torchStrength: Int = 1,
    val exposureIndex: Int = 0,
    val activeFilter: InspectionFilter = InspectionFilter.NORMAL,
    val showGrid: Boolean = false,
    val focusPeakingEnabled: Boolean = false,
    val isFrozen: Boolean = false,
    val frozenBitmap: Bitmap? = null,
    val controlsExpanded: Boolean = false,
    val lastSavedUri: Uri? = null,
    val statusMessage: String? = null
)

class MagnifierViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MagnifierUiState())
    val uiState: StateFlow<MagnifierUiState> = _uiState.asStateFlow()

    private val _peakingBitmap = MutableStateFlow<Bitmap?>(null)
    val peakingBitmap: StateFlow<Bitmap?> = _peakingBitmap.asStateFlow()

    init {
        val discovered = CameraLensDiscovery.discoverRearLenses(application)
        val defaultLens = discovered.firstOrNull { it.label == "Macro" } ?: discovered.firstOrNull()
        _uiState.update {
            it.copy(
                lenses = discovered,
                activeLens = defaultLens,
                torchStrength = (defaultLens?.maxTorchStrengthLevel ?: 1).coerceAtLeast(1)
            )
        }
    }

    fun selectLens(lens: CameraLensInfo) {
        _uiState.update {
            it.copy(
                activeLens = lens,
                zoomRatio = 1f,
                manualFocusEnabled = false,
                focusDistanceDiopters = 0f,
                afLocked = false,
                torchEnabled = false,
                torchStrength = lens.maxTorchStrengthLevel.coerceAtLeast(1),
                exposureIndex = 0
            )
        }
    }

    fun onCapabilitiesReady(capabilities: CameraCapabilities) {
        _uiState.update { state ->
            state.copy(
                capabilities = capabilities,
                zoomRatio = state.zoomRatio.coerceIn(capabilities.minZoom, capabilities.maxZoom),
                exposureIndex = state.exposureIndex.coerceIn(capabilities.evMin, capabilities.evMax)
            )
        }
    }

    fun setZoomRatio(ratio: Float) {
        _uiState.update { state ->
            val clamped = ratio.coerceIn(state.capabilities.minZoom, state.capabilities.maxZoom)
            state.copy(zoomRatio = clamped)
        }
    }

    fun toggleAfLock() {
        _uiState.update { it.copy(afLocked = !it.afLocked, manualFocusEnabled = false) }
    }

    fun setManualFocusEnabled(enabled: Boolean) {
        _uiState.update { state ->
            val maxDiopters = state.activeLens?.minFocusDistanceDiopters ?: 0f
            val initialDiopters = if (enabled && state.focusDistanceDiopters == 0f) {
                maxDiopters * 0.7f
            } else {
                state.focusDistanceDiopters
            }
            state.copy(
                manualFocusEnabled = enabled,
                afLocked = false,
                focusDistanceDiopters = initialDiopters
            )
        }
    }

    fun setFocusDistance(diopters: Float) {
        _uiState.update { state ->
            val maxDiopters = state.activeLens?.minFocusDistanceDiopters ?: 10f
            state.copy(
                manualFocusEnabled = true,
                afLocked = false,
                focusDistanceDiopters = diopters.coerceIn(0f, maxDiopters)
            )
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    fun setTorchStrength(level: Int) {
        _uiState.update { state ->
            val max = (state.activeLens?.maxTorchStrengthLevel ?: 1).coerceAtLeast(1)
            state.copy(torchStrength = level.coerceIn(1, max))
        }
    }

    fun setExposureIndex(index: Int) {
        _uiState.update { state ->
            state.copy(
                exposureIndex = index.coerceIn(state.capabilities.evMin, state.capabilities.evMax)
            )
        }
    }

    fun setFilter(filter: InspectionFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleFocusPeaking() {
        _uiState.update { state ->
            val next = !state.focusPeakingEnabled
            if (!next) _peakingBitmap.value = null
            state.copy(focusPeakingEnabled = next)
        }
    }

    fun updatePeakingBitmap(bitmap: Bitmap?) {
        val old = _peakingBitmap.value
        _peakingBitmap.value = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            old.recycle()
        }
    }

    fun setFrozenBitmap(bitmap: Bitmap) {
        val old = _uiState.value.frozenBitmap
        _uiState.update { it.copy(isFrozen = true, frozenBitmap = bitmap) }
        if (old != null && old !== bitmap && !old.isRecycled) {
            old.recycle()
        }
    }

    fun unfreeze() {
        val old = _uiState.value.frozenBitmap
        _uiState.update { it.copy(isFrozen = false, frozenBitmap = null) }
        if (old != null && !old.isRecycled) {
            old.recycle()
        }
    }

    fun toggleControlsDrawer() {
        _uiState.update { it.copy(controlsExpanded = !it.controlsExpanded) }
    }

    fun onSnapshotSaved(uri: Uri) {
        _uiState.update {
            it.copy(lastSavedUri = uri, statusMessage = "Saved to Pictures/Magnifier")
        }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
