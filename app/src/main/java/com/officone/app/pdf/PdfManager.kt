package com.officone.app.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationUnderline
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Data model for PDF search hits.
 */
data class PdfSearchResult(
    val pageIndex: Int,
    val snippet: String,
    val startOffset: Int = 0,
    val endOffset: Int = 0
)

/**
 * Form field representation extracted from PDF AcroForm.
 */
data class PdfFormField(
    val name: String,
    val value: String,
    val isReadOnly: Boolean
)

/**
 * Supported annotation types and payloads.
 */
sealed class PdfAnnotation {
    data class Highlight(val rect: RectF, val color: Int = Color.YELLOW) : PdfAnnotation()
    data class Underline(val rect: RectF, val color: Int = Color.RED) : PdfAnnotation()
    data class Note(val position: PointF, val contents: String) : PdfAnnotation()
    data class Link(val rect: RectF, val url: String) : PdfAnnotation()
}

/**
 * Comprehensive PDF document management and manipulation class using PdfBox-Android.
 *
 * Implements [Closeable] to ensure underlying [PDDocument] handles are safely released.
 */
class PdfManager : Closeable {

    private var document: PDDocument? = null
    private var renderer: PDFRenderer? = null
    private var currentFile: File? = null

    /**
     * Opens a PDF file and prepares it for rendering and manipulation.
     */
    @Throws(IOException::class)
    fun openPdf(file: File): PDDocument {
        close()
        val doc = PDDocument.load(file)
        document = doc
        renderer = PDFRenderer(doc)
        currentFile = file
        Log.d(TAG, "Opened PDF: ${file.name}, pages: ${doc.numberOfPages}")
        return doc
    }

    /**
     * Returns total page count of currently loaded PDF.
     */
    fun getPageCount(): Int {
        return document?.numberOfPages ?: 0
    }

    /**
     * Renders a 0-indexed page into a [Bitmap] scaled to fit within [width] and [height].
     */
    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? = withContext(Dispatchers.Default) {
        val currentRenderer = renderer ?: return@withContext null
        val doc = document ?: return@withContext null

        if (pageIndex !in 0 until doc.numberOfPages) {
            Log.w(TAG, "renderPage index out of bounds: $pageIndex (pages=${doc.numberOfPages})")
            return@withContext null
        }

        try {
            val page = doc.getPage(pageIndex)
            val cropBox = page.cropBox
            val pageWidth = cropBox.width
            val pageHeight = cropBox.height

            val scaleX = width.toFloat() / pageWidth
            val scaleY = height.toFloat() / pageHeight
            val scale = minOf(scaleX, scaleY).coerceAtLeast(0.1f)

            currentRenderer.renderImage(pageIndex, scale)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render page $pageIndex", e)
            null
        }
    }

    /**
     * Extracts plain text from the specified 0-indexed page.
     */
    suspend fun extractText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        val doc = document ?: return@withContext ""
        if (pageIndex !in 0 until doc.numberOfPages) return@withContext ""

        try {
            val stripper = PDFTextStripper().apply {
                startPage = pageIndex + 1
                endPage = pageIndex + 1
            }
            stripper.getText(doc) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from page $pageIndex", e)
            ""
        }
    }

    /**
     * Searches for occurrences of [query] across all pages of the document.
     */
    suspend fun searchText(query: String): List<PdfSearchResult> = withContext(Dispatchers.IO) {
        val doc = document ?: return@withContext emptyList()
        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<PdfSearchResult>()
        val totalPages = doc.numberOfPages

        val stripper = PDFTextStripper()
        for (i in 0 until totalPages) {
            stripper.startPage = i + 1
            stripper.endPage = i + 1
            val pageText = try {
                stripper.getText(doc) ?: ""
            } catch (e: Exception) {
                ""
            }

            var index = pageText.indexOf(query, ignoreCase = true)
            while (index >= 0) {
                val start = (index - 25).coerceAtLeast(0)
                val end = (index + query.length + 25).coerceAtMost(pageText.length)
                val snippet = pageText.substring(start, end).replace('\n', ' ')

                results.add(
                    PdfSearchResult(
                        pageIndex = i,
                        snippet = "...$snippet...",
                        startOffset = index,
                        endOffset = index + query.length
                    )
                )
                index = pageText.indexOf(query, index + query.length, ignoreCase = true)
            }
        }

        results
    }

    /**
     * Adds an annotation (Highlight, Underline, Note, or Link) to [pageIndex].
     */
    fun addAnnotation(pageIndex: Int, annotation: PdfAnnotation) {
        val doc = document ?: return
        if (pageIndex !in 0 until doc.numberOfPages) return

        try {
            val page = doc.getPage(pageIndex)
            val pdAnnotation: PDAnnotation = when (annotation) {
                is PdfAnnotation.Highlight -> {
                    PDAnnotationHighlight().apply {
                        val rect = PDRectangle(
                            annotation.rect.left,
                            annotation.rect.top,
                            annotation.rect.width(),
                            annotation.rect.height()
                        )
                        rectangle = rect
                        // 8 float values for quad points [x1, y1, x2, y2, x3, y3, x4, y4]
                        quadPoints = floatArrayOf(
                            rect.lowerLeftX, rect.upperRightY,
                            rect.upperRightX, rect.upperRightY,
                            rect.lowerLeftX, rect.lowerLeftY,
                            rect.upperRightX, rect.lowerLeftY
                        )
                    }
                }
                is PdfAnnotation.Underline -> {
                    PDAnnotationUnderline().apply {
                        val rect = PDRectangle(
                            annotation.rect.left,
                            annotation.rect.top,
                            annotation.rect.width(),
                            annotation.rect.height()
                        )
                        rectangle = rect
                        quadPoints = floatArrayOf(
                            rect.lowerLeftX, rect.upperRightY,
                            rect.upperRightX, rect.upperRightY,
                            rect.lowerLeftX, rect.lowerLeftY,
                            rect.upperRightX, rect.lowerLeftY
                        )
                    }
                }
                is PdfAnnotation.Note -> {
                    PDAnnotationText().apply {
                        rectangle = PDRectangle(
                            annotation.position.x,
                            annotation.position.y,
                            24f,
                            24f
                        )
                        contents = annotation.contents
                    }
                }
                is PdfAnnotation.Link -> {
                    PDAnnotationLink().apply {
                        rectangle = PDRectangle(
                            annotation.rect.left,
                            annotation.rect.top,
                            annotation.rect.width(),
                            annotation.rect.height()
                        )
                        val action = PDActionURI().apply {
                            uri = annotation.url
                        }
                        this.action = action
                    }
                }
            }

            page.annotations.add(pdAnnotation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add annotation to page $pageIndex", e)
        }
    }

    /**
     * Sets value for a given form field in AcroForm.
     */
    fun fillFormField(fieldName: String, value: String) {
        val doc = document ?: return
        val acroForm: PDAcroForm = doc.documentCatalog.acroForm ?: return

        try {
            val field: PDField? = acroForm.getField(fieldName)
            if (field != null) {
                field.setValue(value)
            } else {
                Log.w(TAG, "Field not found: $fieldName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fill form field $fieldName", e)
        }
    }

    /**
     * Lists all interactive form fields available in the document.
     */
    fun getFormFields(): List<PdfFormField> {
        val doc = document ?: return emptyList()
        val acroForm = doc.documentCatalog.acroForm ?: return emptyList()

        return acroForm.fields.map { field ->
            PdfFormField(
                name = field.fullyQualifiedName ?: field.partialName ?: "",
                value = field.valueAsString ?: "",
                isReadOnly = field.isReadOnly
            )
        }
    }

    /**
     * Merges multiple PDF files into an [output] file.
     */
    suspend fun mergePdfs(files: List<File>, output: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val merger = PDFMergerUtility().apply {
                destinationFileName = output.absolutePath
            }
            for (file in files) {
                merger.addSource(file)
            }
            output.parentFile?.mkdirs()
            merger.mergeDocuments(null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error merging PDFs", e)
            false
        }
    }

    /**
     * Splits a PDF file into parts defined by [ranges].
     */
    suspend fun splitPdf(
        file: File,
        outputDir: File,
        ranges: List<IntRange>
    ): List<File> = withContext(Dispatchers.IO) {
        val createdFiles = mutableListOf<File>()
        try {
            outputDir.mkdirs()
            PDDocument.load(file).use { srcDoc ->
                for ((idx, range) in ranges.withIndex()) {
                    val subDoc = PDDocument()
                    for (p in range) {
                        if (p in 0 until srcDoc.numberOfPages) {
                            subDoc.addPage(srcDoc.getPage(p))
                        }
                    }
                    val targetFile = File(outputDir, "${file.nameWithoutExtension}_part_${idx + 1}.pdf")
                    subDoc.save(targetFile)
                    subDoc.close()
                    createdFiles.add(targetFile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error splitting PDF", e)
        }
        createdFiles
    }

    /**
     * Deletes a page at 0-indexed [pageIndex].
     */
    fun deletePage(pageIndex: Int) {
        val doc = document ?: return
        if (pageIndex in 0 until doc.numberOfPages) {
            doc.removePage(pageIndex)
            renderer = PDFRenderer(doc)
        }
    }

    /**
     * Rotates page at [pageIndex] by [degrees] (e.g. 90, 180, 270).
     */
    fun rotatePage(pageIndex: Int, degrees: Int) {
        val doc = document ?: return
        if (pageIndex in 0 until doc.numberOfPages) {
            val page = doc.getPage(pageIndex)
            val currentRotation = page.rotation
            page.rotation = (currentRotation + degrees) % 360
        }
    }

    /**
     * Reorders pages based on a list of 0-based page indices.
     */
    fun reorderPages(newOrder: List<Int>) {
        val doc = document ?: return
        val currentCount = doc.numberOfPages
        val validOrder = newOrder.filter { it in 0 until currentCount }
        if (validOrder.isEmpty()) return

        val newDoc = PDDocument()
        for (pageIdx in validOrder) {
            newDoc.addPage(doc.getPage(pageIdx))
        }

        close()
        document = newDoc
        renderer = PDFRenderer(newDoc)
    }

    /**
     * Saves changes to [outputFile].
     */
    suspend fun savePdf(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val doc = document ?: return@withContext false
        try {
            outputFile.parentFile?.mkdirs()
            doc.save(outputFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF to ${outputFile.name}", e)
            false
        }
    }

    /**
     * Closes the document and frees memory.
     */
    override fun close() {
        try {
            document?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing PDDocument", e)
        } finally {
            document = null
            renderer = null
            currentFile = null
        }
    }

    companion object {
        private const val TAG = "PdfManager"
    }
}
