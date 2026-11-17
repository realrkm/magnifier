package com.magnifier.app.processing

import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions

/**
 * Represents a structured entity extracted from recognized text.
 */
data class ExtractedEntity(
    val text: String,
    val typeLabel: String
)

/**
 * On-device Google ML Kit Entity Extraction wrapper for identifying actionable entities
 * (URLs, emails, phone numbers, tracking numbers, dates, addresses, flight numbers, etc.)
 * from scanned text.
 */
object EntityExtractorHelper {
    private val extractor by lazy {
        val options = EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
        EntityExtraction.getClient(options)
    }

    fun extractEntities(
        text: String,
        onResult: (List<ExtractedEntity>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (text.isBlank()) {
            onResult(emptyList())
            return
        }

        extractor.downloadModelIfNeeded()
            .addOnSuccessListener {
                extractor.annotate(text)
                    .addOnSuccessListener { annotations ->
                        val entities = annotations.mapNotNull { annotation ->
                            val primaryEntity = annotation.entities.firstOrNull() ?: return@mapNotNull null
                            ExtractedEntity(
                                text = annotation.annotatedText,
                                typeLabel = entityTypeLabel(primaryEntity.type)
                            )
                        }
                        onResult(entities)
                    }
                    .addOnFailureListener { e ->
                        onError("Entity extraction failed: ${e.localizedMessage ?: "Unknown error"}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Entity model download failed: ${e.localizedMessage ?: "Unknown error"}")
            }
    }

    internal fun entityTypeLabel(type: Int): String = when (type) {
        Entity.TYPE_URL -> "URL"
        Entity.TYPE_EMAIL -> "Email"
        Entity.TYPE_PHONE -> "Phone"
        Entity.TYPE_TRACKING_NUMBER -> "Tracking Number"
        Entity.TYPE_DATE_TIME -> "Date/Time"
        Entity.TYPE_ADDRESS -> "Address"
        Entity.TYPE_FLIGHT_NUMBER -> "Flight Number"
        Entity.TYPE_IBAN -> "IBAN"
        Entity.TYPE_ISBN -> "ISBN"
        Entity.TYPE_MONEY -> "Money"
        Entity.TYPE_PAYMENT_CARD -> "Payment Card"
        else -> "Entity"
    }
}
