package com.magnifier.app.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.magnifier.app.processing.InspectionFilter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves inspection snapshots to the device gallery via MediaStore and creates
 * temporary FileProvider URIs for instant sharing.
 */
object SnapshotRepository {

    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        filter: InspectionFilter,
        zoomRatio: Float
    ): Result<Uri> = runCatching {
        val processed = filter.applyToBitmap(bitmap)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "MAG_${timestamp}_${String.format(Locale.US, "%.1fx", zoomRatio)}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Magnifier"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { out ->
            if (!processed.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                error("Failed to compress JPEG")
            }
        } ?: error("Unable to open output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        if (processed !== bitmap) {
            processed.recycle()
        }
        uri
    }

    fun createShareUri(
        context: Context,
        bitmap: Bitmap,
        filter: InspectionFilter
    ): Result<Uri> = runCatching {
        val processed = filter.applyToBitmap(bitmap)
        val dir = File(context.cacheDir, "snapshots").apply { mkdirs() }
        val file = File(dir, "magnifier_share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            processed.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        if (processed !== bitmap) {
            processed.recycle()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
