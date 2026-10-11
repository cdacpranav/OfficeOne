package com.officone.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationEditorLogicTest {

    @Test
    fun testSlideDataCreationAndMutation() {
        val slide = SlideData(
            id = 1,
            title = "Introduction",
            body = "Welcome to Office One",
            notes = "Spoken notes"
        )

        assertEquals(1, slide.id)
        assertEquals("Introduction", slide.title)
        assertEquals("Welcome to Office One", slide.body)
        assertEquals("Spoken notes", slide.notes)

        val updatedSlide = slide.copy(title = "Updated Title")
        assertEquals("Updated Title", updatedSlide.title)
        assertEquals("Welcome to Office One", updatedSlide.body)
    }

    @Test
    fun testSlideListOperations() {
        val slides = mutableListOf(
            SlideData(1, "Slide 1"),
            SlideData(2, "Slide 2")
        )

        // Add Slide
        val newSlide = SlideData(3, "Slide 3")
        slides.add(newSlide)
        assertEquals(3, slides.size)
        assertEquals("Slide 3", slides[2].title)

        // Delete Slide
        slides.removeAt(1) // remove Slide 2
        assertEquals(2, slides.size)
        assertEquals("Slide 1", slides[0].title)
        assertEquals("Slide 3", slides[1].title)
    }
}
