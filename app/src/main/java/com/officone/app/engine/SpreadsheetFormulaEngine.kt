package com.officone.app.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.pow
import kotlin.math.round

/**
 * High-performance, offline spreadsheet calculation engine for Office One.
 *
 * Implements standard Excel / OpenDocument formulas:
 * - Mathematical: SUM, AVERAGE, MIN, MAX, COUNT, COUNTA, ROUND
 * - Logical: IF, IFS, AND, OR, IFERROR
 * - Lookup & Reference: VLOOKUP, INDEX, MATCH
 * - Text: CONCAT, TEXT, LEFT, RIGHT, MID, LEN
 * - Conditional: SUMIF, COUNTIF
 * - Date & Time: TODAY, NOW
 * - Operators: +, -, *, /, ^, &, =, <>, <, >, <=, >=
 *
 * Supports range resolution (e.g., A1:C5), cell references (relative/absolute e.g., $A$1),
 * cycle detection (#CIRCULAR!), and standard Excel error codes (#DIV/0!, #VALUE!, #N/A, #NAME?, #REF!).
 */
object SpreadsheetFormulaEngine {

    const val ERROR_DIV_ZERO = "#DIV/0!"
    const val ERROR_VALUE = "#VALUE!"
    const val ERROR_REF = "#REF!"
    const val ERROR_NAME = "#NAME?"
    const val ERROR_NA = "#N/A!"
    const val ERROR_CIRCULAR = "#CIRCULAR!"
    const val ERROR_GENERIC = "#ERROR!"

    private val CELL_REF_REGEX = Pattern.compile("^\\$?([A-Za-z]+)\\$?([0-9]+)$")
    private val RANGE_REGEX = Pattern.compile("^\\$?([A-Za-z]+)\\$?([0-9]+):\\$?([A-Za-z]+)\\$?([0-9]+)$")

    /**
     * Recalculates all cells in the grid, resolving dependencies and detecting cycles.
     */
    fun recalculateAll(grid: Map<String, String>): Map<String, String> {
        val calculated = mutableMapOf<String, String>()
        val visiting = mutableSetOf<String>()

        for (cell in grid.keys) {
            evaluateCellRecursive(cell, grid, calculated, visiting)
        }
        return calculated
    }

    /**
     * Evaluates a single cell formula or literal value.
     */
    fun evaluate(formulaOrValue: String, grid: Map<String, String>): String {
        val trimmed = formulaOrValue.trim()
        if (!trimmed.startsWith("=")) {
            return trimmed
        }
        val expression = trimmed.substring(1).trim()
        return try {
            val result = evaluateExpression(expression, grid, mutableSetOf())
            formatResult(result)
        } catch (e: FormulaException) {
            e.errorCode
        } catch (e: Exception) {
            ERROR_GENERIC
        }
    }

    private fun evaluateCellRecursive(
        cell: String,
        grid: Map<String, String>,
        calculated: MutableMap<String, String>,
        visiting: MutableSet<String>
    ): String {
        val normalizedCell = normalizeCellRef(cell)
        if (calculated.containsKey(normalizedCell)) {
            return calculated[normalizedCell]!!
        }

        val raw = grid[normalizedCell]?.trim() ?: grid[cell]?.trim() ?: ""
        if (!raw.startsWith("=")) {
            calculated[normalizedCell] = raw
            return raw
        }

        if (visiting.contains(normalizedCell)) {
            calculated[normalizedCell] = ERROR_CIRCULAR
            return ERROR_CIRCULAR
        }

        visiting.add(normalizedCell)
        val expr = raw.substring(1).trim()

        val evaluated = try {
            val res = evaluateExpression(expr, grid, visiting)
            formatResult(res)
        } catch (e: FormulaException) {
            e.errorCode
        } catch (e: Exception) {
            ERROR_GENERIC
        }

        visiting.remove(normalizedCell)
        calculated[normalizedCell] = evaluated
        return evaluated
    }

    private fun evaluateExpression(
        expr: String,
        grid: Map<String, String>,
        visiting: MutableSet<String>
    ): Any {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return ""

        val parser = ExpressionParser(tokens, grid, visiting)
        return parser.parse()
    }

    private fun formatResult(result: Any): String {
        return when (result) {
            is Double -> {
                if (result.isInfinite() || result.isNaN()) ERROR_DIV_ZERO
                else if (result == round(result)) result.toLong().toString()
                else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
            }
            is Boolean -> result.toString().uppercase()
            else -> result.toString()
        }
    }

    /**
     * Tokenizer supporting strings, numbers, cell refs, ranges, functions, and operators.
     */
    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val n = expr.length

        while (i < n) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c == '"' -> {
                    val sb = StringBuilder()
                    sb.append('"')
                    i++
                    while (i < n && expr[i] != '"') {
                        sb.append(expr[i])
                        i++
                    }
                    if (i < n && expr[i] == '"') {
                        sb.append('"')
                        i++
                    }
                    tokens.add(sb.toString())
                }
                c == '<' -> {
                    if (i + 1 < n && (expr[i + 1] == '=' || expr[i + 1] == '>')) {
                        tokens.add(expr.substring(i, i + 2))
                        i += 2
                    } else {
                        tokens.add("<")
                        i++
                    }
                }
                c == '>' -> {
                    if (i + 1 < n && expr[i + 1] == '=') {
                        tokens.add(">=")
                        i += 2
                    } else {
                        tokens.add(">")
                        i++
                    }
                }
                c in "+-*/^&(),;:" -> {
                    tokens.add(c.toString())
                    i++
                }
                c == '=' -> {
                    tokens.add("==")
                    i++
                }
                else -> {
                    val sb = StringBuilder()
                    while (i < n && !expr[i].isWhitespace() && expr[i] !in "+-*/^&(),;:<>=\"") {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
            }
        }
        return tokens
    }

    private class ExpressionParser(
        private val tokens: List<String>,
        private val grid: Map<String, String>,
        private val visiting: MutableSet<String>
    ) {
        private var pos = 0

        fun parse(): Any {
            val result = parseComparison()
            if (pos < tokens.size) {
                // If extra tokens exist, handle concatenation or return result
            }
            return result
        }

        private fun parseComparison(): Any {
            var left = parseConcat()
            while (pos < tokens.size) {
                val op = tokens[pos]
                if (op in listOf("==", "<>", "<", ">", "<=", ">=")) {
                    pos++
                    val right = parseConcat()
                    left = compareValues(left, right, op)
                } else {
                    break
                }
            }
            return left
        }

        private fun compareValues(left: Any, right: Any, op: String): Boolean {
            val lNum = toDoubleOrNull(left)
            val rNum = toDoubleOrNull(right)

            return if (lNum != null && rNum != null) {
                when (op) {
                    "==" -> lNum == rNum
                    "<>" -> lNum != rNum
                    "<" -> lNum < rNum
                    ">" -> lNum > rNum
                    "<=" -> lNum <= rNum
                    ">=" -> lNum >= rNum
                    else -> false
                }
            } else {
                val lStr = left.toString()
                val rStr = right.toString()
                val cmp = lStr.compareTo(rStr, ignoreCase = true)
                when (op) {
                    "==" -> cmp == 0
                    "<>" -> cmp != 0
                    "<" -> cmp < 0
                    ">" -> cmp > 0
                    "<=" -> cmp <= 0
                    ">=" -> cmp >= 0
                    else -> false
                }
            }
        }

        private fun parseConcat(): Any {
            var left = parseAddSub()
            while (pos < tokens.size && tokens[pos] == "&") {
                pos++
                val right = parseAddSub()
                left = left.toString() + right.toString()
            }
            return left
        }

        private fun parseAddSub(): Any {
            var left = parseMulDiv()
            while (pos < tokens.size && (tokens[pos] == "+" || tokens[pos] == "-")) {
                val op = tokens[pos++]
                val right = parseMulDiv()
                val lNum = toDouble(left)
                val rNum = toDouble(right)
                left = if (op == "+") lNum + rNum else lNum - rNum
            }
            return left
        }

        private fun parseMulDiv(): Any {
            var left = parsePower()
            while (pos < tokens.size && (tokens[pos] == "*" || tokens[pos] == "/")) {
                val op = tokens[pos++]
                val right = parsePower()
                val lNum = toDouble(left)
                val rNum = toDouble(right)
                if (op == "/") {
                    if (rNum == 0.0) throw FormulaException(ERROR_DIV_ZERO)
                    left = lNum / rNum
                } else {
                    left = lNum * rNum
                }
            }
            return left
        }

        private fun parsePower(): Any {
            var left = parseUnary()
            while (pos < tokens.size && tokens[pos] == "^") {
                pos++
                val right = parseUnary()
                left = toDouble(left).pow(toDouble(right))
            }
            return left
        }

        private fun parseUnary(): Any {
            if (pos < tokens.size) {
                if (tokens[pos] == "-") {
                    pos++
                    return -toDouble(parseUnary())
                }
                if (tokens[pos] == "+") {
                    pos++
                    return parseUnary()
                }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Any {
            if (pos >= tokens.size) throw FormulaException(ERROR_VALUE)
            val tok = tokens[pos++]

            // String literal
            if (tok.startsWith("\"") && tok.endsWith("\"")) {
                return tok.substring(1, tok.length - 1)
            }

            // Numeric literal
            tok.toDoubleOrNull()?.let { return it }

            // Parenthesized expression
            if (tok == "(") {
                val res = parseComparison()
                if (pos < tokens.size && tokens[pos] == ")") {
                    pos++
                }
                return res
            }

            // Function call or cell/range reference
            if (pos < tokens.size && tokens[pos] == "(") {
                pos++ // consume '('
                val args = parseArguments()
                return executeFunction(tok.uppercase(), args)
            }

            // Check if it's a range (e.g. A1:B3)
            if (pos + 1 < tokens.size && tokens[pos] == ":") {
                val start = tok
                pos++ // consume ':'
                val end = tokens[pos++]
                return resolveRange(start, end)
            }

            // Cell Reference
            if (isCellReference(tok)) {
                return resolveCell(tok)
            }

            // Boolean literals
            if (tok.equals("TRUE", ignoreCase = true)) return true
            if (tok.equals("FALSE", ignoreCase = true)) return false

            throw FormulaException(ERROR_NAME)
        }

        private fun parseArguments(): List<Any> {
            val args = mutableListOf<Any>()
            if (pos < tokens.size && tokens[pos] == ")") {
                pos++
                return args
            }

            while (pos < tokens.size) {
                // Check if current is a range token or comparison
                if (isCellReference(tokens[pos]) && pos + 1 < tokens.size && tokens[pos + 1] == ":") {
                    val start = tokens[pos]
                    pos += 2
                    val end = tokens[pos++]
                    args.add(resolveRange(start, end))
                } else {
                    args.add(parseComparison())
                }

                if (pos < tokens.size && (tokens[pos] == "," || tokens[pos] == ";")) {
                    pos++
                } else if (pos < tokens.size && tokens[pos] == ")") {
                    pos++
                    break
                } else {
                    break
                }
            }
            return args
        }

        private fun executeFunction(funcName: String, args: List<Any>): Any {
            val flat = flattenNumbers(args)

            return when (funcName) {
                "SUM" -> flat.sum()
                "AVERAGE" -> {
                    if (flat.isEmpty()) throw FormulaException(ERROR_DIV_ZERO)
                    flat.average()
                }
                "MIN" -> flat.minOrNull() ?: 0.0
                "MAX" -> flat.maxOrNull() ?: 0.0
                "COUNT" -> flat.size.toDouble()
                "COUNTA" -> {
                    val flatAll = flattenAll(args)
                    flatAll.filter { it.toString().isNotBlank() }.size.toDouble()
                }
                "ROUND" -> {
                    if (args.isEmpty()) throw FormulaException(ERROR_VALUE)
                    val num = toDouble(args[0])
                    val digits = if (args.size > 1) toDouble(args[1]).toInt() else 0
                    val factor = 10.0.pow(digits)
                    round(num * factor) / factor
                }
                "IF" -> {
                    if (args.size < 2) throw FormulaException(ERROR_VALUE)
                    val condition = toBoolean(args[0])
                    if (condition) args[1] else if (args.size > 2) args[2] else false
                }
                "IFS" -> {
                    if (args.size < 2 || args.size % 2 != 0) throw FormulaException(ERROR_VALUE)
                    for (i in 0 until args.size step 2) {
                        if (toBoolean(args[i])) return args[i + 1]
                    }
                    throw FormulaException(ERROR_NA)
                }
                "AND" -> {
                    if (args.isEmpty()) throw FormulaException(ERROR_VALUE)
                    args.all { toBoolean(it) }
                }
                "OR" -> {
                    if (args.isEmpty()) throw FormulaException(ERROR_VALUE)
                    args.any { toBoolean(it) }
                }
                "IFERROR" -> {
                    if (args.size < 2) throw FormulaException(ERROR_VALUE)
                    val valStr = args[0].toString()
                    if (isError(valStr)) args[1] else args[0]
                }
                "CONCAT", "CONCATENATE" -> {
                    val sb = StringBuilder()
                    flattenAll(args).forEach { sb.append(it.toString()) }
                    sb.toString()
                }
                "LEN" -> {
                    if (args.isEmpty()) 0.0
                    else args[0].toString().length.toDouble()
                }
                "LEFT" -> {
                    if (args.isEmpty()) ""
                    else {
                        val str = args[0].toString()
                        val count = if (args.size > 1) toDouble(args[1]).toInt() else 1
                        str.take(count.coerceAtLeast(0))
                    }
                }
                "RIGHT" -> {
                    if (args.isEmpty()) ""
                    else {
                        val str = args[0].toString()
                        val count = if (args.size > 1) toDouble(args[1]).toInt() else 1
                        str.takeLast(count.coerceAtLeast(0))
                    }
                }
                "MID" -> {
                    if (args.size < 3) throw FormulaException(ERROR_VALUE)
                    val str = args[0].toString()
                    val start = (toDouble(args[1]).toInt() - 1).coerceAtLeast(0)
                    val len = toDouble(args[2]).toInt().coerceAtLeast(0)
                    if (start >= str.length) "" else str.substring(start, (start + len).coerceAtMost(str.length))
                }
                "TODAY" -> {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                }
                "NOW" -> {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                }
                "SUMIF" -> executeSumIf(args)
                "COUNTIF" -> executeCountIf(args)
                "VLOOKUP" -> executeVLookup(args)
                "INDEX" -> executeIndex(args)
                "MATCH" -> executeMatch(args)
                else -> throw FormulaException(ERROR_NAME)
            }
        }

        private fun executeSumIf(args: List<Any>): Double {
            if (args.size < 2) throw FormulaException(ERROR_VALUE)
            val range = flattenAll(listOf(args[0]))
            val criteria = args[1].toString()
            val sumRange = if (args.size >= 3) flattenAll(listOf(args[2])) else range

            var total = 0.0
            for (i in 0 until minOf(range.size, sumRange.size)) {
                if (matchesCriteria(range[i], criteria)) {
                    total += toDoubleOrNull(sumRange[i]) ?: 0.0
                }
            }
            return total
        }

        private fun executeCountIf(args: List<Any>): Double {
            if (args.size < 2) throw FormulaException(ERROR_VALUE)
            val range = flattenAll(listOf(args[0]))
            val criteria = args[1].toString()

            var count = 0
            for (item in range) {
                if (matchesCriteria(item, criteria)) {
                    count++
                }
            }
            return count.toDouble()
        }

        private fun executeVLookup(args: List<Any>): Any {
            if (args.size < 3) throw FormulaException(ERROR_VALUE)
            val lookupValue = args[0].toString()
            val table = args[1] as? List<*> ?: throw FormulaException(ERROR_VALUE)
            val colIndex = toDouble(args[2]).toInt()

            if (colIndex < 1) throw FormulaException(ERROR_VALUE)

            // Table is represented as list of rows
            for (rowObj in table) {
                val row = rowObj as? List<*> ?: continue
                if (row.isNotEmpty()) {
                    val firstVal = row[0]?.toString() ?: ""
                    if (firstVal.equals(lookupValue, ignoreCase = true)) {
                        if (colIndex <= row.size) {
                            return row[colIndex - 1] ?: ""
                        } else {
                            throw FormulaException(ERROR_REF)
                        }
                    }
                }
            }
            throw FormulaException(ERROR_NA)
        }

        private fun executeIndex(args: List<Any>): Any {
            if (args.size < 2) throw FormulaException(ERROR_VALUE)
            val array = flattenAll(listOf(args[0]))
            val rowNum = toDouble(args[1]).toInt()
            if (rowNum < 1 || rowNum > array.size) throw FormulaException(ERROR_REF)
            return array[rowNum - 1]
        }

        private fun executeMatch(args: List<Any>): Double {
            if (args.size < 2) throw FormulaException(ERROR_VALUE)
            val lookupValue = args[0].toString()
            val array = flattenAll(listOf(args[1]))

            for (i in array.indices) {
                if (array[i].toString().equals(lookupValue, ignoreCase = true)) {
                    return (i + 1).toDouble() // 1-based index
                }
            }
            throw FormulaException(ERROR_NA)
        }

        private fun matchesCriteria(value: Any, criteria: String): Boolean {
            val crit = criteria.trim()
            val valNum = toDoubleOrNull(value)

            return when {
                crit.startsWith(">=") -> valNum != null && valNum >= (toDoubleOrNull(crit.substring(2)) ?: 0.0)
                crit.startsWith("<=") -> valNum != null && valNum <= (toDoubleOrNull(crit.substring(2)) ?: 0.0)
                crit.startsWith("<>") -> value.toString() != crit.substring(2)
                crit.startsWith(">") -> valNum != null && valNum > (toDoubleOrNull(crit.substring(1)) ?: 0.0)
                crit.startsWith("<") -> valNum != null && valNum < (toDoubleOrNull(crit.substring(1)) ?: 0.0)
                crit.startsWith("=") -> value.toString().equals(crit.substring(1), ignoreCase = true)
                else -> value.toString().equals(crit, ignoreCase = true)
            }
        }

        private fun resolveCell(cellRef: String): Any {
            val normalized = normalizeCellRef(cellRef)
            val raw = grid[normalized] ?: ""
            if (raw.startsWith("=")) {
                val evaluated = evaluateCellRecursive(normalized, grid, mutableMapOf(), visiting)
                toDoubleOrNull(evaluated)?.let { return it }
                return evaluated
            }
            toDoubleOrNull(raw)?.let { return it }
            return raw
        }

        private fun resolveRange(startRef: String, endRef: String): List<Any> {
            val cells = expandRange(startRef, endRef)
            return cells.map { resolveCell(it) }
        }

        private fun flattenNumbers(list: List<Any>): List<Double> {
            val result = mutableListOf<Double>()
            for (item in list) {
                when (item) {
                    is Double -> result.add(item)
                    is Number -> result.add(item.toDouble())
                    is List<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        result.addAll(flattenNumbers(item as List<Any>))
                    }
                    else -> toDoubleOrNull(item.toString())?.let { result.add(it) }
                }
            }
            return result
        }

        private fun flattenAll(list: List<Any>): List<Any> {
            val result = mutableListOf<Any>()
            for (item in list) {
                if (item is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    result.addAll(flattenAll(item as List<Any>))
                } else {
                    result.add(item)
                }
            }
            return result
        }

        private fun toDouble(obj: Any): Double {
            return toDoubleOrNull(obj) ?: throw FormulaException(ERROR_VALUE)
        }

        private fun toDoubleOrNull(obj: Any?): Double? {
            if (obj == null) return null
            if (obj is Number) return obj.toDouble()
            return obj.toString().trim().toDoubleOrNull()
        }

        private fun toBoolean(obj: Any): Boolean {
            return when (obj) {
                is Boolean -> obj
                is Number -> obj.toDouble() != 0.0
                else -> obj.toString().equals("TRUE", ignoreCase = true)
            }
        }

        private fun isError(str: String): Boolean {
            return str in listOf(ERROR_DIV_ZERO, ERROR_VALUE, ERROR_REF, ERROR_NAME, ERROR_NA, ERROR_CIRCULAR, ERROR_GENERIC)
        }
    }

    private fun normalizeCellRef(cellRef: String): String {
        return cellRef.replace("$", "").uppercase(Locale.US)
    }

    private fun isCellReference(token: String): Boolean {
        return CELL_REF_REGEX.matcher(token).matches()
    }

    fun expandRange(startRef: String, endRef: String): List<String> {
        val startM = CELL_REF_REGEX.matcher(startRef)
        val endM = CELL_REF_REGEX.matcher(endRef)

        if (!startM.matches() || !endM.matches()) return emptyList()

        val startCol = colNameToIndex(startM.group(1))
        val startRow = startM.group(2).toInt()
        val endCol = colNameToIndex(endM.group(1))
        val endRow = endM.group(2).toInt()

        val minCol = minOf(startCol, endCol)
        val maxCol = maxOf(startCol, endCol)
        val minRow = minOf(startRow, endRow)
        val maxRow = maxOf(startRow, endRow)

        val result = mutableListOf<String>()
        for (r in minRow..maxRow) {
            for (c in minCol..maxCol) {
                result.add("${indexToColName(c)}$r")
            }
        }
        return result
    }

    private fun colNameToIndex(name: String): Int {
        var idx = 0
        for (ch in name.uppercase(Locale.US)) {
            idx = idx * 26 + (ch - 'A' + 1)
        }
        return idx
    }

    private fun indexToColName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    class FormulaException(val errorCode: String) : RuntimeException(errorCode)
}
