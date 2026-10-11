package com.officone.app.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.officone.app.ui.ribbon.HomeRibbonContent
import com.officone.app.ui.ribbon.RibbonBar
import com.officone.app.ui.ribbon.RibbonTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetEditorScreen(
    uriString: String,
    mimeType: String,
    onNavigateBack: () -> Unit,
    viewModel: SpreadsheetEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val documentInfo by viewModel.documentInfo.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCell by viewModel.selectedCell.collectAsState()
    val formulaValue by viewModel.formulaValue.collectAsState()
    val sheetNames by viewModel.sheetNames.collectAsState()
    val activeSheetIndex by viewModel.activeSheetIndex.collectAsState()
    val cellData by viewModel.cellData.collectAsState()
    val evaluatedCellData by viewModel.evaluatedCellData.collectAsState()

    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        val uri = Uri.parse(uriString)
        viewModel.loadSpreadsheet(context, uri, mimeType)
    }

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
            text = { Text("Do you want to save changes before closing?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        viewModel.saveSpreadsheet(context)
                        onNavigateBack()
                    }
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = documentInfo?.displayName ?: "Spreadsheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isModified) showUnsavedDialog = true else onNavigateBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveSpreadsheet(context) }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* Additional Sheet Options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Sheet Tabs and add sheet button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = activeSheetIndex,
                        edgePadding = 4.dp,
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        sheetNames.forEachIndexed { index, name ->
                            Tab(
                                selected = (activeSheetIndex == index),
                                onClick = { viewModel.setActiveSheet(index) },
                                text = { Text(name, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.addSheet() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Sheet")
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
            // Ribbon Bar for Spreadsheet
            val sheetTabs = remember {
                listOf(
                    RibbonTab("Home"),
                    RibbonTab("Insert"),
                    RibbonTab("Formulas"),
                    RibbonTab("Data"),
                    RibbonTab("View")
                )
            }

            RibbonBar(
                tabs = sheetTabs,
                customContentProvider = { selectedTab ->
                    when (selectedTab) {
                        "Home" -> HomeRibbonContent(
                            onFormatAction = { /* Handle cell formatting */ }
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

            // Formula Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCell,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = "Formula fx",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                BasicTextField(
                    value = formulaValue,
                    onValueChange = { viewModel.updateFormulaValue(it) },
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    singleLine = true
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Spreadsheet Grid
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            } else {
                val columns = remember { ('A'..'Z').map { it.toString() } }
                val rowCount = 50

                val horizontalScrollState = rememberScrollState()

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Header Row (Column labels)
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .height(28.dp)
                        ) {
                            // Empty top-left cell
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            )
                            columns.forEach { colName ->
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Data Rows
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(rowCount) { rowIdx ->
                                val rowNum = rowIdx + 1
                                Row(
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    // Row number header
                                    Box(
                                        modifier = Modifier
                                            .width(42.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rowNum",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Cells
                                    columns.forEach { colName ->
                                        val cellRef = "$colName$rowNum"
                                        val rawValue = cellData[cellRef] ?: ""
                                        val displayValue = evaluatedCellData[cellRef] ?: rawValue

                                        Box(
                                            modifier = Modifier
                                                .width(80.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .border(
                                                    if (isSelected) 1.5.dp else 0.5.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                )
                                                .clickable { viewModel.selectCell(cellRef) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = displayValue,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
