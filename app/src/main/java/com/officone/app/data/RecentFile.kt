package com.officone.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_files",
    indices = [Index(value = ["uri_string"], unique = true)]
)
data class RecentFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "uri_string")
    val uriString: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "document_type")
    val documentType: String,

    @ColumnInfo(name = "last_opened_timestamp")
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null
)
