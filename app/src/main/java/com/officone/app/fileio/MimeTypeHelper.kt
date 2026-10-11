package com.officone.app.fileio

import android.webkit.MimeTypeMap
import com.officone.app.document.DocumentType

object MimeTypeHelper {

    // Document MIME constants
    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val MIME_DOC = "application/msword"
    const val MIME_ODT = "application/vnd.oasis.opendocument.text"
    const val MIME_RTF = "application/rtf"
    const val MIME_RTF_TEXT = "text/rtf"
    const val MIME_TXT = "text/plain"
    const val MIME_MD = "text/markdown"
    const val MIME_MD_ALT = "text/x-markdown"
    const val MIME_HTML = "text/html"

    // Spreadsheet MIME constants
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_XLS = "application/vnd.ms-excel"
    const val MIME_ODS = "application/vnd.oasis.opendocument.spreadsheet"
    const val MIME_CSV = "text/csv"
    const val MIME_CSV_APP = "application/csv"
    const val MIME_TSV = "text/tab-separated-values"

    // Presentation MIME constants
    const val MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    const val MIME_PPT = "application/vnd.ms-powerpoint"
    const val MIME_ODP = "application/vnd.oasis.opendocument.presentation"

    // PDF MIME constant
    const val MIME_PDF = "application/pdf"

    // Fallback octet-stream
    const val MIME_OCTET_STREAM = "application/octet-stream"

    private val extensionToMimeMap: Map<String, String> = mapOf(
        // Documents
        "docx" to MIME_DOCX,
        "doc" to MIME_DOC,
        "odt" to MIME_ODT,
        "rtf" to MIME_RTF,
        "txt" to MIME_TXT,
        "text" to MIME_TXT,
        "log" to MIME_TXT,
        "md" to MIME_MD,
        "markdown" to MIME_MD,
        "html" to MIME_HTML,
        "htm" to MIME_HTML,

        // Spreadsheets
        "xlsx" to MIME_XLSX,
        "xls" to MIME_XLS,
        "ods" to MIME_ODS,
        "csv" to MIME_CSV,
        "tsv" to MIME_TSV,

        // Presentations
        "pptx" to MIME_PPTX,
        "ppt" to MIME_PPT,
        "odp" to MIME_ODP,

        // PDF
        "pdf" to MIME_PDF
    )

    private val mimeToExtensionMap: Map<String, String> = mapOf(
        MIME_DOCX to "docx",
        MIME_DOC to "doc",
        MIME_ODT to "odt",
        MIME_RTF to "rtf",
        MIME_RTF_TEXT to "rtf",
        MIME_TXT to "txt",
        MIME_MD to "md",
        MIME_MD_ALT to "md",
        MIME_HTML to "html",

        MIME_XLSX to "xlsx",
        MIME_XLS to "xls",
        MIME_ODS to "ods",
        MIME_CSV to "csv",
        MIME_CSV_APP to "csv",
        MIME_TSV to "tsv",

        MIME_PPTX to "pptx",
        MIME_PPT to "ppt",
        MIME_ODP to "odp",

        MIME_PDF to "pdf"
    )

    private val mimeToDocumentTypeMap: Map<String, DocumentType> = mapOf(
        MIME_DOCX to DocumentType.DOCUMENT,
        MIME_DOC to DocumentType.DOCUMENT,
        MIME_ODT to DocumentType.DOCUMENT,
        MIME_RTF to DocumentType.DOCUMENT,
        MIME_RTF_TEXT to DocumentType.DOCUMENT,
        MIME_TXT to DocumentType.DOCUMENT,
        MIME_MD to DocumentType.DOCUMENT,
        MIME_MD_ALT to DocumentType.DOCUMENT,
        MIME_HTML to DocumentType.DOCUMENT,

        MIME_XLSX to DocumentType.SPREADSHEET,
        MIME_XLS to DocumentType.SPREADSHEET,
        MIME_ODS to DocumentType.SPREADSHEET,
        MIME_CSV to DocumentType.SPREADSHEET,
        MIME_CSV_APP to DocumentType.SPREADSHEET,
        MIME_TSV to DocumentType.SPREADSHEET,

        MIME_PPTX to DocumentType.PRESENTATION,
        MIME_PPT to DocumentType.PRESENTATION,
        MIME_ODP to DocumentType.PRESENTATION,

        MIME_PDF to DocumentType.PDF
    )

    fun getDocumentTypeForMime(mimeType: String?): DocumentType {
        if (mimeType.isNullOrBlank()) return DocumentType.UNKNOWN
        val cleanMime = mimeType.trim().lowercase()
        return mimeToDocumentTypeMap[cleanMime] ?: DocumentType.fromMimeType(cleanMime)
    }

    fun getMimeTypeForExtension(ext: String?): String {
        if (ext.isNullOrBlank()) return MIME_OCTET_STREAM
        val cleanExt = ext.trim().lowercase().removePrefix(".")
        extensionToMimeMap[cleanExt]?.let { return it }

        val systemMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(cleanExt)
        return systemMime ?: MIME_OCTET_STREAM
    }

    fun getExtensionForMimeType(mime: String?): String {
        if (mime.isNullOrBlank()) return ""
        val cleanMime = mime.trim().lowercase()
        mimeToExtensionMap[cleanMime]?.let { return it }

        val systemExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime)
        return systemExt.orEmpty()
    }

    fun isSupported(mimeType: String?): Boolean {
        if (mimeType.isNullOrBlank()) return false
        val cleanMime = mimeType.trim().lowercase()
        return mimeToDocumentTypeMap.containsKey(cleanMime) || DocumentType.fromMimeType(cleanMime) != DocumentType.UNKNOWN
    }

    fun getAllSupportedMimeTypes(): Set<String> {
        return mimeToDocumentTypeMap.keys
    }
}
