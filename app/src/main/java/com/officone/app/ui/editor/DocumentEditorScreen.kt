package com.officone.app.ui.editor

import android.content.Intent
import android.net.Uri
import android.print.PrintManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.officone.app.ui.ribbon.HomeRibbonContent
import com.officone.app.ui.ribbon.InsertRibbonContent
import com.officone.app.ui.ribbon.RibbonBar
import com.officone.app.ui.ribbon.RibbonTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditorScreen(
    uriString: String,
    mimeType: String,
    onNavigateBack: () -> Unit,
    onSaveAsRequest: (Uri) -> Unit = {},
    viewModel: DocumentEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val documentInfo by viewModel.documentInfo.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val documentContent by viewModel.documentContent.collectAsState()
    val currentFormat by viewModel.currentFormat.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()
    val characterCount by viewModel.characterCount.collectAsState()

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        val uri = Uri.parse(uriString)
        viewModel.loadDocument(context, uri, mimeType)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    // Intercept Back Press for unsaved changes
    BackHandler(enabled = true) {
        if (isModified) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes in '${documentInfo?.displayName ?: "this document"}'. Do you want to save before closing?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        viewModel.saveDocument(context)
                        onNavigateBack()
                    }
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Discard")
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showPropertiesDialog && documentInfo != null) {
        val info = documentInfo!!
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            title = { Text("Document Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${info.displayName}", fontWeight = FontWeight.SemiBold)
                    Text("Type: ${info.documentType.getDisplayName()}")
                    Text("MIME: ${info.mimeType}")
                    Text("Size: ${info.formattedSize()}")
                    Text("Read Only: ${if (info.isReadOnly) "Yes" else "No"}")
                    Text("URI: ${info.uri}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = documentInfo?.displayName ?: "Document",
                        onValueChange = { viewModel.updateDisplayName(it) },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isModified) {
                                showUnsavedDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveDocument(context) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save As") },
                                leadingIcon = { Icon(Icons.Default.SaveAs, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    documentInfo?.uri?.let { onSaveAsRequest(it) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Export to PDF scheduled")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    documentInfo?.let { doc ->
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = doc.mimeType.ifEmpty { "*/*" }
                                            putExtra(Intent.EXTRA_STREAM, doc.uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Print") },
                                leadingIcon = { Icon(Icons.Default.Print, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Print service unavailable")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Properties") },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showPropertiesDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Page $currentPage of $totalPages • $wordCount words • $characterCount chars",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.zoomOut() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${(zoomLevel * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { viewModel.zoomIn() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ribbon Bar
            val editorTabs = remember {
                listOf(
                    RibbonTab("Home"),
                    RibbonTab("Insert"),
                    RibbonTab("Layout"),
                    RibbonTab("View")
                )
            }

            RibbonBar(
                tabs = editorTabs,
                customContentProvider = { selectedTab ->
                    when (selectedTab) {
                        "Home" -> HomeRibbonContent(
                            onFormatAction = { action ->
                                viewModel.applyRibbonFormatAction(action)
                            }
                        )
                        "Insert" -> InsertRibbonContent(
                            onInsertAction = { action ->
                                viewModel.applyRibbonInsertAction(action)
                            }
                        )
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "$selectedTab tools",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Main Document Viewing / Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                } else {
                    // Interactive A4 Document Page Editor
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .fillMaxHeight(0.96f)
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(24.dp)
                    ) {
                        BasicTextField(
                            value = documentContent,
                            onValueChange = { newText -> viewModel.updateContent(newText) },
                            textStyle = TextStyle(
                                color = if (currentFormat.fontColor != null) Color(currentFormat.fontColor!!) else Color(0xFF1F1F1F),
                                fontSize = ((currentFormat.fontSize ?: 14f) * zoomLevel).sp,
                                fontWeight = if (currentFormat.bold == true) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (currentFormat.italic == true) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                textDecoration = when {
                                    currentFormat.underline == true && currentFormat.strikethrough == true ->
                                        androidx.compose.ui.text.style.TextDecoration.combine(listOf(
                                            androidx.compose.ui.text.style.TextDecoration.Underline,
                                            androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        ))
                                    currentFormat.underline == true -> androidx.compose.ui.text.style.TextDecoration.Underline
                                    currentFormat.strikethrough == true -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    else -> androidx.compose.ui.text.style.TextDecoration.None
                                },
                                textAlign = when (currentFormat.alignment) {
                                    com.officone.app.engine.TextAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                                    com.officone.app.engine.TextAlignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                                    com.officone.app.engine.TextAlignment.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
                                    else -> androidx.compose.ui.text.style.TextAlign.Left
                                }
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
