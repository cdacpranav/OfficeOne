package com.officone.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.officone.app.document.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Fallback document engine using Apache POI.
 *
 * Used when LOKit native libraries are unavailable.
 * Supports:
 * - DOCX reading via [XWPFDocument]
 * - XLSX reading via [XSSFWorkbook]
 * - PPTX reading via [XMLSlideShow]
 * - Text/RTF/CSV reading via standard I/O
 *
 * Renders document previews to [Bitmap] with graceful error handling and resource cleanup.
 */
class FallbackEngine(private val context: Context) : DocumentEngine {

    private val idGenerator = AtomicLong(5000L)
    private val activeDocuments = ConcurrentHashMap<Long, ParsedDocumentData>()

    private data class ParsedDocumentData(
        val handle: DocumentHandle,
        val file: File,
        val pagesText: List<String>,
        var fullText: String,
        val undoHistory: MutableList<String> = mutableListOf(),
        val redoHistory: MutableList<String> = mutableListOf()
    )

    override suspend fun loadDocument(file: File, password: String?): DocumentHandle = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalArgumentException("Document file does not exist: ${file.absolutePath}")
        }

        val ext = file.extension.lowercase()
        val docType = DocumentType.fromExtension(ext)
        val handleId = idGenerator.incrementAndGet()

        Log.d(TAG, "FallbackEngine loading file: ${file.name} as $docType")

        val (pages, fullText) = try {
            when (ext) {
                "docx" -> parseDocx(file)
                "xlsx" -> parseXlsx(file)
                "pptx" -> parsePptx(file)
                else -> parsePlainText(file)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error parsing file with POI: ${file.name}", e)
            val fallbackText = "Document: ${file.name}\n(Preview could not parse formatting: ${e.localizedMessage ?: "Unknown error"})"
            listOf(fallbackText) to fallbackText
        }

        val handle = DocumentHandle(
            id = handleId,
            filePath = file.absolutePath,
            documentType = docType,
            isModified = false
        )

        val parsedData = ParsedDocumentData(
            handle = handle,
            file = file,
            pagesText = if (pages.isEmpty()) listOf(fullText) else pages,
            fullText = fullText
        )

        activeDocuments[handleId] = parsedData
        handle
    }

    private fun parseDocx(file: File): Pair<List<String>, String> {
        val pages = mutableListOf<String>()
        val fullBuilder = StringBuilder()

        FileInputStream(file).use { fis ->
            XWPFDocument(fis).use { document ->
                var currentPageBuilder = StringBuilder()
                var paragraphCount = 0

                for (paragraph in document.paragraphs) {
                    val text = paragraph.text
                    if (text.isNotBlank()) {
                        currentPageBuilder.append(text).append("\n\n")
                        fullBuilder.append(text).append("\n\n")
                        paragraphCount++

                        // Approximate page break every ~15 paragraphs
                        if (paragraphCount >= 15) {
                            pages.add(currentPageBuilder.toString().trim())
                            currentPageBuilder = StringBuilder()
                            paragraphCount = 0
                        }
                    }
                }

                // Also parse tables
                for (table in document.tables) {
                    val tableBuilder = StringBuilder()
                    for (row in table.rows) {
                        val rowText = row.tableCells.joinToString(separator = " | ") { it.text.trim() }
                        tableBuilder.append("| ").append(rowText).append(" |\n")
                    }
                    if (tableBuilder.isNotBlank()) {
                        currentPageBuilder.append(tableBuilder).append("\n")
                        fullBuilder.append(tableBuilder).append("\n")
                    }
                }

                if (currentPageBuilder.isNotBlank()) {
                    pages.add(currentPageBuilder.toString().trim())
                }
            }
        }

        return pages to fullBuilder.toString().trim()
    }

    private fun parseXlsx(file: File): Pair<List<String>, String> {
        val sheetsText = mutableListOf<String>()
        val fullBuilder = StringBuilder()

        FileInputStream(file).use { fis ->
            XSSFWorkbook(fis).use { workbook ->
                val numSheets = workbook.numberOfSheets
                for (i in 0 until numSheets) {
                    val sheet = workbook.getSheetAt(i)
                    val sheetBuilder = StringBuilder()
                    sheetBuilder.append("=== Sheet: ").append(sheet.sheetName).append(" ===\n\n")

                    var rowLimit = 0
                    for (row in sheet) {
                        if (rowLimit++ > 150) {
                            sheetBuilder.append("... [truncated additional rows]\n")
                            break
                        }
                        val cells = mutableListOf<String>()
                        for (cell in row) {
                            cells.add(cell.toString().trim())
                        }
                        sheetBuilder.append(cells.joinToString(separator = " \t| ")).append("\n")
                    }

                    val sheetContent = sheetBuilder.toString().trim()
                    sheetsText.add(sheetContent)
                    fullBuilder.append(sheetContent).append("\n\n")
                }
            }
        }

        return sheetsText to fullBuilder.toString().trim()
    }

    private fun parsePptx(file: File): Pair<List<String>, String> {
        val slidesText = mutableListOf<String>()
        val fullBuilder = StringBuilder()

        FileInputStream(file).use { fis ->
            XMLSlideShow(fis).use { presentation ->
                var slideIndex = 1
                for (slide in presentation.slides) {
                    val slideBuilder = StringBuilder()
                    slideBuilder.append("--- Slide ").append(slideIndex++).append(" ---\n\n")

                    for (shape in slide.shapes) {
                        if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                            val text = shape.text
                            if (text.isNotBlank()) {
                                slideBuilder.append(text).append("\n")
                            }
                        }
                    }

                    val slideContent = slideBuilder.toString().trim()
                    slidesText.add(slideContent)
                    fullBuilder.append(slideContent).append("\n\n")
                }
            }
        }

        return slidesText to fullBuilder.toString().trim()
    }

    private fun parsePlainText(file: File): Pair<List<String>, String> {
        val text = file.readText()
        val lines = text.lines()
        val pages = mutableListOf<String>()
        val chunkSize = 40

        for (i in lines.indices step chunkSize) {
            val chunk = lines.subList(i, (i + chunkSize).coerceAtMost(lines.size))
            pages.add(chunk.joinToString("\n"))
        }

        return pages to text
    }

    override suspend fun saveDocument(
        handle: DocumentHandle,
        outputFile: File,
        format: SaveFormat?
    ): Boolean = withContext(Dispatchers.IO) {
        val docData = activeDocuments[handle.id] ?: return@withContext false
        try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                fos.write(docData.fullText.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document in FallbackEngine", e)
            false
        }
    }

    override fun getPageCount(handle: DocumentHandle): Int {
        val docData = activeDocuments[handle.id] ?: return 1
        return docData.pagesText.size.coerceAtLeast(1)
    }

    override suspend fun renderPage(
        handle: DocumentHandle,
        page: Int,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.Default) {
        val docData = activeDocuments[handle.id] ?: return@withContext null
        val targetPage = page.coerceIn(0, (docData.pagesText.size - 1).coerceAtLeast(0))
        val pageText = if (docData.pagesText.isNotEmpty()) docData.pagesText[targetPage] else docData.fullText

        val bitmapWidth = width.coerceAtLeast(300)
        val bitmapHeight = height.coerceAtLeast(400)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw clean page background
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(40, 70, 140)
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val metaPaint = Paint().apply {
            color = Color.GRAY
            textSize = 18f
            isAntiAlias = true
        }

        canvas.drawText(docData.file.name, 40f, 50f, headerPaint)
        canvas.drawText("Page ${targetPage + 1} of ${docData.pagesText.size} • Fallback Engine", 40f, 80f, metaPaint)

        // Draw separator
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }
        canvas.drawLine(40f, 100f, (bitmapWidth - 40).toFloat(), 100f, dividerPaint)

        // Render formatted text body using StaticLayout
        val bodyPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 22f
            isAntiAlias = true
        }

        val textWidth = (bitmapWidth - 80).coerceAtLeast(100)
        val staticLayout = StaticLayout.Builder.obtain(
            pageText,
            0,
            pageText.length,
            bodyPaint,
            textWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.15f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(40f, 120f)
        staticLayout.draw(canvas)
        canvas.restore()

        bitmap
    }

    override fun getDocumentType(handle: DocumentHandle): DocumentType {
        return activeDocuments[handle.id]?.handle?.documentType ?: handle.documentType
    }

    override fun closeDocument(handle: DocumentHandle) {
        activeDocuments.remove(handle.id)
    }

    override fun insertText(handle: DocumentHandle, text: String) {
        val docData = activeDocuments[handle.id] ?: return
        docData.undoHistory.add(docData.fullText)
        docData.fullText += text
    }

    override fun deleteText(handle: DocumentHandle, start: Int, end: Int) {
        val docData = activeDocuments[handle.id] ?: return
        docData.undoHistory.add(docData.fullText)
        if (start in 0..docData.fullText.length && end >= start) {
            val validEnd = end.coerceAtMost(docData.fullText.length)
            docData.fullText = docData.fullText.removeRange(start, validEnd)
        }
    }

    override fun applyFormatting(handle: DocumentHandle, format: TextFormat) {
        // Fallback engine is primarily read/preview; formatting recorded for metadata
        Log.d(TAG, "FallbackEngine applyFormatting: $format")
    }

    override fun undo(handle: DocumentHandle): Boolean {
        val docData = activeDocuments[handle.id] ?: return false
        if (docData.undoHistory.isNotEmpty()) {
            docData.redoHistory.add(docData.fullText)
            docData.fullText = docData.undoHistory.removeAt(docData.undoHistory.lastIndex)
            return true
        }
        return false
    }

    override fun redo(handle: DocumentHandle): Boolean {
        val docData = activeDocuments[handle.id] ?: return false
        if (docData.redoHistory.isNotEmpty()) {
            docData.undoHistory.add(docData.fullText)
            docData.fullText = docData.redoHistory.removeAt(docData.redoHistory.lastIndex)
            return true
        }
        return false
    }

    override fun getWordCount(handle: DocumentHandle): Int {
        val docData = activeDocuments[handle.id] ?: return 0
        return docData.fullText.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    override fun getCharacterCount(handle: DocumentHandle): Int {
        val docData = activeDocuments[handle.id] ?: return 0
        return docData.fullText.length
    }

    override fun searchText(handle: DocumentHandle, query: String): List<SearchResult> {
        val docData = activeDocuments[handle.id] ?: return emptyList()
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchResult>()
        docData.pagesText.forEachIndexed { pageIndex, pageContent ->
            var idx = pageContent.indexOf(query, ignoreCase = true)
            while (idx >= 0) {
                val start = (idx - 25).coerceAtLeast(0)
                val end = (idx + query.length + 25).coerceAtMost(pageContent.length)
                val snippet = pageContent.substring(start, end).replace('\n', ' ')

                results.add(
                    SearchResult(
                        page = pageIndex,
                        startOffset = idx,
                        endOffset = idx + query.length,
                        snippet = "...$snippet..."
                    )
                )
                idx = pageContent.indexOf(query, idx + query.length, ignoreCase = true)
            }
        }
        return results
    }

    companion object {
        private const val TAG = "FallbackEngine"
    }
}
