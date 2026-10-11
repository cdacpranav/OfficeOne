package com.officone.app.engine

/**
 * Sealed class defining all formatting and editing actions dispatched from the Office One ribbon UI.
 */
sealed class FormatAction {
    // Text formatting
    data object ToggleBold : FormatAction()
    data object ToggleItalic : FormatAction()
    data object ToggleUnderline : FormatAction()
    data object ToggleStrikethrough : FormatAction()
    data class SetFontFamily(val family: String) : FormatAction()
    data class SetFontSize(val size: Float) : FormatAction()
    data class SetFontColor(val color: Int) : FormatAction()
    data class SetHighlightColor(val color: Int) : FormatAction()

    // Paragraph
    data class SetAlignment(val alignment: TextAlignment) : FormatAction()
    data object ToggleBullets : FormatAction()
    data object ToggleNumbering : FormatAction()
    data object IncreaseIndent : FormatAction()
    data object DecreaseIndent : FormatAction()

    // Clipboard
    data object Cut : FormatAction()
    data object Copy : FormatAction()
    data object Paste : FormatAction()

    // Edit
    data object Undo : FormatAction()
    data object Redo : FormatAction()

    // Insert
    data object InsertTable : FormatAction()
    data object InsertImage : FormatAction()
    data object InsertHyperlink : FormatAction()
    data object InsertPageBreak : FormatAction()
    data class InsertShape(val shapeType: String) : FormatAction()
}
