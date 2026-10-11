package com.officone.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentFile::class],
    version = 1,
    exportSchema = false
)
abstract class OfficeOneDatabase : RoomDatabase() {

    abstract fun recentFileDao(): RecentFileDao

    companion object {
        private const val DATABASE_NAME = "office_one.db"

        @Volatile
        private var INSTANCE: OfficeOneDatabase? = null

        fun getInstance(context: Context): OfficeOneDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OfficeOneDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
