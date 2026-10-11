package com.officone.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatActionTest {

    @Test
    fun testFormatActionInstantiations() {
        val boldAction: FormatAction = FormatAction.ToggleBold
        val fontAction = FormatAction.SetFontSize(14.5f)
        val alignAction = FormatAction.SetAlignment(TextAlignment.CENTER)
        val shapeAction = FormatAction.InsertShape("rectangle")

        assertTrue(boldAction is FormatAction.ToggleBold)
        assertEquals(14.5f, fontAction.size)
        assertEquals(TextAlignment.CENTER, alignAction.alignment)
        assertEquals("rectangle", shapeAction.shapeType)
    }

    @Test
    fun testTextFormatDataClass() {
        val format = TextFormat(
            bold = true,
            italic = false,
            fontFamily = "Calibri",
            fontSize = 12f,
            alignment = TextAlignment.LEFT
        )

        assertEquals(true, format.bold)
        assertEquals(false, format.italic)
        assertEquals("Calibri", format.fontFamily)
        assertEquals(12f, format.fontSize)
        assertEquals(TextAlignment.LEFT, format.alignment)
    }
}
