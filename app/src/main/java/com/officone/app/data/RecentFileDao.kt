package com.officone.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY last_opened_timestamp DESC LIMIT 50")
    fun getRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recentFile: RecentFile): Long

    @Query("SELECT * FROM recent_files WHERE uri_string = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): RecentFile?

    @Delete
    suspend fun delete(recentFile: RecentFile): Int

    @Query("DELETE FROM recent_files")
    suspend fun deleteAll(): Int

    @Transaction
    suspend fun upsertByUri(recentFile: RecentFile) {
        val existing = getByUri(recentFile.uriString)
        if (existing != null) {
            insert(
                recentFile.copy(
                    id = existing.id,
                    lastOpenedTimestamp = recentFile.lastOpenedTimestamp
                )
            )
        } else {
            insert(recentFile)
        }
    }
}
