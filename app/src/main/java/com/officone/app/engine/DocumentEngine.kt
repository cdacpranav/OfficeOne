package com.officone.app.engine

import android.graphics.Bitmap
import com.officone.app.document.DocumentType
import java.io.File

/**
 * Common interface defining the contract for document rendering, editing, and querying.
 */
interface DocumentEngine {
    /**
     * Loads a document from file. Supports optional password protection.
     */
    suspend fun loadDocument(file: File, password: String? = null): DocumentHandle

    /**
     * Saves the document represented by [handle] to [outputFile] with an optional [format].
     */
    suspend fun saveDocument(handle: DocumentHandle, outputFile: File, format: SaveFormat? = null): Boolean

    /**
     * Returns the total page count for the loaded document.
     */
    fun getPageCount(handle: DocumentHandle): Int

    /**
     * Renders a specific 0-based page index into a [Bitmap] at requested dimensions.
     */
    suspend fun renderPage(handle: DocumentHandle, page: Int, width: Int, height: Int): Bitmap?

    /**
     * Returns the document type of the opened document.
     */
    fun getDocumentType(handle: DocumentHandle): DocumentType

    /**
     * Closes the document handle and releases all associated resources.
     */
    fun closeDocument(handle: DocumentHandle)

    // Editing operations
    fun insertText(handle: DocumentHandle, text: String)
    fun deleteText(handle: DocumentHandle, start: Int, end: Int)
    fun applyFormatting(handle: DocumentHandle, format: TextFormat)
    fun undo(handle: DocumentHandle): Boolean
    fun redo(handle: DocumentHandle): Boolean

    // Query operations
    fun getWordCount(handle: DocumentHandle): Int
    fun getCharacterCount(handle: DocumentHandle): Int
    fun searchText(handle: DocumentHandle, query: String): List<SearchResult>
}

/**
 * Handle identifying an active document session in a [DocumentEngine].
 */
data class DocumentHandle(
    val id: Long,
    val filePath: String,
    val documentType: DocumentType,
    val isModified: Boolean = false
)

/**
 * Supported document export and save formats.
 */
enum class SaveFormat {
    DOCX, DOC, ODT, RTF, PDF, TXT, HTML,
    XLSX, XLS, ODS, CSV,
    PPTX, PPT, ODP
}

/**
 * Text formatting properties applied to document selections.
 */
data class TextFormat(
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val fontFamily: String? = null,
    val fontSize: Float? = null,
    val fontColor: Int? = null,
    val highlightColor: Int? = null,
    val alignment: TextAlignment? = null
)

/**
 * Alignment options for text blocks and paragraphs.
 */
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

/**
 * Search hit information within a document.
 */
data class SearchResult(
    val page: Int,
    val startOffset: Int,
    val endOffset: Int,
    val snippet: String
)
