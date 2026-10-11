package com.officone.app.document

enum class DocumentType {
    DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    PDF,
    UNKNOWN;

    fun getDisplayName(): String {
        return when (this) {
            DOCUMENT -> "Document"
            SPREADSHEET -> "Spreadsheet"
            PRESENTATION -> "Presentation"
            PDF -> "PDF Document"
            UNKNOWN -> "Unknown Document"
        }
    }

    /**
     * Returns an icon resource identifier or fallback placeholder (android.R.drawable).
     */
    fun getIconResId(): Int {
        return when (this) {
            DOCUMENT -> android.R.drawable.ic_menu_edit
            SPREADSHEET -> android.R.drawable.ic_menu_agenda
            PRESENTATION -> android.R.drawable.ic_menu_slideshow
            PDF -> android.R.drawable.ic_menu_view
            UNKNOWN -> android.R.drawable.ic_menu_help
        }
    }

    companion object {
        fun fromMimeType(mimeType: String?): DocumentType {
            if (mimeType.isNullOrBlank()) return UNKNOWN
            val normalized = mimeType.trim().lowercase()

            return when {
                normalized == "application/pdf" -> PDF

                // Word Processing / Documents
                normalized == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                normalized == "application/msword" ||
                normalized == "application/vnd.oasis.opendocument.text" ||
                normalized == "application/rtf" ||
                normalized == "text/rtf" ||
                normalized == "text/plain" ||
                normalized == "text/markdown" ||
                normalized == "text/x-markdown" ||
                normalized == "text/html" -> DOCUMENT

                // Spreadsheets
                normalized == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                normalized == "application/vnd.ms-excel" ||
                normalized == "application/vnd.oasis.opendocument.spreadsheet" ||
                normalized == "text/csv" ||
                normalized == "text/tab-separated-values" ||
                normalized == "application/csv" -> SPREADSHEET

                // Presentations
                normalized == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
                normalized == "application/vnd.ms-powerpoint" ||
                normalized == "application/vnd.oasis.opendocument.presentation" -> PRESENTATION

                else -> UNKNOWN
            }
        }

        fun fromExtension(extension: String?): DocumentType {
            if (extension.isNullOrBlank()) return UNKNOWN
            val ext = extension.trim().lowercase().removePrefix(".")

            return when (ext) {
                // PDF
                "pdf" -> PDF

                // Word Processing / Text
                "docx", "doc", "odt", "rtf", "txt", "md", "markdown", "html", "htm" -> DOCUMENT

                // Spreadsheets
                "xlsx", "xls", "ods", "csv", "tsv" -> SPREADSHEET

                // Presentations
                "pptx", "ppt", "odp" -> PRESENTATION

                else -> UNKNOWN
            }
        }

        fun getSupportedMimeTypes(): List<String> {
            return listOf(
                // Word / Document
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "application/vnd.oasis.opendocument.text",
                "application/rtf",
                "text/rtf",
                "text/plain",
                "text/markdown",
                "text/x-markdown",
                "text/html",
                // Spreadsheet
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "application/vnd.oasis.opendocument.spreadsheet",
                "text/csv",
                "text/tab-separated-values",
                "application/csv",
                // Presentation
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-powerpoint",
                "application/vnd.oasis.opendocument.presentation",
                // PDF
                "application/pdf"
            )
        }
    }
}
