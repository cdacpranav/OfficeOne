package com.officone.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentEditorLogicTest {

    @Test
    fun testWordAndCharCountLogic() {
        val sampleText = "Office One is an all-in-one offline Android office suite."
        val charCount = sampleText.length
        val words = sampleText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val wordCount = words.size

        assertEquals(57, charCount)
        assertEquals(9, wordCount)
    }

    @Test
    fun testPaginationEstimation() {
        // 300 words per page average
        val shortText = (1..150).joinToString(" ") { "word" }
        val shortWords = shortText.split("\\s+".toRegex()).size
        val page1 = ((shortWords / 300) + 1).coerceAtLeast(1)
        assertEquals(1, page1)

        val longText = (1..650).joinToString(" ") { "word" }
        val longWords = longText.split("\\s+".toRegex()).size
        val page3 = ((longWords / 300) + 1).coerceAtLeast(1)
        assertEquals(3, page3)
    }

    @Test
    fun testUndoRedoStackLogic() {
        val undoStack = mutableListOf<String>()
        val redoStack = mutableListOf<String>()

        var currentText = "Initial"

        // Type change 1
        undoStack.add(currentText)
        redoStack.clear()
        currentText = "Initial + Second"

        // Type change 2
        undoStack.add(currentText)
        redoStack.clear()
        currentText = "Initial + Second + Third"

        assertEquals(2, undoStack.size)

        // Perform Undo
        val prev1 = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(currentText)
        currentText = prev1
        assertEquals("Initial + Second", currentText)

        // Perform Undo again
        val prev2 = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(currentText)
        currentText = prev2
        assertEquals("Initial", currentText)
        assertTrue(undoStack.isEmpty())

        // Perform Redo
        val next1 = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(currentText)
        currentText = next1
        assertEquals("Initial + Second", currentText)
    }
}
