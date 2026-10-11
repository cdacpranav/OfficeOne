package com.officone.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.officone.app.document.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * LOKit-based implementation of [DocumentEngine].
 *
 * This serves as the PRIMARY document engine for Office One, interfacing with LibreOfficeKit (LOKit)
 * via JNI native libraries for desktop-grade document rendering, pagination, and Uno command execution.
 *
 * Architecture & JNI Integration Points:
 * 1. Native Library Loading:
 *    - libreofficekit.so / libsofficeapp.so are initialized with Context and asset data.
 *    - In companion object [initialize], System.loadLibrary("lokit_jni") is invoked.
 *
 * 2. Document Loading:
 *    - Native call: `lok_document_load(lokInstance, filePath, options)`
 *    - Password-protected documents supply password parameter directly to LOKit load options.
 *
 * 3. Tiled Rendering:
 *    - Native call: `lok_document_paintTile(lokDoc, buffer, canvasWidth, canvasHeight, tilePosX, tilePosY, tileWidth, tileHeight)`
 *    - Direct rendering to a native DirectByteBuffer linked to an Android [Bitmap].
 *
 * 4. UNO Command Dispatch & Editing:
 *    - Native call: `lok_document_postUnoCommand(lokDoc, commandName, argumentsJson, notifyListener)`
 *    - Text insertion: `lok_document_postKeyEvent()` or Uno command `.uno:InsertText`
 *    - Formatting: UNO commands such as `.uno:Bold`, `.uno:Italic`, `.uno:Underline`, `.uno:FontHeight`
 *    - Undo / Redo: `.uno:Undo`, `.uno:Redo`
 *
 * Currently structured for Phase 2 PoC with comprehensive stubs, state tracking, and clear JNI hooks.
 */
class LOKitEngine(private val context: Context) : DocumentEngine {

    private val idGenerator = AtomicLong(1000L)
    private val activeHandles = ConcurrentHashMap<Long, DocumentSession>()

    private data class DocumentSession(
        val handle: DocumentHandle,
        val file: File,
        val nativeDocPtr: Long = 0L, // Placeholder for LibreOfficeKitDocument* pointer
        var pageCount: Int = 1,
        var textContent: String = "",
        val undoStack: MutableList<String> = mutableListOf(),
        val redoStack: MutableList<String> = mutableListOf()
    )

    override suspend fun loadDocument(file: File, password: String?): DocumentHandle = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalArgumentException("Document file does not exist: ${file.absolutePath}")
        }

        val ext = file.extension
        val docType = DocumentType.fromExtension(ext)
        val handleId = idGenerator.incrementAndGet()

        Log.d(TAG, "LOKit loadDocument: id=$handleId, path=${file.absolutePath}, type=$docType")

        /*
         * TODO: [LOKit JNI Integration - Document Loading]
         * val nativeDocPtr = nativeLoadDocument(
         *     lokInstance = nativeLokInstance,
         *     url = file.toURI().toString(),
         *     password = password
         * )
         * if (nativeDocPtr == 0L) throw IOException("Failed to load document via LOKit: ${file.name}")
         * val pageCount = nativeGetParts(nativeDocPtr)
         */

        val initialContent = runCatching {
            if (file.length() < 1024 * 1024) file.readText() else "Document: ${file.name}"
        }.getOrDefault("Office One Document: ${file.name}")

        val handle = DocumentHandle(
            id = handleId,
            filePath = file.absolutePath,
            documentType = docType,
            isModified = false
        )

        val session = DocumentSession(
            handle = handle,
            file = file,
            nativeDocPtr = 0L,
            pageCount = 3, // PoC default pages
            textContent = initialContent
        )
        activeHandles[handleId] = session

        handle
    }

    override suspend fun saveDocument(
        handle: DocumentHandle,
        outputFile: File,
        format: SaveFormat?
    ): Boolean = withContext(Dispatchers.IO) {
        val session = activeHandles[handle.id] ?: run {
            Log.e(TAG, "Cannot save document: Handle ${handle.id} not found")
            return@withContext false
        }

        Log.d(TAG, "LOKit saveDocument: handle=${handle.id}, out=${outputFile.absolutePath}, format=$format")

        /*
         * TODO: [LOKit JNI Integration - Document Export]
         * val lokFormatFilter = when (format) {
         *     SaveFormat.PDF -> "writer_pdf_Export"
         *     SaveFormat.DOCX -> "MS Word 2007 XML"
         *     SaveFormat.ODT -> "writer8"
         *     SaveFormat.XLSX -> "Calc MS Excel 2007 XML"
         *     SaveFormat.PPTX -> "Impress MS PowerPoint 2007 XML"
         *     else -> null
         * }
         * val success = nativeSaveAs(session.nativeDocPtr, outputFile.absolutePath, lokFormatFilter)
         */

        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(session.textContent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save document stub", e)
            false
        }
    }

    override fun getPageCount(handle: DocumentHandle): Int {
        val session = activeHandles[handle.id]
        /*
         * TODO: [LOKit JNI Integration - Page/Part Count]
         * return nativeGetParts(session.nativeDocPtr)
         */
        return session?.pageCount ?: 1
    }

    override suspend fun renderPage(
        handle: DocumentHandle,
        page: Int,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.Default) {
        val session = activeHandles[handle.id] ?: return@withContext null

        /*
         * TODO: [LOKit JNI Integration - Tiled Rendering]
         * val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
         * nativePaintTile(
         *     docPtr = session.nativeDocPtr,
         *     targetBitmap = bitmap,
         *     canvasWidth = width,
         *     canvasHeight = height,
         *     tilePosX = 0,
         *     tilePosY = page * pageHeightTwips,
         *     tileWidth = pageWidthTwips,
         *     tileHeight = pageHeightTwips
         * )
         * return@withContext bitmap
         */

        // Placeholder PoC rendering showing document name and page number
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(100), height.coerceAtLeast(100), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
        }

        canvas.drawText("LOKit Engine Preview (PoC Stub)", 40f, 60f, paint)
        paint.textSize = 20f
        canvas.drawText("File: ${session.file.name}", 40f, 100f, paint)
        canvas.drawText("Type: ${session.handle.documentType.getDisplayName()}", 40f, 130f, paint)
        canvas.drawText("Page: ${page + 1} of ${session.pageCount}", 40f, 160f, paint)

        // Draw preview lines
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }
        for (i in 0 until 12) {
            val y = 220f + (i * 35f)
            if (y < height - 40) {
                canvas.drawLine(40f, y, (width - 40).toFloat(), y, linePaint)
            }
        }

        bitmap
    }

    override fun getDocumentType(handle: DocumentHandle): DocumentType {
        return activeHandles[handle.id]?.handle?.documentType ?: handle.documentType
    }

    override fun closeDocument(handle: DocumentHandle) {
        val session = activeHandles.remove(handle.id)
        if (session != null) {
            Log.d(TAG, "LOKit closeDocument: handle=${handle.id}")
            /*
             * TODO: [LOKit JNI Integration - Document Cleanup]
             * if (session.nativeDocPtr != 0L) {
             *     nativeDestroyDocument(session.nativeDocPtr)
             * }
             */
        }
    }

    override fun insertText(handle: DocumentHandle, text: String) {
        val session = activeHandles[handle.id] ?: return
        Log.d(TAG, "LOKit insertText: '${text.take(30)}'")
        /*
         * TODO: [LOKit JNI Integration - Insert Text via UNO]
         * val escaped = JSONObject.quote(text)
         * nativePostUnoCommand(session.nativeDocPtr, ".uno:InsertText", "{\"Text\":{\"type\":\"string\",\"value\":$escaped}}")
         */
        session.undoStack.add(session.textContent)
        session.textContent += text
    }

    override fun deleteText(handle: DocumentHandle, start: Int, end: Int) {
        val session = activeHandles[handle.id] ?: return
        Log.d(TAG, "LOKit deleteText: range $start to $end")
        /*
         * TODO: [LOKit JNI Integration - Delete Text / Key Event]
         * nativePostUnoCommand(session.nativeDocPtr, ".uno:Delete", null)
         */
        session.undoStack.add(session.textContent)
        if (start in 0..session.textContent.length && end >= start) {
            val validEnd = end.coerceAtMost(session.textContent.length)
            session.textContent = session.textContent.removeRange(start, validEnd)
        }
    }

    override fun applyFormatting(handle: DocumentHandle, format: TextFormat) {
        val session = activeHandles[handle.id] ?: return
        Log.d(TAG, "LOKit applyFormatting: $format")
        /*
         * TODO: [LOKit JNI Integration - Formatting via UNO Commands]
         * format.bold?.let { nativePostUnoCommand(session.nativeDocPtr, ".uno:Bold", null) }
         * format.italic?.let { nativePostUnoCommand(session.nativeDocPtr, ".uno:Italic", null) }
         * format.underline?.let { nativePostUnoCommand(session.nativeDocPtr, ".uno:Underline", null) }
         * format.strikethrough?.let { nativePostUnoCommand(session.nativeDocPtr, ".uno:Strikeout", null) }
         * format.fontSize?.let { size ->
         *     nativePostUnoCommand(session.nativeDocPtr, ".uno:FontHeight", "{\"FontHeight.Height\":{\"type\":\"float\",\"value\":$size}}")
         * }
         * format.fontFamily?.let { family ->
         *     nativePostUnoCommand(session.nativeDocPtr, ".uno:CharFontName", "{\"CharFontName.FamilyName\":{\"type\":\"string\",\"value\":\"$family\"}}")
         * }
         */
    }

    override fun undo(handle: DocumentHandle): Boolean {
        val session = activeHandles[handle.id] ?: return false
        Log.d(TAG, "LOKit undo")
        /*
         * TODO: [LOKit JNI Integration - Undo via UNO]
         * return nativePostUnoCommand(session.nativeDocPtr, ".uno:Undo", null)
         */
        if (session.undoStack.isNotEmpty()) {
            session.redoStack.add(session.textContent)
            session.textContent = session.undoStack.removeAt(session.undoStack.lastIndex)
            return true
        }
        return false
    }

    override fun redo(handle: DocumentHandle): Boolean {
        val session = activeHandles[handle.id] ?: return false
        Log.d(TAG, "LOKit redo")
        /*
         * TODO: [LOKit JNI Integration - Redo via UNO]
         * return nativePostUnoCommand(session.nativeDocPtr, ".uno:Redo", null)
         */
        if (session.redoStack.isNotEmpty()) {
            session.undoStack.add(session.textContent)
            session.textContent = session.redoStack.removeAt(session.redoStack.lastIndex)
            return true
        }
        return false
    }

    override fun getWordCount(handle: DocumentHandle): Int {
        val session = activeHandles[handle.id] ?: return 0
        return session.textContent.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    override fun getCharacterCount(handle: DocumentHandle): Int {
        val session = activeHandles[handle.id] ?: return 0
        return session.textContent.length
    }

    override fun searchText(handle: DocumentHandle, query: String): List<SearchResult> {
        val session = activeHandles[handle.id] ?: return emptyList()
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchResult>()
        var index = session.textContent.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            val snippetStart = (index - 20).coerceAtLeast(0)
            val snippetEnd = (index + query.length + 20).coerceAtMost(session.textContent.length)
            val snippet = session.textContent.substring(snippetStart, snippetEnd).replace('\n', ' ')

            results.add(
                SearchResult(
                    page = 0,
                    startOffset = index,
                    endOffset = index + query.length,
                    snippet = "...$snippet..."
                )
            )
            index = session.textContent.indexOf(query, index + query.length, ignoreCase = true)
        }
        return results
    }

    companion object {
        private const val TAG = "LOKitEngine"
        private var isNativeLoaded = false

        /**
         * Checks if the LibreOfficeKit native shared libraries are loaded and ready.
         */
        @JvmStatic
        fun isAvailable(): Boolean {
            return isNativeLoaded
        }

        /**
         * Initializes LibreOfficeKit native libraries and environment.
         */
        @JvmStatic
        @Synchronized
        fun initialize(context: Context) {
            if (isNativeLoaded) return

            try {
                // In production, libraries are unpacked to app private storage or bundled in apk lib/
                // e.g.:
                // System.loadLibrary("sofficeapp")
                // System.loadLibrary("lokit_jni")
                // nativeInitLokit(context.applicationInfo.nativeLibraryDir, context.cacheDir.absolutePath)
                Log.i(TAG, "Checking LOKit native library availability...")
                // For Phase 2 PoC stub, keep isNativeLoaded false by default unless libraries exist
                isNativeLoaded = false
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "LOKit native libraries not loaded (will fallback to POI): ${e.message}")
                isNativeLoaded = false
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error initializing LOKit", e)
                isNativeLoaded = false
            }
        }
    }
}
