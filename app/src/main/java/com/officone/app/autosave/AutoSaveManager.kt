package com.officone.app.autosave

import android.content.Context
import android.util.Log
import com.officone.app.document.DocumentType
import com.officone.app.engine.DocumentEngine
import com.officone.app.engine.DocumentHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata descriptor for an auto-saved recovery snapshot.
 */
data class RecoveryFile(
    val recoveryFile: File,
    val originalUriOrPath: String,
    val timestamp: Long,
    val documentType: DocumentType
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Manages automatic document background saving and crash recovery files.
 *
 * Saves periodic snapshots into an isolated internal app storage directory.
 * Metadata for each snapshot (original file URI, timestamp, document type) is stored alongside
 * the recovery snapshot in a `.meta.json` file.
 */
class AutoSaveManager(
    private val context: Context,
    private val documentEngine: DocumentEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null

    private val recoveryDir: File
        get() = File(context.filesDir, RECOVERY_DIRECTORY_NAME).apply {
            if (!exists()) mkdirs()
        }

    /**
     * Starts the periodic auto-save ticker loop for [documentHandle].
     */
    fun startAutoSave(documentHandle: DocumentHandle, intervalMs: Long = 30_000L) {
        stopAutoSave()
        autoSaveJob = scope.launch {
            Log.d(TAG, "Starting auto-save for doc: ${documentHandle.filePath} every ${intervalMs}ms")
            while (isActive) {
                delay(intervalMs)
                try {
                    performSnapshot(documentHandle)
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing auto-save snapshot", e)
                }
            }
        }
    }

    /**
     * Stops the running auto-save ticker job.
     */
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    /**
     * Writes the current document content to the recovery directory with companion metadata.
     */
    private suspend fun performSnapshot(documentHandle: DocumentHandle) {
        val timestamp = System.currentTimeMillis()
        val originalFile = File(documentHandle.filePath)
        val baseName = originalFile.nameWithoutExtension.ifBlank { "untitled" }
        val ext = originalFile.extension.ifBlank { "tmp" }

        val snapshotFile = File(recoveryDir, "recovery_${documentHandle.id}_${timestamp}.$ext")
        val metaFile = File(recoveryDir, "recovery_${documentHandle.id}_${timestamp}.meta.json")

        val saved = documentEngine.saveDocument(documentHandle, snapshotFile)
        if (saved) {
            val json = JSONObject().apply {
                put("originalPath", documentHandle.filePath)
                put("timestamp", timestamp)
                put("docType", documentHandle.documentType.name)
                put("handleId", documentHandle.id)
            }
            metaFile.writeText(json.toString())
            Log.d(TAG, "Auto-saved snapshot saved to: ${snapshotFile.name}")
        } else {
            Log.w(TAG, "DocumentEngine failed to save auto-save snapshot")
        }
    }

    /**
     * Lists all available recovery files sorted descending by modification time.
     */
    suspend fun getRecoveryFiles(): List<RecoveryFile> = withContext(Dispatchers.IO) {
        val files = recoveryDir.listFiles() ?: return@withContext emptyList()
        val recoveryList = mutableListOf<RecoveryFile>()

        val metaFiles = files.filter { it.name.endsWith(".meta.json") }
        for (metaFile in metaFiles) {
            try {
                val json = JSONObject(metaFile.readText())
                val originalPath = json.optString("originalPath", "")
                val timestamp = json.optLong("timestamp", metaFile.lastModified())
                val docTypeName = json.optString("docType", DocumentType.UNKNOWN.name)
                val docType = runCatching { DocumentType.valueOf(docTypeName) }.getOrDefault(DocumentType.UNKNOWN)

                val dataFileName = metaFile.name.removeSuffix(".meta.json")
                val matchedDataFile = files.firstOrNull { it.name.startsWith(dataFileName) && !it.name.endsWith(".meta.json") }

                if (matchedDataFile != null && matchedDataFile.exists()) {
                    recoveryList.add(
                        RecoveryFile(
                            recoveryFile = matchedDataFile,
                            originalUriOrPath = originalPath,
                            timestamp = timestamp,
                            documentType = docType
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse recovery metadata file: ${metaFile.name}", e)
            }
        }

        recoveryList.sortedByDescending { it.timestamp }
    }

    /**
     * Returns the recovery file ready to be loaded or restored.
     */
    fun recoverDocument(recoveryFile: RecoveryFile): File {
        return recoveryFile.recoveryFile
    }

    /**
     * Deletes a recovery snapshot and its accompanying metadata.
     */
    suspend fun deleteRecoveryFile(recoveryFile: RecoveryFile) = withContext(Dispatchers.IO) {
        try {
            val prefix = recoveryFile.recoveryFile.nameWithoutExtension
            recoveryFile.recoveryFile.delete()
            val metaFile = File(recoveryDir, "$prefix.meta.json")
            if (metaFile.exists()) {
                metaFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recovery file: ${recoveryFile.recoveryFile.name}", e)
        }
    }

    /**
     * Purges recovery files older than [maxAgeDays] days.
     */
    suspend fun cleanupOldRecoveryFiles(maxAgeDays: Int = 7) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val maxAgeMs = maxAgeDays.toLong() * 24L * 60L * 60L * 1000L
        val list = getRecoveryFiles()

        for (item in list) {
            if (now - item.timestamp > maxAgeMs) {
                deleteRecoveryFile(item)
            }
        }
    }

    companion object {
        private const val TAG = "AutoSaveManager"
        private const val RECOVERY_DIRECTORY_NAME = "officeone_recovery"
    }
}
