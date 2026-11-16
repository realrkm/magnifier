package com.magnifier.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.magnifier.app.processing.FocusPeakingAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class CameraCapabilities(
    val minZoom: Float = 1f,
    val maxZoom: Float = 8f,
    val evMin: Int = 0,
    val evMax: Int = 0,
    val evStep: Float = 0.5f,
    val hasFlash: Boolean = false
)

/**
 * Manages CameraX + Camera2Interop binding, zoom, AF lock, manual focus distance,
 * torch + variable strength, exposure compensation, focus peaking, and high-res capture.
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: CameraLensInfo?,
        isFocusPeakingEnabled: () -> Boolean,
        onPeakingBitmap: (Bitmap?) -> Unit,
        onCapabilitiesReady: (CameraCapabilities) -> Unit,
        onError: (String) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val selector = buildSelector(lens)

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(640, 480),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            analysisExecutor,
                            FocusPeakingAnalyzer(isFocusPeakingEnabled, onPeakingBitmap)
                        )
                    }
                imageAnalysis = analysis

                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture,
                    analysis
                )
                camera = boundCamera

                val info = boundCamera.cameraInfo
                val zoomState = info.zoomState.value
                val evState = info.exposureState
                onCapabilitiesReady(
                    CameraCapabilities(
                        minZoom = zoomState?.minZoomRatio ?: 1f,
                        maxZoom = (zoomState?.maxZoomRatio ?: 8f).coerceAtLeast(1f),
                        evMin = evState.exposureCompensationRange.lower,
                        evMax = evState.exposureCompensationRange.upper,
                        evStep = evState.exposureCompensationStep.toFloat(),
                        hasFlash = info.hasFlashUnit()
                    )
                )
            } catch (e: Exception) {
                onError("Camera init failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }, mainExecutor)
    }

    private fun buildSelector(lens: CameraLensInfo?): CameraSelector {
        if (lens == null) return CameraSelector.DEFAULT_BACK_CAMERA
        return CameraSelector.Builder()
            .addCameraFilter { cameras ->
                val matched = cameras.filter { info ->
                    runCatching { Camera2CameraInfo.from(info).cameraId == lens.cameraId }
                        .getOrDefault(false)
                }
                matched.ifEmpty { cameras }
            }
            .build()
    }

    fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val zoomState = cam.cameraInfo.zoomState.value
        val min = zoomState?.minZoomRatio ?: 1f
        val max = zoomState?.maxZoomRatio ?: 10f
        cam.cameraControl.setZoomRatio(ratio.coerceIn(min, max))
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float, lockAfterFocus: Boolean) {
        val cam = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val builder = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        if (lockAfterFocus) {
            builder.disableAutoCancel()
        } else {
            builder.setAutoCancelDuration(4, TimeUnit.SECONDS)
        }
        cam.cameraControl.startFocusAndMetering(builder.build())
    }

    fun applyFocusMode(manualMode: Boolean, focusDistanceDiopters: Float, afLocked: Boolean) {
        val cam = camera ?: return
        val cam2Control = Camera2CameraControl.from(cam.cameraControl)
        val options = if (manualMode) {
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                .setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    focusDistanceDiopters.coerceAtLeast(0f)
                )
                .build()
        } else if (afLocked) {
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO
                )
                .build()
        } else {
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                .build()
        }
        cam2Control.setCaptureRequestOptions(options)
    }

    fun setExposureCompensation(index: Int) {
        val cam = camera ?: return
        val range = cam.cameraInfo.exposureState.exposureCompensationRange
        cam.cameraControl.setExposureCompensationIndex(index.coerceIn(range.lower, range.upper))
    }

    fun setTorch(enabled: Boolean, strengthLevel: Int, maxStrengthLevel: Int) {
        val cam = camera ?: return
        cam.cameraControl.enableTorch(enabled)
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && maxStrengthLevel > 1) {
            val cam2Control = Camera2CameraControl.from(cam.cameraControl)
            val clamped = strengthLevel.coerceIn(1, maxStrengthLevel)
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                .setCaptureRequestOption(CaptureRequest.FLASH_STRENGTH_LEVEL, clamped)
                .build()
            cam2Control.setCaptureRequestOptions(options)
        }
    }

    fun captureBitmap(
        onCaptured: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera not ready")
            return
        }
        capture.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = imageProxyToBitmap(image)
                        onCaptured(bitmap)
                    } catch (e: Exception) {
                        onError("Failed to process image: ${e.localizedMessage}")
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError("Capture failed: ${exception.localizedMessage}")
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return raw
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            .also { raw.recycle() }
    }

    fun release() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }
}
