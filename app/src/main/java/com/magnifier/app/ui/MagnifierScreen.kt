package com.magnifier.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.magnifier.app.camera.CameraController
import com.magnifier.app.camera.CameraLensInfo
import com.magnifier.app.processing.InspectionFilter
import com.magnifier.app.processing.SuperResolutionEnhancer
import com.magnifier.app.processing.TextScanner
import com.magnifier.app.storage.SnapshotRepository
import com.magnifier.app.ui.theme.LoupeAmber
import com.magnifier.app.ui.theme.LoupeCyan
import com.magnifier.app.ui.theme.LoupeSurface
import com.magnifier.app.ui.theme.LoupeSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private val GlassBorder = Color.White.copy(alpha = 0.14f)
private val GlassBg = Color(0xCC0D1117)

@Composable
fun MagnifierScreen(viewModel: MagnifierViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val state by viewModel.uiState.collectAsState()
    val peakingBitmap by viewModel.peakingBitmap.collectAsState()

    val controller = remember { CameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusRingScale = remember { Animatable(1.35f) }

    var frozenScale by remember { mutableFloatStateOf(1f) }
    var frozenOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(state.isFrozen) {
        if (!state.isFrozen) {
            frozenScale = 1f
            frozenOffset = Offset.Zero
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    LaunchedEffect(tapFocusPoint) {
        if (tapFocusPoint != null) {
            focusRingScale.snapTo(1.35f)
            focusRingScale.animateTo(1f, animationSpec = tween(durationMillis = 220))
            delay(950)
            tapFocusPoint = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    LaunchedEffect(state.activeLens?.cameraId) {
        controller.bind(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lens = state.activeLens,
            isFocusPeakingEnabled = { viewModel.uiState.value.focusPeakingEnabled && !viewModel.uiState.value.isFrozen },
            onPeakingBitmap = { viewModel.updatePeakingBitmap(it) },
            onCapabilitiesReady = { viewModel.onCapabilitiesReady(it) },
            onError = { viewModel.showMessage(it) }
        )
    }

    LaunchedEffect(state.hardwareZoom) {
        controller.setHardwareZoomRatio(state.hardwareZoom)
    }
    LaunchedEffect(state.manualFocusEnabled, state.focusDistanceDiopters, state.afLocked) {
        controller.applyFocusMode(
            manualMode = state.manualFocusEnabled,
            focusDistanceDiopters = state.focusDistanceDiopters,
            afLocked = state.afLocked
        )
    }
    LaunchedEffect(state.torchEnabled, state.torchStrength, state.activeLens?.cameraId) {
        controller.setTorch(
            enabled = state.torchEnabled,
            strengthLevel = state.torchStrength,
            maxStrengthLevel = state.activeLens?.maxTorchStrengthLevel ?: 1
        )
    }
    LaunchedEffect(state.exposureIndex) {
        controller.setExposureCompensation(state.exposureIndex)
    }

    LaunchedEffect(state.activeFilter) {
        if (state.activeFilter == InspectionFilter.NORMAL) {
            previewView.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix(state.activeFilter.toColorMatrixArray()))
            }
            previewView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        }
    }

    fun processZoomedBitmap(raw: Bitmap, targetMaxDim: Int): Bitmap {
        val dScale = state.digitalScale
        return if (state.clarityEnhanceEnabled || dScale > 1.05f) {
            SuperResolutionEnhancer.enhanceZoomedRegion(
                source = raw,
                digitalScale = dScale,
                targetMaxDim = targetMaxDim,
                sharpenStrength = if (state.clarityEnhanceEnabled) 0.70f else 0f
            ).also { processed ->
                if (processed !== raw) raw.recycle()
            }
        } else {
            raw
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
    ) {
        // 1. Live Camera Viewfinder with Hybrid 200x Scaling
        if (!state.isFrozen) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = state.digitalScale
                        scaleY = state.digitalScale
                    }
                    .pointerInput(state.capabilities.minZoom, state.maxTotalZoom) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                viewModel.setZoomRatio(viewModel.uiState.value.zoomRatio * zoom)
                            }
                        }
                    }
                    .pointerInput(state.afLocked) {
                        detectTapGestures { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tapFocusPoint = offset
                            controller.tapToFocus(
                                previewView = previewView,
                                x = offset.x,
                                y = offset.y,
                                lockAfterFocus = state.afLocked
                            )
                        }
                    }
            )
        }

        // 2. Frozen Super-Res Frame with Pan & Zoom
        val frozenBmp = state.frozenBitmap
        if (state.isFrozen && frozenBmp != null) {
            Image(
                bitmap = frozenBmp.asImageBitmap(),
                contentDescription = "Frozen inspection frame",
                contentScale = ContentScale.Crop,
                colorFilter = if (state.activeFilter == InspectionFilter.NORMAL) {
                    null
                } else {
                    ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix(state.activeFilter.toColorMatrixArray())
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            frozenScale = (frozenScale * zoom).coerceIn(1f, 8f)
                            if (frozenScale > 1f) {
                                frozenOffset += pan
                            } else {
                                frozenOffset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = frozenScale
                        scaleY = frozenScale
                        translationX = frozenOffset.x
                        translationY = frozenOffset.y
                    }
            )
        }

        // 3. Real-time Focus Peaking Overlay
        val peakBmp = peakingBitmap
        if (!state.isFrozen && state.focusPeakingEnabled && peakBmp != null && !peakBmp.isRecycled) {
            Image(
                bitmap = peakBmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = state.digitalScale
                        scaleY = state.digitalScale
                    }
            )
        }

        // 4. Measurement Reticle & Reference Grid Overlay
        if (state.showGrid) {
            ReticleGridOverlay(modifier = Modifier.fillMaxSize())
        }

        // 5. Animated Tap-to-Focus Ring
        tapFocusPoint?.let { point ->
            val ringSizePx = with(LocalDensity.current) { 76.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (point.x - ringSizePx / 2f).roundToInt(),
                            (point.y - ringSizePx / 2f).roundToInt()
                        )
                    }
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = focusRingScale.value
                        scaleY = focusRingScale.value
                    }
                    .border(2.dp, LoupeCyan, CircleShape)
            )
        }

        // Subtle top gradient scrim for HUD legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )

        // 6. Top Floating HUD
        TopLoupeBar(
            state = state,
            onSelectLens = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectLens(it)
            },
            onToggleAfLock = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleAfLock()
            },
            frozenScale = frozenScale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )

        // ML Kit Recognized Text / Serial Number Banner
        state.recognizedText?.let { text ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlassBg,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .border(1.dp, LoupeCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ML KIT DETECTED TEXT",
                            color = LoupeCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = text.ifEmpty { "No readable text found in frame." },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(text))
                            viewModel.showMessage("Copied text to clipboard")
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = LoupeCyan
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.setRecognizedText(null) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss OCR",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 7. Bottom Glassmorphic Control Deck
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = GlassBg,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                AnimatedVisibility(
                    visible = state.controlsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    InspectorDrawer(
                        state = state,
                        onManualFocusToggle = viewModel::setManualFocusEnabled,
                        onFocusDistanceChange = viewModel::setFocusDistance,
                        onTorchStrengthChange = viewModel::setTorchStrength,
                        onExposureChange = viewModel::setExposureIndex
                    )
                }

                // Filter & On-Device AI Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.clarityEnhanceEnabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleClarityEnhance()
                        },
                        label = { Text("AI Clarity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = LoupeSurfaceVariant.copy(alpha = 0.65f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = LoupeCyan.copy(alpha = 0.22f),
                            selectedLabelColor = LoupeCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.clarityEnhanceEnabled,
                            borderColor = GlassBorder,
                            selectedBorderColor = LoupeCyan.copy(alpha = 0.6f)
                        )
                    )

                    FilterChip(
                        selected = state.isScanningText || state.recognizedText != null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val frozen = state.frozenBitmap
                            if (state.isFrozen && frozen != null) {
                                viewModel.setScanningText(true)
                                TextScanner.recognizeText(
                                    bitmap = frozen,
                                    onResult = { viewModel.setRecognizedText(it) },
                                    onError = {
                                        viewModel.setScanningText(false)
                                        viewModel.showMessage(it)
                                    }
                                )
                            } else {
                                viewModel.setScanningText(true)
                                controller.captureBitmap(
                                    onCaptured = { raw ->
                                        scope.launch {
                                            val enhanced = withContext(Dispatchers.Default) {
                                                processZoomedBitmap(raw, targetMaxDim = 2048)
                                            }
                                            TextScanner.recognizeText(
                                                bitmap = enhanced,
                                                onResult = { text ->
                                                    viewModel.setRecognizedText(text)
                                                    enhanced.recycle()
                                                },
                                                onError = { err ->
                                                    viewModel.setScanningText(false)
                                                    viewModel.showMessage(err)
                                                    enhanced.recycle()
                                                }
                                            )
                                        }
                                    },
                                    onError = {
                                        viewModel.setScanningText(false)
                                        viewModel.showMessage(it)
                                    }
                                )
                            }
                        },
                        label = {
                            Text(
                                if (state.isScanningText) "Scanning..." else "ML Kit OCR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = LoupeSurfaceVariant.copy(alpha = 0.65f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = LoupeCyan.copy(alpha = 0.22f),
                            selectedLabelColor = LoupeCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.isScanningText || state.recognizedText != null,
                            borderColor = GlassBorder,
                            selectedBorderColor = LoupeCyan.copy(alpha = 0.6f)
                        )
                    )

                    InspectionFilter.entries.forEach { filter ->
                        val selected = state.activeFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setFilter(filter)
                            },
                            label = { Text(filter.label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = LoupeSurfaceVariant.copy(alpha = 0.65f),
                                labelColor = Color.White.copy(alpha = 0.85f),
                                selectedContainerColor = LoupeCyan.copy(alpha = 0.22f),
                                selectedLabelColor = LoupeCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GlassBorder,
                                selectedBorderColor = LoupeCyan.copy(alpha = 0.6f)
                            )
                        )
                    }

                    FilterChip(
                        selected = state.focusPeakingEnabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleFocusPeaking()
                        },
                        label = { Text("Peaking", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = LoupeSurfaceVariant.copy(alpha = 0.65f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = LoupeCyan.copy(alpha = 0.22f),
                            selectedLabelColor = LoupeCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.focusPeakingEnabled,
                            borderColor = GlassBorder,
                            selectedBorderColor = LoupeCyan.copy(alpha = 0.6f)
                        )
                    )

                    FilterChip(
                        selected = state.showGrid,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleGrid()
                        },
                        label = { Text("Grid", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.GridOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = LoupeSurfaceVariant.copy(alpha = 0.65f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = LoupeCyan.copy(alpha = 0.22f),
                            selectedLabelColor = LoupeCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.showGrid,
                            borderColor = GlassBorder,
                            selectedBorderColor = LoupeCyan.copy(alpha = 0.6f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Logarithmic 1x - 200x Zoom Presets + Precision Slider
                if (!state.isFrozen) {
                    ZoomControlRow200x(
                        zoomRatio = state.zoomRatio,
                        minZoom = state.capabilities.minZoom,
                        maxZoom = state.maxTotalZoom,
                        onZoomChange = viewModel::setZoomRatio
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LoupeAmber.copy(alpha = 0.14f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 8.dp)
                            .border(1.dp, LoupeAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "FROZEN · ${String.format(Locale.US, "%.1f×", frozenScale)} PAN/ZOOM · TAP PLAY TO RESUME",
                            color = LoupeAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Primary Thumb Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val torchTint by animateColorAsState(
                        targetValue = if (state.torchEnabled) LoupeAmber else Color.White,
                        label = "torchTint"
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (state.torchEnabled) LoupeAmber.copy(alpha = 0.18f) else LoupeSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(46.dp)
                            .border(
                                1.dp,
                                if (state.torchEnabled) LoupeAmber.copy(alpha = 0.6f) else GlassBorder,
                                CircleShape
                            )
                            .clip(CircleShape)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleTorch()
                            },
                            enabled = state.capabilities.hasFlash && !state.isFrozen
                        ) {
                            Icon(
                                imageVector = if (state.torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Toggle Flashlight",
                                tint = torchTint
                            )
                        }
                    }

                    // Freeze / Unfreeze (with Super-Res Clarity)
                    Surface(
                        shape = CircleShape,
                        color = if (state.isFrozen) LoupeAmber else LoupeSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(52.dp)
                            .border(1.dp, if (state.isFrozen) LoupeAmber else GlassBorder, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (state.isFrozen) {
                                    viewModel.unfreeze()
                                } else {
                                    controller.captureBitmap(
                                        onCaptured = { raw ->
                                            scope.launch {
                                                val enhanced = withContext(Dispatchers.Default) {
                                                    processZoomedBitmap(raw, targetMaxDim = 2048)
                                                }
                                                viewModel.setFrozenBitmap(enhanced)
                                            }
                                        },
                                        onError = { viewModel.showMessage(it) }
                                    )
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (state.isFrozen) "Resume live view" else "Freeze frame",
                                tint = if (state.isFrozen) Color.Black else Color.White
                            )
                        }
                    }

                    // Concentric Pro Shutter Button (saves up to 4K/8K-class super-res ROI)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(68.dp)
                            .border(2.5.dp, LoupeCyan, CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(LoupeCyan)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val currentFrozen = state.frozenBitmap
                                if (state.isFrozen && currentFrozen != null) {
                                    SnapshotRepository.saveToGallery(
                                        context = context,
                                        bitmap = currentFrozen,
                                        filter = state.activeFilter,
                                        zoomRatio = state.zoomRatio * frozenScale
                                    ).onSuccess { uri ->
                                        viewModel.onSnapshotSaved(uri)
                                    }.onFailure { err ->
                                        viewModel.showMessage("Save failed: ${err.localizedMessage}")
                                    }
                                } else {
                                    controller.captureBitmap(
                                        onCaptured = { raw ->
                                            scope.launch {
                                                val enhanced = withContext(Dispatchers.Default) {
                                                    processZoomedBitmap(raw, targetMaxDim = 3840)
                                                }
                                                SnapshotRepository.saveToGallery(
                                                    context = context,
                                                    bitmap = enhanced,
                                                    filter = state.activeFilter,
                                                    zoomRatio = state.zoomRatio
                                                ).onSuccess { uri ->
                                                    viewModel.onSnapshotSaved(uri)
                                                }.onFailure { err ->
                                                    viewModel.showMessage("Save failed: ${err.localizedMessage}")
                                                }
                                                enhanced.recycle()
                                            }
                                        },
                                        onError = { viewModel.showMessage(it) }
                                    )
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Save snapshot",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (state.controlsExpanded) LoupeCyan.copy(alpha = 0.2f) else LoupeSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(46.dp)
                            .border(
                                1.dp,
                                if (state.controlsExpanded) LoupeCyan.copy(alpha = 0.6f) else GlassBorder,
                                CircleShape
                            )
                            .clip(CircleShape)
                    ) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleControlsDrawer()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Focus and exposure controls",
                                tint = if (state.controlsExpanded) LoupeCyan else Color.White
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = LoupeSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clip(CircleShape)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val saved = state.lastSavedUri
                                val frozen = state.frozenBitmap
                                if (frozen != null) {
                                    SnapshotRepository.createShareUri(context, frozen, state.activeFilter)
                                        .onSuccess { uri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/jpeg"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Inspection"))
                                        }
                                } else if (saved != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, saved)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Snapshot"))
                                } else {
                                    controller.captureBitmap(
                                        onCaptured = { raw ->
                                            scope.launch {
                                                val enhanced = withContext(Dispatchers.Default) {
                                                    processZoomedBitmap(raw, targetMaxDim = 2560)
                                                }
                                                SnapshotRepository.createShareUri(context, enhanced, state.activeFilter)
                                                    .onSuccess { uri ->
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "image/jpeg"
                                                            putExtra(Intent.EXTRA_STREAM, uri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, "Share Inspection"))
                                                    }
                                                enhanced.recycle()
                                            }
                                        },
                                        onError = { viewModel.showMessage(it) }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share snapshot",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
        )
    }
}

@Composable
private fun TopLoupeBar(
    state: MagnifierUiState,
    onSelectLens: (CameraLensInfo) -> Unit,
    onToggleAfLock: () -> Unit,
    frozenScale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.lenses.forEach { lens ->
                val selected = state.activeLens?.cameraId == lens.cameraId
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) LoupeCyan else GlassBg,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) LoupeCyan else GlassBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectLens(lens) }
                ) {
                    Text(
                        text = lens.label,
                        color = if (selected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (state.afLocked) LoupeAmber else GlassBg,
            modifier = Modifier
                .border(
                    1.dp,
                    if (state.afLocked) LoupeAmber else GlassBorder,
                    RoundedCornerShape(18.dp)
                )
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = !state.isFrozen) { onToggleAfLock() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (state.afLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "AF Lock",
                    tint = if (state.afLocked) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (state.afLocked) "AF LOCK" else "AF",
                    color = if (state.afLocked) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        val displayZoom = if (state.isFrozen) state.zoomRatio * frozenScale else state.zoomRatio
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassBg,
            modifier = Modifier.border(1.dp, LoupeCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (state.clarityEnhanceEnabled || state.capabilities.computationalExtensionActive) {
                    Text(
                        text = "AI ",
                        color = LoupeAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = if (displayZoom >= 20f) {
                        String.format(Locale.US, "%.0f×", displayZoom)
                    } else {
                        String.format(Locale.US, "%.1f×", displayZoom)
                    },
                    color = LoupeCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ZoomControlRow200x(
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit
) {
    val presets = listOf(1f, 5f, 20f, 50f, 100f, 200f).filter { it in minZoom..maxZoom }

    // Logarithmic mapping so 1x..10x has fine precision while reaching 200x smoothly
    val safeMin = minZoom.coerceAtLeast(0.5f)
    val logRange = ln((maxZoom / safeMin).toDouble()).toFloat().coerceAtLeast(0.01f)
    val sliderProgress = (ln((zoomRatio.coerceIn(safeMin, maxZoom) / safeMin).toDouble()).toFloat() / logRange)
        .coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isClose = abs(zoomRatio - preset) / preset < 0.12f
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isClose) LoupeCyan.copy(alpha = 0.22f) else LoupeSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .border(
                            1.dp,
                            if (isClose) LoupeCyan.copy(alpha = 0.65f) else GlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onZoomChange(preset) }
                ) {
                    Text(
                        text = "${preset.toInt()}×",
                        color = if (isClose) LoupeCyan else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Slider(
            value = sliderProgress,
            onValueChange = { p ->
                val mappedZoom = safeMin * kotlin.math.E.pow((p * logRange).toDouble()).toFloat()
                onZoomChange(mappedZoom.coerceIn(minZoom, maxZoom))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = LoupeCyan,
                activeTrackColor = LoupeCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )
    }
}

@Composable
private fun InspectorDrawer(
    state: MagnifierUiState,
    onManualFocusToggle: (Boolean) -> Unit,
    onFocusDistanceChange: (Float) -> Unit,
    onTorchStrengthChange: (Int) -> Unit,
    onExposureChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LoupeSurface.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            val supportsManual = state.activeLens?.supportsManualFocus == true
            if (supportsManual) {
                val maxDiopters = state.activeLens!!.minFocusDistanceDiopters
                val distLabel = if (!state.manualFocusEnabled) {
                    "Auto"
                } else if (state.focusDistanceDiopters <= 0.1f) {
                    "∞"
                } else {
                    String.format(Locale.US, "~%.1f cm", 100f / state.focusDistanceDiopters)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manual Focus ($distLabel)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Switch(
                        checked = state.manualFocusEnabled,
                        onCheckedChange = onManualFocusToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = LoupeCyan
                        )
                    )
                }
                if (state.manualFocusEnabled) {
                    Slider(
                        value = state.focusDistanceDiopters.coerceIn(0f, maxDiopters),
                        onValueChange = onFocusDistanceChange,
                        valueRange = 0f..maxDiopters,
                        colors = SliderDefaults.colors(
                            thumbColor = LoupeCyan,
                            activeTrackColor = LoupeCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            val maxTorch = state.activeLens?.maxTorchStrengthLevel ?: 1
            if (state.torchEnabled && maxTorch > 1) {
                Text(
                    text = "Torch Strength (${state.torchStrength}/$maxTorch)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoupeAmber
                )
                Slider(
                    value = state.torchStrength.toFloat(),
                    onValueChange = { onTorchStrengthChange(it.roundToInt()) },
                    valueRange = 1f..maxTorch.toFloat(),
                    steps = (maxTorch - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = LoupeAmber,
                        activeTrackColor = LoupeAmber,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }

            val evMin = state.capabilities.evMin
            val evMax = state.capabilities.evMax
            if (evMax > evMin) {
                val evValue = state.exposureIndex * state.capabilities.evStep
                Text(
                    text = "Exposure (${String.format(Locale.US, "%+.1f EV", evValue)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Slider(
                    value = state.exposureIndex.toFloat(),
                    onValueChange = { onExposureChange(it.roundToInt()) },
                    valueRange = evMin.toFloat()..evMax.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}

@Composable
private fun ReticleGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val gridColor = Color.White.copy(alpha = 0.20f)
        val step = 64.dp.toPx()

        var x = cx % step
        while (x < w) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            x += step
        }
        var y = cy % step
        while (y < h) {
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            y += step
        }

        val reticleColor = LoupeCyan.copy(alpha = 0.8f)
        val arm = 24.dp.toPx()
        drawCircle(
            color = reticleColor,
            radius = 18.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
        drawLine(reticleColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 2f)
        drawLine(reticleColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 2f)
    }
}
