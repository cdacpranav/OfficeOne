package com.officone.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.officone.app.data.OfficeOneDatabase
import com.officone.app.data.RecentFile
import com.officone.app.data.ThemeMode
import com.officone.app.document.DocumentType
import com.officone.app.fileio.SAFFileHandler
import com.officone.app.navigation.NavRoutes
import com.officone.app.navigation.OfficeOneNavHost
import com.officone.app.ui.theme.OfficeOneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val safFileHandler = SAFFileHandler()
    private val incomingUriFlow = MutableSharedFlow<Uri>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Process startup intent
        handleIntent(intent)

        setContent {
            val preferencesManager = (application as? OfficeOneApplication)?.preferencesManager
            val themeMode by (preferencesManager?.themeMode
                ?: kotlinx.coroutines.flow.flowOf(ThemeMode.SYSTEM)).collectAsState(initial = ThemeMode.SYSTEM)

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            OfficeOneTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Observe incoming intents to navigate
                    LaunchedEffect(Unit) {
                        incomingUriFlow.asSharedFlow().collect { uri ->
                            navigateToDocument(navController, uri)
                        }
                    }

                    OfficeOneNavHost(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> {
                val uri = intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                if (uri != null) {
                    processIncomingUri(uri)
                }
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

                if (uri != null) {
                    processIncomingUri(uri)
                }
            }
        }
    }

    private fun processIncomingUri(uri: Uri) {
        // Take persistable permission if content URI
        if (uri.scheme == "content") {
            safFileHandler.takePersistablePermission(this, uri)
        }

        // Record in Recent Files database
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val docInfo = safFileHandler.openDocument(this@MainActivity, uri)
                val database = OfficeOneDatabase.getInstance(this@MainActivity)
                database.recentFileDao().upsertByUri(
                    RecentFile(
                        uriString = uri.toString(),
                        displayName = docInfo.displayName,
                        mimeType = docInfo.mimeType,
                        documentType = docInfo.documentType.name,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        fileSize = docInfo.size
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error recording recent file for $uri: ${e.message}")
            }
        }

        incomingUriFlow.tryEmit(uri)
    }

    private fun navigateToDocument(navController: NavHostController, uri: Uri) {
        val mimeType = safFileHandler.getMimeType(this, uri)
        val docType = safFileHandler.openDocument(this, uri).documentType

        val route = when (docType) {
            DocumentType.PDF -> NavRoutes.PdfViewer.createRoute(uri.toString())
            DocumentType.SPREADSHEET -> NavRoutes.SpreadsheetEditor.createRoute(uri.toString(), mimeType)
            DocumentType.PRESENTATION -> NavRoutes.PresentationEditor.createRoute(uri.toString(), mimeType)
            DocumentType.DOCUMENT -> NavRoutes.DocumentEditor.createRoute(uri.toString(), mimeType)
            DocumentType.UNKNOWN -> {
                if (mimeType.contains("pdf")) {
                    NavRoutes.PdfViewer.createRoute(uri.toString())
                } else {
                    NavRoutes.DocumentEditor.createRoute(uri.toString(), mimeType)
                }
            }
        }

        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
