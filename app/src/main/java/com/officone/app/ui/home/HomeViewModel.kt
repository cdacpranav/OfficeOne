package com.officone.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.officone.app.OfficeOneApplication
import com.officone.app.data.RecentFile
import com.officone.app.data.RecentFileDao
import com.officone.app.document.DocumentInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val recentFileDao: RecentFileDao = (application as OfficeOneApplication).database.recentFileDao()
) : AndroidViewModel(application) {

    val recentFiles: StateFlow<List<RecentFile>> = recentFileDao.getRecentFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRecentFile(documentInfo: DocumentInfo) {
        viewModelScope.launch {
            val recent = RecentFile(
                uriString = documentInfo.uri.toString(),
                displayName = documentInfo.displayName,
                mimeType = documentInfo.mimeType,
                documentType = documentInfo.documentType.name,
                lastOpenedTimestamp = System.currentTimeMillis(),
                fileSize = documentInfo.size
            )
            recentFileDao.upsertByUri(recent)
        }
    }

    fun addRecentFile(uri: Uri, displayName: String, mimeType: String, docTypeName: String, fileSize: Long = 0L) {
        viewModelScope.launch {
            val recent = RecentFile(
                uriString = uri.toString(),
                displayName = displayName,
                mimeType = mimeType,
                documentType = docTypeName,
                lastOpenedTimestamp = System.currentTimeMillis(),
                fileSize = fileSize
            )
            recentFileDao.upsertByUri(recent)
        }
    }

    fun removeRecentFile(recentFile: RecentFile) {
        viewModelScope.launch {
            recentFileDao.delete(recentFile)
        }
    }

    fun clearRecentFiles() {
        viewModelScope.launch {
            recentFileDao.deleteAll()
        }
    }
}
