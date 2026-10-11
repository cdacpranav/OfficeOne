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

class SpreadsheetEditorViewModel(
    application: Application,
    private val safFileHandler: SAFFileHandler = SAFFileHandler()
) : AndroidViewModel(application) {

    private val _documentInfo = MutableStateFlow<DocumentInfo?>(null)
    val documentInfo: StateFlow<DocumentInfo?> = _documentInfo.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCell = MutableStateFlow("A1")
    val selectedCell: StateFlow<String> = _selectedCell.asStateFlow()

    private val _formulaValue = MutableStateFlow("")
    val formulaValue: StateFlow<String> = _formulaValue.asStateFlow()

    private val _sheetNames = MutableStateFlow(listOf("Sheet1"))
    val sheetNames: StateFlow<List<String>> = _sheetNames.asStateFlow()

    private val _activeSheetIndex = MutableStateFlow(0)
    val activeSheetIndex: StateFlow<Int> = _activeSheetIndex.asStateFlow()

    private val _cellData = MutableStateFlow<Map<String, String>>(emptyMap())
    val cellData: StateFlow<Map<String, String>> = _cellData.asStateFlow()

    private val _evaluatedCellData = MutableStateFlow<Map<String, String>>(emptyMap())
    val evaluatedCellData: StateFlow<Map<String, String>> = _evaluatedCellData.asStateFlow()

    private var localCachedFile: File? = null

    fun loadSpreadsheet(context: Context, uri: Uri, mimeType: String? = null) {
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

                // Initialize or recalculate initial cell grid
                _evaluatedCellData.value = com.officone.app.engine.SpreadsheetFormulaEngine.recalculateAll(_cellData.value)

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
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCell(cellRef: String) {
        _selectedCell.value = cellRef
        _formulaValue.value = _cellData.value[cellRef] ?: ""
    }

    fun updateFormulaValue(value: String) {
        _formulaValue.value = value
        val current = _cellData.value.toMutableMap()
        current[_selectedCell.value] = value
        _cellData.value = current
        _evaluatedCellData.value = com.officone.app.engine.SpreadsheetFormulaEngine.recalculateAll(current)
        _isModified.value = true
    }

    fun addSheet() {
        val currentSheets = _sheetNames.value.toMutableList()
        val newSheetName = "Sheet${currentSheets.size + 1}"
        currentSheets.add(newSheetName)
        _sheetNames.value = currentSheets
        _activeSheetIndex.value = currentSheets.size - 1
        _isModified.value = true
    }

    fun setActiveSheet(index: Int) {
        if (index in _sheetNames.value.indices) {
            _activeSheetIndex.value = index
        }
    }

    fun saveSpreadsheet(context: Context) {
        val info = _documentInfo.value ?: return
        val cache = localCachedFile ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    safFileHandler.saveToUri(context, info.uri, cache)
                }
                _isModified.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
