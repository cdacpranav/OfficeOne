package com.officone.app.fileio

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.officone.app.document.DocumentInfo
import com.officone.app.document.DocumentType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class SAFFileHandler {

    /**
     * Resolves metadata for the provided Content URI and returns a [DocumentInfo] instance.
     */
    fun openDocument(context: Context, uri: Uri): DocumentInfo {
        var displayName = "Unknown"
        var size = -1L
        var lastModified = System.currentTimeMillis()
        var isReadOnly = false

        val contentResolver: ContentResolver = context.contentResolver

        try {
            contentResolver.query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_FLAGS
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex)
                    }

                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }

                    val modIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (modIndex != -1 && !cursor.isNull(modIndex)) {
                        lastModified = cursor.getLong(modIndex)
                    }

                    val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
                    if (flagsIndex != -1 && !cursor.isNull(flagsIndex)) {
                        val flags = cursor.getInt(flagsIndex)
                        isReadOnly = (flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) == 0
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query full metadata for $uri: ${e.message}")
        }

        // Fallback name if missing
        if (displayName == "Unknown") {
            uri.lastPathSegment?.let { segment ->
                val clean = segment.substringAfterLast('/')
                if (clean.isNotBlank()) displayName = clean
            }
        }

        val mimeType = getMimeType(context, uri)
        val docType = if (mimeType.isNotBlank() && mimeType != MimeTypeHelper.MIME_OCTET_STREAM) {
            MimeTypeHelper.getDocumentTypeForMime(mimeType)
        } else {
            val ext = displayName.substringAfterLast('.', "")
            DocumentType.fromExtension(ext)
        }

        return DocumentInfo(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            size = size,
            lastModified = lastModified,
            documentType = docType,
            isReadOnly = isReadOnly
        )
    }

    /**
     * Copies the content from [uri] into a private cache file inside the application cache directory.
     */
    @Throws(IOException::class)
    fun copyToCache(context: Context, uri: Uri): File {
        val cacheDir = File(context.cacheDir, CACHE_SUBDIR).apply { if (!exists()) mkdirs() }
        val ext = getFileExtension(context, uri)
        val prefix = "doc_${System.currentTimeMillis()}"
        val suffix = if (ext.isNotEmpty()) ".$ext" else ".tmp"

        val cacheFile = File.createTempFile(prefix, suffix, cacheDir)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(cacheFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Could not open input stream for uri: $uri")

        return cacheFile
    }

    /**
     * Writes the contents of [cacheFile] back to the target content [uri].
     * Returns true if successful, false otherwise.
     */
    fun saveToUri(context: Context, uri: Uri, cacheFile: File): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                FileInputStream(cacheFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.flush()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cache file to uri: $uri", e)
            false
        }
    }

    /**
     * Helper to retrieve MIME type for the URI, falling back to extension detection.
     */
    fun getMimeType(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri)
        if (!type.isNullOrBlank() && type != MimeTypeHelper.MIME_OCTET_STREAM) {
            return type
        }

        val ext = getFileExtension(context, uri)
        return MimeTypeHelper.getMimeTypeForExtension(ext)
    }

    /**
     * Extracts file extension from URI path or content query.
     */
    fun getFileExtension(context: Context, uri: Uri): String {
        // First try query display name
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        val name = cursor.getString(nameIndex)
                        val ext = name.substringAfterLast('.', "")
                        if (ext.isNotBlank()) return ext.lowercase()
                    }
                }
            }
        } catch (ignored: Exception) {}

        // Fallback to path segment
        val segment = uri.lastPathSegment
        if (!segment.isNullOrBlank() && segment.contains(".")) {
            return segment.substringAfterLast('.').lowercase()
        }

        // Fallback to resolver MIME type
        val mime = context.contentResolver.getType(uri)
        return MimeTypeHelper.getExtensionForMimeType(mime)
    }

    /**
     * Takes persistable URI read and/or write permission grants for SAF documents.
     */
    fun takePersistablePermission(
        context: Context,
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    ) {
        try {
            val contentResolver = context.contentResolver
            val takeFlags = flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to take persistable URI permission for $uri: ${e.message}")
        }
    }

    /**
     * Releases persistable URI permissions if no longer needed.
     */
    fun releasePersistablePermission(context: Context, uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val releaseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.releasePersistableUriPermission(uri, releaseFlags)
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to release persistable URI permission for $uri: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SAFFileHandler"
        private const val CACHE_SUBDIR = "opened_documents"
    }
}
