package com.magnifier.app.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Represents a decoded barcode or 2D matrix code with its raw payload and human-readable format.
 */
data class ScannedBarcode(
    val rawValue: String,
    val formatLabel: String
)

/**
 * On-device Google ML Kit Barcode Scanning wrapper supporting 1D and 2D formats
 * including DataMatrix, QR, Aztec, Code 128, and EAN.
 */
object BarcodeScanner {
    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    fun scanBarcodes(
        bitmap: Bitmap,
        onResult: (List<ScannedBarcode>) -> Unit,
        onError: (String) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val results = barcodes.mapNotNull { barcode ->
                    val raw = barcode.rawValue ?: barcode.displayValue
                    if (raw.isNullOrBlank()) {
                        null
                    } else {
                        ScannedBarcode(
                            rawValue = raw,
                            formatLabel = formatLabel(barcode.format)
                        )
                    }
                }
                onResult(results)
            }
            .addOnFailureListener { e ->
                onError("ML Kit barcode scan failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }

    internal fun formatLabel(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        else -> "Barcode"
    }
}
