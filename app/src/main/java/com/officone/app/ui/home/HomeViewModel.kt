package com.officone.app.ui.home

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.officone.app.data.OfficeOneDatabase
import com.officone.app.data.RecentFile
import com.officone.app.data.RecentFileDao
import com.officone.app.document.DocumentInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val recentFileDao: RecentFileDao? by lazy {
        try {
            OfficeOneDatabase.getInstance(application).recentFileDao()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to get RecentFileDao", e)
            null
        }
    }

    val recentFiles: StateFlow<List<RecentFile>> = (recentFileDao?.getRecentFiles() ?: flowOf(emptyList()))
        .catch { e ->
            Log.e(TAG, "Error collecting recent files", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRecentFile(documentInfo: DocumentInfo) {
        viewModelScope.launch {
            try {
                val recent = RecentFile(
                    uriString = documentInfo.uri.toString(),
                    displayName = documentInfo.displayName,
                    mimeType = documentInfo.mimeType,
                    documentType = documentInfo.documentType.name,
                    lastOpenedTimestamp = System.currentTimeMillis(),
                    fileSize = documentInfo.size
                )
                recentFileDao?.upsertByUri(recent)
            } catch (e: Throwable) {
                Log.e(TAG, "Error adding recent file", e)
            }
        }
    }

    fun addRecentFile(uri: Uri, displayName: String, mimeType: String, docTypeName: String, fileSize: Long = 0L) {
        viewModelScope.launch {
            try {
                val recent = RecentFile(
                    uriString = uri.toString(),
                    displayName = displayName,
                    mimeType = mimeType,
                    documentType = docTypeName,
                    lastOpenedTimestamp = System.currentTimeMillis(),
                    fileSize = fileSize
                )
                recentFileDao?.upsertByUri(recent)
            } catch (e: Throwable) {
                Log.e(TAG, "Error adding recent file", e)
            }
        }
    }

    fun removeRecentFile(recentFile: RecentFile) {
        viewModelScope.launch {
            try {
                recentFileDao?.delete(recentFile)
            } catch (e: Throwable) {
                Log.e(TAG, "Error removing recent file", e)
            }
        }
    }

    fun clearRecentFiles() {
        viewModelScope.launch {
            try {
                recentFileDao?.deleteAll()
            } catch (e: Throwable) {
                Log.e(TAG, "Error clearing recent files", e)
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
