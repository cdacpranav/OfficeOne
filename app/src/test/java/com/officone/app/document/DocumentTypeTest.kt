package com.officone.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTypeTest {

    @Test
    fun testFromExtension() {
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension("docx"))
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension("DOCX"))
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension(".doc"))
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension("odt"))
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension("rtf"))
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension("txt"))
        assertEquals(DocumentType.DOCUMENT, DocumentType.fromExtension("md"))

        assertEquals(DocumentType.SPREADSHEET, DocumentType.fromExtension("xlsx"))
        assertEquals(DocumentType.SPREADSHEET, DocumentType.fromExtension("xls"))
        assertEquals(DocumentType.SPREADSHEET, DocumentType.fromExtension("ods"))
        assertEquals(DocumentType.SPREADSHEET, DocumentType.fromExtension("csv"))
        assertEquals(DocumentType.SPREADSHEET, DocumentType.fromExtension("tsv"))

        assertEquals(DocumentType.PRESENTATION, DocumentType.fromExtension("pptx"))
        assertEquals(DocumentType.PRESENTATION, DocumentType.fromExtension("ppt"))
        assertEquals(DocumentType.PRESENTATION, DocumentType.fromExtension("odp"))

        assertEquals(DocumentType.PDF, DocumentType.fromExtension("pdf"))
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromExtension("zip"))
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromExtension(null))
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromExtension(""))
    }

    @Test
    fun testFromMimeType() {
        assertEquals(
            DocumentType.DOCUMENT,
            DocumentType.fromMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        )
        assertEquals(
            DocumentType.SPREADSHEET,
            DocumentType.fromMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        )
        assertEquals(
            DocumentType.PRESENTATION,
            DocumentType.fromMimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
        )
        assertEquals(
            DocumentType.PDF,
            DocumentType.fromMimeType("application/pdf")
        )
        assertEquals(
            DocumentType.UNKNOWN,
            DocumentType.fromMimeType("application/octet-stream")
        )
    }

    @Test
    fun testDisplayName() {
        assertEquals("Document", DocumentType.DOCUMENT.getDisplayName())
        assertEquals("Spreadsheet", DocumentType.SPREADSHEET.getDisplayName())
        assertEquals("Presentation", DocumentType.PRESENTATION.getDisplayName())
        assertEquals("PDF Document", DocumentType.PDF.getDisplayName())
    }

    @Test
    fun testSupportedMimeTypesNotEmpty() {
        val list = DocumentType.getSupportedMimeTypes()
        assertTrue(list.isNotEmpty())
        assertTrue(list.contains("application/pdf"))
        assertTrue(list.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
    }
}
