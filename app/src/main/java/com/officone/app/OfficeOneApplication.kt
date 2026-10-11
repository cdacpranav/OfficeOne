package com.officone.app

import android.app.Application
import android.util.Log
import com.officone.app.data.OfficeOneDatabase
import com.officone.app.data.PreferencesManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfficeOneApplication : Application() {

    lateinit var database: OfficeOneDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        setupCrashHandler()
        initializeSingletons()
    }

    private fun initializeSingletons() {
        // Initialize Room Database
        database = OfficeOneDatabase.getInstance(this)

        // Initialize PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)

        // Initialize PDFBox resource loader for Android
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize PDFBoxResourceLoader", e)
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val crashDir = File(filesDir, CRASH_LOG_DIR)
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val crashFile = File(crashDir, "crash_$timestamp.log")

        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        val stackTrace = stringWriter.toString()

        FileWriter(crashFile).use { writer ->
            writer.write("================ CRASH REPORT ================\n")
            writer.write("Timestamp: ${Date()}\n")
            writer.write("Thread: ${thread.name} (id: ${thread.id})\n")
            writer.write("Package: $packageName\n")
            writer.write("Exception: ${throwable::class.java.name}\n")
            writer.write("Message: ${throwable.message}\n\n")
            writer.write("Stacktrace:\n")
            writer.write(stackTrace)
            writer.write("\n==============================================\n")
        }
        Log.e(TAG, "Uncaught exception in thread ${thread.name}. Log written to ${crashFile.absolutePath}", throwable)
    }

    companion object {
        private const val TAG = "OfficeOneApplication"
        private const val CRASH_LOG_DIR = "crashes"

        lateinit var instance: OfficeOneApplication
            private set
    }
}
