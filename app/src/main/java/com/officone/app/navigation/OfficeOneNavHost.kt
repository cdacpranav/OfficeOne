package com.officone.app.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.officone.app.document.DocumentType
import com.officone.app.fileio.SAFFileHandler
import com.officone.app.ui.editor.DocumentEditorScreen
import com.officone.app.ui.editor.PresentationEditorScreen
import com.officone.app.ui.editor.SpreadsheetEditorScreen
import com.officone.app.ui.home.HomeScreen
import com.officone.app.ui.pdf.PdfViewerScreen
import com.officone.app.ui.settings.SettingsScreen

internal fun navigateToUri(
    navController: NavHostController,
    context: android.content.Context,
    uri: Uri,
    safHandler: SAFFileHandler
) {
    val mimeType = safHandler.getMimeType(context, uri)
    val docType = safHandler.openDocument(context, uri).documentType

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

@Composable
fun OfficeOneNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavRoutes.Home.route,
    homeContent: @Composable () -> Unit = {
        val context = LocalContext.current
        val safHandler = remember { SAFFileHandler() }
        val openDocLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                safHandler.takePersistablePermission(context, uri)
                navigateToUri(navController, context, uri, safHandler)
            }
        }
        val createDocLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("*/*")
        ) { uri ->
            if (uri != null) {
                safHandler.takePersistablePermission(context, uri)
                navigateToUri(navController, context, uri, safHandler)
            }
        }

        HomeScreen(
            onOpenFile = {
                openDocLauncher.launch(
                    arrayOf(
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.oasis.opendocument.text",
                        "application/vnd.oasis.opendocument.spreadsheet",
                        "application/vnd.oasis.opendocument.presentation",
                        "application/pdf",
                        "text/plain",
                        "text/csv",
                        "application/rtf",
                        "*/*"
                    )
                )
            },
            onCreateDocument = { docType ->
                val defaultName = when (docType) {
                    DocumentType.DOCUMENT -> "New_Document.docx"
                    DocumentType.SPREADSHEET -> "New_Spreadsheet.xlsx"
                    DocumentType.PRESENTATION -> "New_Presentation.pptx"
                    else -> "New_Document.docx"
                }
                createDocLauncher.launch(defaultName)
            },
            onOpenRecentFile = { recentFile ->
                val uri = Uri.parse(recentFile.uriString)
                navigateToUri(navController, context, uri, safHandler)
            },
            onNavigateToSettings = {
                navController.navigate(NavRoutes.Settings.route)
            }
        )
    },
    documentEditorContent: @Composable (uri: Uri, mimeType: String) -> Unit = { uri, mime ->
        DocumentEditorScreen(
            uriString = uri.toString(),
            mimeType = mime,
            onNavigateBack = { navController.popBackStack() }
        )
    },
    spreadsheetEditorContent: @Composable (uri: Uri, mimeType: String) -> Unit = { uri, mime ->
        SpreadsheetEditorScreen(
            uriString = uri.toString(),
            mimeType = mime,
            onNavigateBack = { navController.popBackStack() }
        )
    },
    presentationEditorContent: @Composable (uri: Uri, mimeType: String) -> Unit = { uri, mime ->
        PresentationEditorScreen(
            uriString = uri.toString(),
            mimeType = mime,
            onNavigateBack = { navController.popBackStack() }
        )
    },
    pdfViewerContent: @Composable (uri: Uri) -> Unit = { uri ->
        PdfViewerScreen(
            uri = uri,
            onNavigateBack = { navController.popBackStack() }
        )
    },
    settingsContent: @Composable () -> Unit = {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    },
    aboutContent: @Composable () -> Unit = {
        DefaultAboutScreen(onBack = { navController.popBackStack() })
    }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.Home.route) {
            homeContent()
        }

        composable(
            route = NavRoutes.DocumentEditor.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType },
                navArgument("mimeType") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("encodedUri").orEmpty()
            val rawMime = backStackEntry.arguments?.getString("mimeType").orEmpty()
            val decodedUri = Uri.parse(NavRoutes.decodeParam(rawUri))
            val decodedMime = NavRoutes.decodeParam(rawMime)

            documentEditorContent(decodedUri, decodedMime)
        }

        composable(
            route = NavRoutes.SpreadsheetEditor.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType },
                navArgument("mimeType") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("encodedUri").orEmpty()
            val rawMime = backStackEntry.arguments?.getString("mimeType").orEmpty()
            val decodedUri = Uri.parse(NavRoutes.decodeParam(rawUri))
            val decodedMime = NavRoutes.decodeParam(rawMime)

            spreadsheetEditorContent(decodedUri, decodedMime)
        }

        composable(
            route = NavRoutes.PresentationEditor.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType },
                navArgument("mimeType") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("encodedUri").orEmpty()
            val rawMime = backStackEntry.arguments?.getString("mimeType").orEmpty()
            val decodedUri = Uri.parse(NavRoutes.decodeParam(rawUri))
            val decodedMime = NavRoutes.decodeParam(rawMime)

            presentationEditorContent(decodedUri, decodedMime)
        }

        composable(
            route = NavRoutes.PdfViewer.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("encodedUri").orEmpty()
            val decodedUri = Uri.parse(NavRoutes.decodeParam(rawUri))

            pdfViewerContent(decodedUri)
        }

        composable(NavRoutes.Settings.route) {
            settingsContent()
        }

        composable(NavRoutes.About.route) {
            aboutContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultHomeScreen(onNavigateSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Office One") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to Office One\nOpen a document to get started.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultEditorScreen(
    title: String,
    uri: Uri,
    mimeType: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "URI: $uri",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (mimeType.isNotBlank()) {
                    Text(
                        text = "MIME: $mimeType",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultSettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Office One Settings",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultAboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Office One") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Office One v1.0.0\nZero Permissions Offline Office Suite",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
