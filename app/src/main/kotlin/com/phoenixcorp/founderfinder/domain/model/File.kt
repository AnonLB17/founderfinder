package com.phoenixcorp.founderfinder.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

/**
 * Document stored at organizations/{orgId}/files/{fileId}.
 * Binary lives in Firebase Storage at [storagePath].
 */
@Keep
data class File(
    val fileId: String = "",
    val orgId: String = "",
    val name: String = "",
    val type: String = "",
    val url: String = "",
    val storagePath: String = "",
    val mimeType: String = "",
    val uploadedBy: String = "",
    @get:PropertyName("uploadedAt")
    val uploadedAt: Long = 0L
)
