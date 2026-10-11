package com.officone.app.ui.ribbon

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

sealed class FormatAction {
    // Clipboard
    data object Cut : FormatAction()
    data object Copy : FormatAction()
    data object Paste : FormatAction()
    data object FormatPainter : FormatAction()

    // History
    data object Undo : FormatAction()
    data object Redo : FormatAction()

    // Font
    data class FontFamily(val font: String) : FormatAction()
    data class FontSize(val size: Int) : FormatAction()
    data class Bold(val isBold: Boolean) : FormatAction()
    data class Italic(val isItalic: Boolean) : FormatAction()
    data class Underline(val isUnderline: Boolean) : FormatAction()
    data class Strikethrough(val isStrikethrough: Boolean) : FormatAction()
    data class FontColor(val color: Color) : FormatAction()
    data class HighlightColor(val color: Color) : FormatAction()

    // Paragraph
    data object AlignLeft : FormatAction()
    data object AlignCenter : FormatAction()
    data object AlignRight : FormatAction()
    data object AlignJustify : FormatAction()
    data object Bullets : FormatAction()
    data object Numbering : FormatAction()
    data object DecreaseIndent : FormatAction()
    data object IncreaseIndent : FormatAction()
    data object LineSpacing : FormatAction()

    // Styles
    data class Style(val styleName: String) : FormatAction()
}

@Composable
fun HomeRibbonContent(
    onFormatAction: (FormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var selectedFont by remember { mutableStateOf("Roboto") }
    var fontSize by remember { mutableIntStateOf(12) }
    var selectedStyle by remember { mutableStateOf("Normal") }
    var fontColor by remember { mutableStateOf(Color.Black) }
    var highlightColor by remember { mutableStateOf(Color.Yellow) }

    val clipboardGroup = remember {
        RibbonGroup(
            title = "Clipboard",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.Default.ContentCut,
                    label = "Cut",
                    onClick = { onFormatAction(FormatAction.Cut) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = { onFormatAction(FormatAction.Copy) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.ContentPaste,
                    label = "Paste",
                    onClick = { onFormatAction(FormatAction.Paste) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.FormatPaint,
                    label = "Format Painter",
                    onClick = { onFormatAction(FormatAction.FormatPainter) }
                )
            )
        )
    }

    val fontGroup = RibbonGroup(
        title = "Font",
        items = listOf(
            RibbonItem.DropdownButton(
                label = selectedFont,
                options = listOf("Roboto", "Arial", "Times New Roman", "Courier New", "Georgia"),
                selectedOption = selectedFont,
                onSelect = {
                    selectedFont = it
                    onFormatAction(FormatAction.FontFamily(it))
                }
            ),
            RibbonItem.Spinner(
                label = "Size",
                value = fontSize,
                min = 8,
                max = 72,
                step = 1,
                onValueChange = {
                    fontSize = it
                    onFormatAction(FormatAction.FontSize(it))
                }
            ),
            RibbonItem.ToggleButton(
                icon = Icons.Default.FormatBold,
                label = "Bold",
                isActive = isBold,
                onToggle = {
                    isBold = it
                    onFormatAction(FormatAction.Bold(it))
                }
            ),
            RibbonItem.ToggleButton(
                icon = Icons.Default.FormatItalic,
                label = "Italic",
                isActive = isItalic,
                onToggle = {
                    isItalic = it
                    onFormatAction(FormatAction.Italic(it))
                }
            ),
            RibbonItem.ToggleButton(
                icon = Icons.Default.FormatUnderlined,
                label = "Underline",
                isActive = isUnderline,
                onToggle = {
                    isUnderline = it
                    onFormatAction(FormatAction.Underline(it))
                }
            ),
            RibbonItem.ToggleButton(
                icon = Icons.Default.FormatStrikethrough,
                label = "Strikethrough",
                isActive = isStrikethrough,
                onToggle = {
                    isStrikethrough = it
                    onFormatAction(FormatAction.Strikethrough(it))
                }
            ),
            RibbonItem.ColorPicker(
                icon = Icons.Default.FormatColorText,
                label = "Font Color",
                currentColor = fontColor,
                onColorSelect = {
                    fontColor = it
                    onFormatAction(FormatAction.FontColor(it))
                }
            ),
            RibbonItem.ColorPicker(
                icon = Icons.Default.FormatColorFill,
                label = "Highlight Color",
                currentColor = highlightColor,
                onColorSelect = {
                    highlightColor = it
                    onFormatAction(FormatAction.HighlightColor(it))
                }
            )
        )
    )

    val paragraphGroup = remember {
        RibbonGroup(
            title = "Paragraph",
            items = listOf(
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    label = "Align Left",
                    onClick = { onFormatAction(FormatAction.AlignLeft) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.FormatAlignCenter,
                    label = "Align Center",
                    onClick = { onFormatAction(FormatAction.AlignCenter) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                    label = "Align Right",
                    onClick = { onFormatAction(FormatAction.AlignRight) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.FormatAlignJustify,
                    label = "Justify",
                    onClick = { onFormatAction(FormatAction.AlignJustify) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    label = "Bullets",
                    onClick = { onFormatAction(FormatAction.Bullets) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.FormatListNumbered,
                    label = "Numbering",
                    onClick = { onFormatAction(FormatAction.Numbering) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                    label = "Decrease Indent",
                    onClick = { onFormatAction(FormatAction.DecreaseIndent) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                    label = "Increase Indent",
                    onClick = { onFormatAction(FormatAction.IncreaseIndent) }
                ),
                RibbonItem.IconButton(
                    icon = Icons.Default.FormatLineSpacing,
                    label = "Line Spacing",
                    onClick = { onFormatAction(FormatAction.LineSpacing) }
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
        RibbonGroupView(group = clipboardGroup)
        RibbonGroupView(group = fontGroup)
        RibbonGroupView(group = paragraphGroup)

        // Styles Group
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            val styles = listOf("Normal", "Heading 1", "Heading 2", "Heading 3", "Title")
            styles.forEach { style ->
                FilterChip(
                    selected = (selectedStyle == style),
                    onClick = {
                        selectedStyle = style
                        onFormatAction(FormatAction.Style(style))
                    },
                    label = {
                        Text(
                            text = style,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}
