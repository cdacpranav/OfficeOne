package com.officone.app.ui.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.officone.app.OfficeOneApplication
import com.officone.app.document.DocumentInfo
import com.officone.app.engine.DocumentEngineFactory
import com.officone.app.engine.FormatAction
import com.officone.app.engine.TextAlignment
import com.officone.app.engine.TextFormat
import com.officone.app.fileio.SAFFileHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentEditorViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val safFileHandler = SAFFileHandler()

    private val _documentInfo = MutableStateFlow<DocumentInfo?>(null)
    val documentInfo: StateFlow<DocumentInfo?> = _documentInfo.asStateFlow()

    private val _documentContent = MutableStateFlow("")
    val documentContent: StateFlow<String> = _documentContent.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _currentFormat = MutableStateFlow(
        TextFormat(
            bold = false,
            italic = false,
            underline = false,
            strikethrough = false,
            fontSize = 14f,
            alignment = TextAlignment.LEFT
        )
    )
    val currentFormat: StateFlow<TextFormat> = _currentFormat.asStateFlow()

    private val _wordCount = MutableStateFlow(0)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _characterCount = MutableStateFlow(0)
    val characterCount: StateFlow<Int> = _characterCount.asStateFlow()

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    private var localCachedFile: File? = null

    fun loadDocument(context: Context, uri: Uri, mimeType: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val (info, content) = withContext(Dispatchers.IO) {
                    safFileHandler.takePersistablePermission(context, uri)
                    val doc = safFileHandler.openDocument(context, uri)
                    val cached = safFileHandler.copyToCache(context, uri)
                    localCachedFile = cached

                    val engine = DocumentEngineFactory.getEngine(context)
                    val loadedContent = try {
                        val handle = engine.loadDocument(cached)
                        val wordC = engine.getWordCount(handle)
                        val charC = engine.getCharacterCount(handle)
                        _wordCount.value = wordC
                        _characterCount.value = charC
                        _totalPages.value = engine.getPageCount(handle)

                        // If file is plain text, markdown, or POI parsed text:
                        if (cached.extension.lowercase() in listOf("txt", "md", "csv", "log")) {
                            cached.readText()
                        } else {
                            // Extract initial text representation
                            val streamText = cached.bufferedReader().useLines { lines ->
                                lines.take(500).joinToString("\n")
                            }
                            if (streamText.contains("Office One Document Report") || streamText.isNotBlank()) {
                                streamText
                            } else {
                                "Document: ${doc.displayName}\n\nStart typing your document content here..."
                            }
                        }
                    } catch (e: Exception) {
                        "Document: ${doc.displayName}\n\nStart typing here..."
                    }
                    doc to loadedContent
                }

                _documentInfo.value = info
                _documentContent.value = content
                updateCounts(content)
                _currentPage.value = 1
                _isModified.value = false

                // Record in recent files
                val homeVm = (context.applicationContext as OfficeOneApplication).database.recentFileDao()
                val recent = com.officone.app.data.RecentFile(
                    uriString = info.uri.toString(),
                    displayName = info.displayName,
                    mimeType = info.mimeType,
                    documentType = info.documentType.name,
                    lastOpenedTimestamp = System.currentTimeMillis(),
                    fileSize = info.size
                )
                homeVm.upsertByUri(recent)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load document: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateContent(newContent: String) {
        if (_documentContent.value != newContent) {
            undoStack.add(_documentContent.value)
            redoStack.clear()
            _documentContent.value = newContent
            updateCounts(newContent)
            _isModified.value = true
        }
    }

    private fun updateCounts(text: String) {
        _characterCount.value = text.length
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        _wordCount.value = words.size
        _totalPages.value = ((words.size / 300) + 1).coerceAtLeast(1)
    }

    fun applyFormatAction(action: FormatAction) {
        val current = _currentFormat.value
        when (action) {
            is FormatAction.ToggleBold -> {
                _currentFormat.value = current.copy(bold = !(current.bold ?: false))
            }
            is FormatAction.ToggleItalic -> {
                _currentFormat.value = current.copy(italic = !(current.italic ?: false))
            }
            is FormatAction.ToggleUnderline -> {
                _currentFormat.value = current.copy(underline = !(current.underline ?: false))
            }
            is FormatAction.ToggleStrikethrough -> {
                _currentFormat.value = current.copy(strikethrough = !(current.strikethrough ?: false))
            }
            is FormatAction.SetFontSize -> {
                _currentFormat.value = current.copy(fontSize = action.size)
            }
            is FormatAction.SetFontFamily -> {
                _currentFormat.value = current.copy(fontFamily = action.family)
            }
            is FormatAction.SetAlignment -> {
                _currentFormat.value = current.copy(alignment = action.alignment)
            }
            is FormatAction.SetFontColor -> {
                _currentFormat.value = current.copy(fontColor = action.color)
            }
            is FormatAction.SetHighlightColor -> {
                _currentFormat.value = current.copy(highlightColor = action.color)
            }
            is FormatAction.InsertTable -> {
                val tableTemplate = "\n\n| Column 1 | Column 2 | Column 3 |\n| :--- | :--- | :--- |\n| Data 1 | Data 2 | Data 3 |\n\n"
                updateContent(_documentContent.value + tableTemplate)
            }
            is FormatAction.InsertPageBreak -> {
                val pageBreak = "\n\n--- Page Break ---\n\n"
                updateContent(_documentContent.value + pageBreak)
            }
            is FormatAction.Undo -> undo()
            is FormatAction.Redo -> redo()
            else -> {}
        }
        _isModified.value = true
    }

    fun applyRibbonFormatAction(action: com.officone.app.ui.ribbon.FormatAction) {
        val current = _currentFormat.value
        when (action) {
            is com.officone.app.ui.ribbon.FormatAction.Bold -> _currentFormat.value = current.copy(bold = action.isBold)
            is com.officone.app.ui.ribbon.FormatAction.Italic -> _currentFormat.value = current.copy(italic = action.isItalic)
            is com.officone.app.ui.ribbon.FormatAction.Underline -> _currentFormat.value = current.copy(underline = action.isUnderline)
            is com.officone.app.ui.ribbon.FormatAction.Strikethrough -> _currentFormat.value = current.copy(strikethrough = action.isStrikethrough)
            is com.officone.app.ui.ribbon.FormatAction.FontSize -> _currentFormat.value = current.copy(fontSize = action.size.toFloat())
            is com.officone.app.ui.ribbon.FormatAction.FontFamily -> _currentFormat.value = current.copy(fontFamily = action.font)
            is com.officone.app.ui.ribbon.FormatAction.AlignLeft -> _currentFormat.value = current.copy(alignment = TextAlignment.LEFT)
            is com.officone.app.ui.ribbon.FormatAction.AlignCenter -> _currentFormat.value = current.copy(alignment = TextAlignment.CENTER)
            is com.officone.app.ui.ribbon.FormatAction.AlignRight -> _currentFormat.value = current.copy(alignment = TextAlignment.RIGHT)
            is com.officone.app.ui.ribbon.FormatAction.AlignJustify -> _currentFormat.value = current.copy(alignment = TextAlignment.JUSTIFY)
            is com.officone.app.ui.ribbon.FormatAction.Bullets -> updateContent(_documentContent.value + "\n• ")
            is com.officone.app.ui.ribbon.FormatAction.Numbering -> updateContent(_documentContent.value + "\n1. ")
            is com.officone.app.ui.ribbon.FormatAction.Undo -> undo()
            is com.officone.app.ui.ribbon.FormatAction.Redo -> redo()
            else -> {}
        }
        _isModified.value = true
    }

    fun applyRibbonInsertAction(action: com.officone.app.ui.ribbon.InsertAction) {
        when (action) {
            is com.officone.app.ui.ribbon.InsertAction.InsertTable -> {
                val tableTemplate = "\n\n| Column 1 | Column 2 | Column 3 |\n| :--- | :--- | :--- |\n| Data 1 | Data 2 | Data 3 |\n\n"
                updateContent(_documentContent.value + tableTemplate)
            }
            is com.officone.app.ui.ribbon.InsertAction.PageBreak -> {
                val pageBreak = "\n\n--- Page Break ---\n\n"
                updateContent(_documentContent.value + pageBreak)
            }
            is com.officone.app.ui.ribbon.InsertAction.InsertDateTime -> {
                val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                updateContent(_documentContent.value + " $now ")
            }
            is com.officone.app.ui.ribbon.InsertAction.InsertTextBox -> {
                updateContent(_documentContent.value + "\n> [Text Box / Note]\n")
            }
            else -> {}
        }
        _isModified.value = true
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_documentContent.value)
            _documentContent.value = previous
            updateCounts(previous)
            _isModified.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_documentContent.value)
            _documentContent.value = next
            updateCounts(next)
            _isModified.value = true
        }
    }

    fun saveDocument(context: Context) {
        val info = _documentInfo.value ?: return
        val cache = localCachedFile ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    // Flush current text content to local cache file
                    FileOutputStream(cache).use { fos ->
                        fos.write(_documentContent.value.toByteArray(Charsets.UTF_8))
                    }
                    safFileHandler.saveToUri(context, info.uri, cache)
                }
                if (success) {
                    _isModified.value = false
                } else {
                    _errorMessage.value = "Failed to save file changes to storage"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error while saving: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveAsDocument(context: Context, newUri: Uri) {
        val cache = localCachedFile ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    FileOutputStream(cache).use { fos ->
                        fos.write(_documentContent.value.toByteArray(Charsets.UTF_8))
                    }
                    safFileHandler.takePersistablePermission(context, newUri)
                    safFileHandler.saveToUri(context, newUri, cache)
                }
                if (success) {
                    val newInfo = safFileHandler.openDocument(context, newUri)
                    _documentInfo.value = newInfo
                    _isModified.value = false
                } else {
                    _errorMessage.value = "Failed to save document to new location"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error during Save As: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDisplayName(newName: String) {
        _documentInfo.value = _documentInfo.value?.copy(displayName = newName)
        _isModified.value = true
    }

    fun markModified() {
        _isModified.value = true
    }

    fun setZoom(level: Float) {
        _zoomLevel.value = level.coerceIn(0.25f, 5.0f)
    }

    fun zoomIn() {
        setZoom(_zoomLevel.value + 0.15f)
    }

    fun zoomOut() {
        setZoom(_zoomLevel.value - 0.15f)
    }

    fun setCurrentPage(page: Int) {
        val total = _totalPages.value
        _currentPage.value = page.coerceIn(1, total.coerceAtLeast(1))
    }

    fun setTotalPages(pages: Int) {
        _totalPages.value = pages.coerceAtLeast(1)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
