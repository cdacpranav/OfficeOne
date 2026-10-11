package com.officone.app.ui.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.officone.app.OfficeOneApplication
import com.officone.app.document.DocumentInfo
import com.officone.app.fileio.SAFFileHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SlideData(
    val id: Int,
    val title: String,
    val body: String = "Tap to add presentation points or subtitle...",
    val notes: String = ""
)

class PresentationEditorViewModel(
    application: Application,
    private val safFileHandler: SAFFileHandler = SAFFileHandler()
) : AndroidViewModel(application) {

    private val _documentInfo = MutableStateFlow<DocumentInfo?>(null)
    val documentInfo: StateFlow<DocumentInfo?> = _documentInfo.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _slides = MutableStateFlow(
        listOf(
            SlideData(1, "Office One Presentation", "Complete offline mobile office suite\n• Zero permissions\n• Privacy-first design\n• All-in-one productivity"),
            SlideData(2, "Architecture & Engine", "• LibreOfficeKit desktop core\n• Apache POI fallback\n• PdfBox-Android rendering\n• Jetpack Compose Ribbon UI"),
            SlideData(3, "Summary & Next Steps", "• Native Storage Access Framework\n• 100% offline document security")
        )
    )
    val slides: StateFlow<List<SlideData>> = _slides.asStateFlow()

    private val _currentSlideIndex = MutableStateFlow(0)
    val currentSlideIndex: StateFlow<Int> = _currentSlideIndex.asStateFlow()

    private val _isNotesExpanded = MutableStateFlow(false)
    val isNotesExpanded: StateFlow<Boolean> = _isNotesExpanded.asStateFlow()

    private val _isPlayingSlideshow = MutableStateFlow(false)
    val isPlayingSlideshow: StateFlow<Boolean> = _isPlayingSlideshow.asStateFlow()

    private var localCachedFile: File? = null

    fun loadPresentation(context: Context, uri: Uri, mimeType: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = withContext(Dispatchers.IO) {
                    safFileHandler.takePersistablePermission(context, uri)
                    val doc = safFileHandler.openDocument(context, uri)
                    localCachedFile = safFileHandler.copyToCache(context, uri)
                    doc
                }
                _documentInfo.value = info
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectSlide(index: Int) {
        if (index in _slides.value.indices) {
            _currentSlideIndex.value = index
        }
    }

    fun addSlide() {
        val current = _slides.value.toMutableList()
        val newIndex = current.size + 1
        current.add(SlideData(newIndex, "Slide $newIndex - New Slide", "Add bullet points or description here..."))
        _slides.value = current
        _currentSlideIndex.value = current.size - 1
        _isModified.value = true
    }

    fun deleteSlide(index: Int) {
        val current = _slides.value.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _slides.value = current
            _currentSlideIndex.value = index.coerceAtMost(current.size - 1)
            _isModified.value = true
        }
    }

    fun updateSlideTitle(title: String) {
        val index = _currentSlideIndex.value
        val current = _slides.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(title = title)
            _slides.value = current
            _isModified.value = true
        }
    }

    fun updateSlideBody(body: String) {
        val index = _currentSlideIndex.value
        val current = _slides.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(body = body)
            _slides.value = current
            _isModified.value = true
        }
    }

    fun updateSlideNotes(notes: String) {
        val index = _currentSlideIndex.value
        val current = _slides.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(notes = notes)
            _slides.value = current
            _isModified.value = true
        }
    }

    fun toggleNotes() {
        _isNotesExpanded.value = !_isNotesExpanded.value
    }

    fun toggleSlideshow(playing: Boolean) {
        _isPlayingSlideshow.value = playing
    }

    fun savePresentation(context: Context) {
        val info = _documentInfo.value ?: return
        val cache = localCachedFile ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val summary = buildString {
                        _slides.value.forEachIndexed { idx, s ->
                            appendLine("--- Slide ${idx + 1}: ${s.title} ---")
                            appendLine(s.body)
                            if (s.notes.isNotBlank()) appendLine("[Notes]: ${s.notes}")
                            appendLine()
                        }
                    }
                    FileOutputStream(cache).use { fos ->
                        fos.write(summary.toByteArray(Charsets.UTF_8))
                    }
                    safFileHandler.saveToUri(context, info.uri, cache)
                }
                _isModified.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
