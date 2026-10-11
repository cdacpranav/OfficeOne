package com.officone.app.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.officone.app.pdf.PdfAnnotation
import com.officone.app.pdf.PdfManager
import com.officone.app.pdf.PdfSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class PdfState {
    data object Idle : PdfState()
    data object Loading : PdfState()
    data class Loaded(val fileName: String, val totalPages: Int) : PdfState()
    data class Error(val message: String) : PdfState()
}

enum class AnnotationMode {
    NONE,
    HIGHLIGHT,
    UNDERLINE,
    NOTE,
    DRAW
}

class PdfViewerViewModel : ViewModel() {

    private val pdfManager = PdfManager()

    private val _pdfState = MutableStateFlow<PdfState>(PdfState.Idle)
    val pdfState: StateFlow<PdfState> = _pdfState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PdfSearchResult>>(emptyList())
    val searchResults: StateFlow<List<PdfSearchResult>> = _searchResults.asStateFlow()

    private val _annotationMode = MutableStateFlow(AnnotationMode.NONE)
    val annotationMode: StateFlow<AnnotationMode> = _annotationMode.asStateFlow()

    // Cache of rendered bitmaps for loaded pages
    private val _renderedPages = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val renderedPages: StateFlow<Map<Int, Bitmap>> = _renderedPages.asStateFlow()

    private var currentFile: File? = null

    fun loadPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _pdfState.value = PdfState.Loading
            try {
                val tempFile = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "temp_view_${System.currentTimeMillis()}.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file
                }

                currentFile = tempFile
                pdfManager.openPdf(tempFile)
                val count = pdfManager.getPageCount()

                _totalPages.value = count
                _currentPage.value = 0
                _pdfState.value = PdfState.Loaded(
                    fileName = uri.lastPathSegment ?: "document.pdf",
                    totalPages = count
                )

                // Pre-render the first few pages
                renderPages(0 until minOf(count, 3))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load PDF from uri: $uri", e)
                _pdfState.value = PdfState.Error(e.localizedMessage ?: "Failed to load PDF document")
            }
        }
    }

    fun loadPdfFile(file: File) {
        viewModelScope.launch {
            _pdfState.value = PdfState.Loading
            try {
                currentFile = file
                pdfManager.openPdf(file)
                val count = pdfManager.getPageCount()

                _totalPages.value = count
                _currentPage.value = 0
                _pdfState.value = PdfState.Loaded(
                    fileName = file.name,
                    totalPages = count
                )

                renderPages(0 until minOf(count, 3))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load PDF from file: ${file.name}", e)
                _pdfState.value = PdfState.Error(e.localizedMessage ?: "Failed to open PDF")
            }
        }
    }

    fun goToPage(page: Int) {
        val total = _totalPages.value
        if (page in 0 until total) {
            _currentPage.value = page
            renderPages((page - 1).coerceAtLeast(0)..(page + 1).coerceAtMost(total - 1))
        }
    }

    fun setZoom(level: Float) {
        _zoomLevel.value = level.coerceIn(0.5f, 4.0f)
    }

    fun zoomIn() {
        setZoom(_zoomLevel.value + 0.25f)
    }

    fun zoomOut() {
        setZoom(_zoomLevel.value - 0.25f)
    }

    fun setAnnotationMode(mode: AnnotationMode) {
        _annotationMode.value = mode
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            val results = pdfManager.searchText(query)
            _searchResults.value = results
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun addHighlight(page: Int, rect: RectF) {
        viewModelScope.launch {
            pdfManager.addAnnotation(page, PdfAnnotation.Highlight(rect))
            renderPageDirectly(page)
        }
    }

    fun addNote(page: Int, position: PointF, text: String) {
        viewModelScope.launch {
            pdfManager.addAnnotation(page, PdfAnnotation.Note(position, text))
            renderPageDirectly(page)
        }
    }

    fun savePdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val tempOut = File(context.cacheDir, "saved_temp.pdf")
                if (pdfManager.savePdf(tempOut)) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        tempOut.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    tempOut.delete()
                }
            }
        }
    }

    fun renderPages(range: IntRange) {
        viewModelScope.launch {
            for (p in range) {
                if (!_renderedPages.value.containsKey(p)) {
                    renderPageDirectly(p)
                }
            }
        }
    }

    private suspend fun renderPageDirectly(pageIndex: Int) {
        val bitmap = pdfManager.renderPage(pageIndex, 1080, 1920)
        if (bitmap != null) {
            _renderedPages.value = _renderedPages.value + (pageIndex to bitmap)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfManager.close()
    }

    companion object {
        private const val TAG = "PdfViewerViewModel"
    }
}
