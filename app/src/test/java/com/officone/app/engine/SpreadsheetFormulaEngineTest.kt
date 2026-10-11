package com.officone.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsheetFormulaEngineTest {

    @Test
    fun testBasicArithmetic() {
        val grid = emptyMap<String, String>()
        assertEquals("15", SpreadsheetFormulaEngine.evaluate("=10 + 5", grid))
        assertEquals("6", SpreadsheetFormulaEngine.evaluate("=10 - 4", grid))
        assertEquals("42", SpreadsheetFormulaEngine.evaluate("=6 * 7", grid))
        assertEquals("5", SpreadsheetFormulaEngine.evaluate("=20 / 4", grid))
        assertEquals("8", SpreadsheetFormulaEngine.evaluate("=2 ^ 3", grid))
        assertEquals("14", SpreadsheetFormulaEngine.evaluate("=2 + 3 * 4", grid)) // Operator precedence
        assertEquals("20", SpreadsheetFormulaEngine.evaluate("=(2 + 3) * 4", grid)) // Parentheses
    }

    @Test
    fun testMathematicalFunctions() {
        val grid = emptyMap<String, String>()
        assertEquals("60", SpreadsheetFormulaEngine.evaluate("=SUM(10, 20, 30)", grid))
        assertEquals("20", SpreadsheetFormulaEngine.evaluate("=AVERAGE(10, 20, 30)", grid))
        assertEquals("5", SpreadsheetFormulaEngine.evaluate("=MIN(15, 5, 25)", grid))
        assertEquals("25", SpreadsheetFormulaEngine.evaluate("=MAX(15, 5, 25)", grid))
        assertEquals("3.14", SpreadsheetFormulaEngine.evaluate("=ROUND(3.14159, 2)", grid))
        assertEquals("3", SpreadsheetFormulaEngine.evaluate("=COUNT(10, \"text\", 30, \"\", 40)", grid))
        assertEquals("4", SpreadsheetFormulaEngine.evaluate("=COUNTA(10, \"text\", 30, \"\", 40)", grid))
    }

    @Test
    fun testLogicalFunctions() {
        val grid = emptyMap<String, String>()
        assertEquals("PASS", SpreadsheetFormulaEngine.evaluate("=IF(10 > 5, \"PASS\", \"FAIL\")", grid))
        assertEquals("FAIL", SpreadsheetFormulaEngine.evaluate("=IF(2 > 5, \"PASS\", \"FAIL\")", grid))
        assertEquals("TRUE", SpreadsheetFormulaEngine.evaluate("=AND(10 > 5, 2 < 4)", grid))
        assertEquals("FALSE", SpreadsheetFormulaEngine.evaluate("=AND(10 > 5, 2 > 4)", grid))
        assertEquals("TRUE", SpreadsheetFormulaEngine.evaluate("=OR(10 < 5, 2 < 4)", grid))
        assertEquals("FALSE", SpreadsheetFormulaEngine.evaluate("=OR(10 < 5, 2 > 4)", grid))
        assertEquals("OK", SpreadsheetFormulaEngine.evaluate("=IFERROR(10 / 0, \"OK\")", grid))
    }

    @Test
    fun testStringFunctions() {
        val grid = emptyMap<String, String>()
        assertEquals("HelloWorld", SpreadsheetFormulaEngine.evaluate("=CONCAT(\"Hello\", \"World\")", grid))
        assertEquals("Hello World", SpreadsheetFormulaEngine.evaluate("=\"Hello\" & \" \" & \"World\"", grid))
        assertEquals("10", SpreadsheetFormulaEngine.evaluate("=LEN(\"Office One\")", grid))
        assertEquals("Off", SpreadsheetFormulaEngine.evaluate("=LEFT(\"Office\", 3)", grid))
        assertEquals("ice", SpreadsheetFormulaEngine.evaluate("=RIGHT(\"Office\", 3)", grid))
        assertEquals("fic", SpreadsheetFormulaEngine.evaluate("=MID(\"Office\", 3, 3)", grid))
    }

    @Test
    fun testRangeAndCellReferences() {
        val grid = mapOf(
            "A1" to "10",
            "A2" to "20",
            "A3" to "30",
            "B1" to "5",
            "B2" to "15"
        )

        assertEquals("60", SpreadsheetFormulaEngine.evaluate("=SUM(A1:A3)", grid))
        assertEquals("20", SpreadsheetFormulaEngine.evaluate("=AVERAGE(A1:A3)", grid))
        assertEquals("80", SpreadsheetFormulaEngine.evaluate("=SUM(A1:A3) + SUM(B1:B2)", grid))
        assertEquals("25", SpreadsheetFormulaEngine.evaluate("=A1 + B2", grid))
    }

    @Test
    fun testDependentRecalculation() {
        val rawGrid = mapOf(
            "A1" to "100",
            "A2" to "200",
            "A3" to "=SUM(A1:A2)",       // 300
            "A4" to "=A3 * 0.1",          // 30 (10% tax)
            "A5" to "=A3 + A4"            // 330 (Total)
        )

        val recalculated = SpreadsheetFormulaEngine.recalculateAll(rawGrid)
        assertEquals("100", recalculated["A1"])
        assertEquals("200", recalculated["A2"])
        assertEquals("300", recalculated["A3"])
        assertEquals("30", recalculated["A4"])
        assertEquals("330", recalculated["A5"])
    }

    @Test
    fun testErrorHandling() {
        val grid = emptyMap<String, String>()
        assertEquals(SpreadsheetFormulaEngine.ERROR_DIV_ZERO, SpreadsheetFormulaEngine.evaluate("=10 / 0", grid))

        // Circular reference detection
        val circularGrid = mapOf(
            "A1" to "=B1 + 1",
            "B1" to "=A1 + 1"
        )
        val circularResult = SpreadsheetFormulaEngine.recalculateAll(circularGrid)
        assertEquals(SpreadsheetFormulaEngine.ERROR_CIRCULAR, circularResult["A1"])
        assertEquals(SpreadsheetFormulaEngine.ERROR_CIRCULAR, circularResult["B1"])
    }

    @Test
    fun testRangeExpansion() {
        val expanded = SpreadsheetFormulaEngine.expandRange("A1", "B2")
        assertEquals(listOf("A1", "B1", "A2", "B2"), expanded)

        val colExpanded = SpreadsheetFormulaEngine.expandRange("C1", "C3")
        assertEquals(listOf("C1", "C2", "C3"), colExpanded)
    }
}
