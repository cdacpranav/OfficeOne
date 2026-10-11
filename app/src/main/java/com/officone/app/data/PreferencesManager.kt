package com.officone.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "office_one_preferences")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromString(value: String?): ThemeMode {
            return when (value?.uppercase()) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
        }
    }
}

class PreferencesManager private constructor(private val context: Context) {

    private val dataStore = context.dataStore

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeStr = preferences[KEY_THEME_MODE]
            ThemeMode.fromString(modeStr)
        }

    val autoSaveEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_AUTO_SAVE_ENABLED] ?: true
        }

    val autoSaveIntervalSeconds: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_AUTO_SAVE_INTERVAL_SECONDS] ?: 30
        }

    val lastOpenDirectory: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LAST_OPEN_DIRECTORY]
        }

    val showFileExtensions: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SHOW_FILE_EXTENSIONS] ?: true
        }

    val defaultSaveFormat: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_DEFAULT_SAVE_FORMAT] ?: "docx"
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = themeMode.name
        }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_SAVE_ENABLED] = enabled
        }
    }

    suspend fun setAutoSaveIntervalSeconds(intervalSeconds: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_SAVE_INTERVAL_SECONDS] = intervalSeconds.coerceAtLeast(5)
        }
    }

    suspend fun setLastOpenDirectory(directoryUriString: String?) {
        dataStore.edit { preferences ->
            if (directoryUriString == null) {
                preferences.remove(KEY_LAST_OPEN_DIRECTORY)
            } else {
                preferences[KEY_LAST_OPEN_DIRECTORY] = directoryUriString
            }
        }
    }

    suspend fun setShowFileExtensions(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_FILE_EXTENSIONS] = show
        }
    }

    suspend fun setDefaultSaveFormat(format: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_SAVE_FORMAT] = format.trim().lowercase().removePrefix(".")
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        private val KEY_AUTO_SAVE_INTERVAL_SECONDS = intPreferencesKey("auto_save_interval_seconds")
        private val KEY_LAST_OPEN_DIRECTORY = stringPreferencesKey("last_open_directory")
        private val KEY_SHOW_FILE_EXTENSIONS = booleanPreferencesKey("show_file_extensions")
        private val KEY_DEFAULT_SAVE_FORMAT = stringPreferencesKey("default_save_format")

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
