package com.officone.app.ui.pdf

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: android.net.Uri? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfViewerViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pdfState by viewModel.pdfState.collectAsState()

    LaunchedEffect(uri) {
        if (uri != null) {
            viewModel.loadPdf(context, uri)
        }
    }
    val totalPages by viewModel.totalPages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val annotationMode by viewModel.annotationMode.collectAsState()
    val renderedPages by viewModel.renderedPages.collectAsState()

    var showSearchOverlay by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val listState = rememberLazyListState()

    // Sync scroll with current page indicator
    val firstVisibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(firstVisibleItemIndex) {
        if (firstVisibleItemIndex != currentPage && totalPages > 0) {
            viewModel.goToPage(firstVisibleItemIndex)
        }
    }

    // Trigger page renders as user scrolls
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    val first = visibleItems.first().index
                    val last = visibleItems.last().index
                    viewModel.renderPages(first..last)
                }
            }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.75f, 4.0f)
        viewModel.setZoom(scale)
        offset += panChange
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        val title = when (val state = pdfState) {
                            is PdfState.Loaded -> state.fileName
                            is PdfState.Loading -> "Loading document..."
                            is PdfState.Error -> "Error opening document"
                            else -> "PDF Viewer"
                        }
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchOverlay = !showSearchOverlay }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search text"
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Annotate") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setAnnotationMode(
                                        if (annotationMode == AnnotationMode.NONE) AnnotationMode.HIGHLIGHT else AnnotationMode.NONE
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit to Width") },
                                onClick = {
                                    showMenu = false
                                    scale = 1.0f
                                    offset = Offset.Zero
                                    viewModel.setZoom(1.0f)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Document Properties") },
                                onClick = { showMenu = false }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Bar Overlay
                AnimatedVisibility(visible = showSearchOverlay) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.search(it) },
                                placeholder = { Text("Find in document...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.clearSearch() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${searchResults.size} found",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Annotation Toolbar
                AnimatedVisibility(visible = annotationMode != AnnotationMode.NONE) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilterChip(
                                selected = annotationMode == AnnotationMode.HIGHLIGHT,
                                onClick = { viewModel.setAnnotationMode(AnnotationMode.HIGHLIGHT) },
                                label = { Text("Highlight") },
                                leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null) }
                            )
                            FilterChip(
                                selected = annotationMode == AnnotationMode.UNDERLINE,
                                onClick = { viewModel.setAnnotationMode(AnnotationMode.UNDERLINE) },
                                label = { Text("Underline") },
                                leadingIcon = { Icon(Icons.Default.FormatUnderlined, contentDescription = null) }
                            )
                            FilterChip(
                                selected = annotationMode == AnnotationMode.NOTE,
                                onClick = { viewModel.setAnnotationMode(AnnotationMode.NOTE) },
                                label = { Text("Note") },
                                leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                            )
                            FilterChip(
                                selected = annotationMode == AnnotationMode.DRAW,
                                onClick = { viewModel.setAnnotationMode(AnnotationMode.DRAW) },
                                label = { Text("Draw") },
                                leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalPages > 0) "Page ${currentPage + 1} of $totalPages" else "Page -",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            scale = (scale - 0.25f).coerceAtLeast(0.5f)
                            viewModel.setZoom(scale)
                        }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                        }
                        Text(
                            text = "${(zoomLevel * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = {
                            scale = (scale + 0.25f).coerceAtMost(4.0f)
                            viewModel.setZoom(scale)
                        }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE0E0E0))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1.2f) 1.0f else 2.0f
                            offset = Offset.Zero
                            viewModel.setZoom(scale)
                        }
                    )
                }
                .transformable(state = transformState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            when (val state = pdfState) {
                is PdfState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PdfState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                is PdfState.Loaded -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed((0 until totalPages).toList()) { index, pageIndex ->
                            val bitmap = renderedPages[pageIndex]

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .shadow(4.dp, RoundedCornerShape(2.dp))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                                    .fillMaxWidth()
                                    .height(480.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Page ${pageIndex + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Rendering page ${pageIndex + 1}...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                PdfState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No document loaded", color = Color.Gray)
                    }
                }
            }
        }
    }
}
