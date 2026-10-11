package com.officone.app.fileio

import com.officone.app.document.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeTypeHelperTest {

    @Test
    fun testGetDocumentTypeForMime() {
        assertEquals(DocumentType.DOCUMENT, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_DOCX))
        assertEquals(DocumentType.DOCUMENT, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_DOC))
        assertEquals(DocumentType.DOCUMENT, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_ODT))
        assertEquals(DocumentType.DOCUMENT, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_TXT))

        assertEquals(DocumentType.SPREADSHEET, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_XLSX))
        assertEquals(DocumentType.SPREADSHEET, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_XLS))
        assertEquals(DocumentType.SPREADSHEET, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_ODS))
        assertEquals(DocumentType.SPREADSHEET, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_CSV))

        assertEquals(DocumentType.PRESENTATION, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_PPTX))
        assertEquals(DocumentType.PRESENTATION, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_PPT))
        assertEquals(DocumentType.PRESENTATION, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_ODP))

        assertEquals(DocumentType.PDF, MimeTypeHelper.getDocumentTypeForMime(MimeTypeHelper.MIME_PDF))
        assertEquals(DocumentType.UNKNOWN, MimeTypeHelper.getDocumentTypeForMime("application/unknown-binary"))
        assertEquals(DocumentType.UNKNOWN, MimeTypeHelper.getDocumentTypeForMime(null))
    }

    @Test
    fun testGetMimeTypeForExtension() {
        assertEquals(MimeTypeHelper.MIME_DOCX, MimeTypeHelper.getMimeTypeForExtension("docx"))
        assertEquals(MimeTypeHelper.MIME_DOCX, MimeTypeHelper.getMimeTypeForExtension(".DOCX"))
        assertEquals(MimeTypeHelper.MIME_XLSX, MimeTypeHelper.getMimeTypeForExtension("xlsx"))
        assertEquals(MimeTypeHelper.MIME_PPTX, MimeTypeHelper.getMimeTypeForExtension("pptx"))
        assertEquals(MimeTypeHelper.MIME_PDF, MimeTypeHelper.getMimeTypeForExtension("pdf"))
        assertEquals(MimeTypeHelper.MIME_CSV, MimeTypeHelper.getMimeTypeForExtension("csv"))
    }

    @Test
    fun testIsSupported() {
        assertTrue(MimeTypeHelper.isSupported(MimeTypeHelper.MIME_DOCX))
        assertTrue(MimeTypeHelper.isSupported(MimeTypeHelper.MIME_XLSX))
        assertTrue(MimeTypeHelper.isSupported(MimeTypeHelper.MIME_PPTX))
        assertTrue(MimeTypeHelper.isSupported(MimeTypeHelper.MIME_PDF))
        assertTrue(MimeTypeHelper.isSupported(MimeTypeHelper.MIME_TXT))
    }

    @Test
    fun testGetAllSupportedMimeTypes() {
        val types = MimeTypeHelper.getAllSupportedMimeTypes()
        assertTrue(types.contains(MimeTypeHelper.MIME_DOCX))
        assertTrue(types.contains(MimeTypeHelper.MIME_XLSX))
        assertTrue(types.contains(MimeTypeHelper.MIME_PPTX))
        assertTrue(types.contains(MimeTypeHelper.MIME_PDF))
    }
}
