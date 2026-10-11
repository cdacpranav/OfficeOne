package com.officone.app.document

import android.net.Uri

data class DocumentInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val documentType: DocumentType,
    val isReadOnly: Boolean
) {
    /**
     * Human-readable file size string (e.g., "1.2 MB", "450 KB").
     */
    fun formattedSize(): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, index.toDouble()), units[index])
    }
}
