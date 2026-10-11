package com.officone.app.ui.ribbon

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class InsertAction {
    // Pages
    data object PageBreak : InsertAction()
    data object BlankPage : InsertAction()

    // Tables
    data object InsertTable : InsertAction()

    // Illustrations
    data object InsertImage : InsertAction()
    data object InsertShape : InsertAction()

    // Links
    data object InsertHyperlink : InsertAction()
    data object InsertBookmark : InsertAction()

    // Header & Footer
    data object InsertHeader : InsertAction()
    data object InsertFooter : InsertAction()
    data object InsertPageNumber : InsertAction()

    // Text
    data object InsertTextBox : InsertAction()
    data object InsertDateTime : InsertAction()
    data object InsertSymbol : InsertAction()
}

@Composable
fun InsertRibbonContent(
    onInsertAction: (InsertAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagesGroup = remember {
        RibbonGroup(
            title = "Pages",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    label = "Blank Page",
                    onClick = { onInsertAction(InsertAction.BlankPage) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.Subject,
                    label = "Page Break",
                    onClick = { onInsertAction(InsertAction.PageBreak) }
                )
            )
        )
    }

    val tablesGroup = remember {
        RibbonGroup(
            title = "Tables",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.Default.TableChart,
                    label = "Insert Table",
                    onClick = { onInsertAction(InsertAction.InsertTable) }
                )
            )
        )
    }

    val illustrationsGroup = remember {
        RibbonGroup(
            title = "Illustrations",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.Default.Image,
                    label = "Image",
                    onClick = { onInsertAction(InsertAction.InsertImage) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.Category,
                    label = "Shapes",
                    onClick = { onInsertAction(InsertAction.InsertShape) }
                )
            )
        )
    }

    val linksGroup = remember {
        RibbonGroup(
            title = "Links",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.Default.Link,
                    label = "Hyperlink",
                    onClick = { onInsertAction(InsertAction.InsertHyperlink) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.Bookmark,
                    label = "Bookmark",
                    onClick = { onInsertAction(InsertAction.InsertBookmark) }
                )
            )
        )
    }

    val headerFooterGroup = remember {
        RibbonGroup(
            title = "Header & Footer",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.Default.ViewHeadline,
                    label = "Header",
                    onClick = { onInsertAction(InsertAction.InsertHeader) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.ViewHeadline,
                    label = "Footer",
                    onClick = { onInsertAction(InsertAction.InsertFooter) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.Functions,
                    label = "Page Number",
                    onClick = { onInsertAction(InsertAction.InsertPageNumber) }
                )
            )
        )
    }

    val textGroup = remember {
        RibbonGroup(
            title = "Text",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.Default.FormatColorText,
                    label = "Text Box",
                    onClick = { onInsertAction(InsertAction.InsertTextBox) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.Schedule,
                    label = "Date & Time",
                    onClick = { onInsertAction(InsertAction.InsertDateTime) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.Functions,
                    label = "Symbol",
                    onClick = { onInsertAction(InsertAction.InsertSymbol) }
                )
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RibbonGroupView(group = pagesGroup)
        RibbonGroupView(group = tablesGroup)
        RibbonGroupView(group = illustrationsGroup)
        RibbonGroupView(group = linksGroup)
        RibbonGroupView(group = headerFooterGroup)
        RibbonGroupView(group = textGroup)
    }
}
