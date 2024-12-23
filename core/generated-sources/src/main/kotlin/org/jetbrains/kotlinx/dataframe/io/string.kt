package org.jetbrains.kotlinx.dataframe.io

import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.AnyRow
import org.jetbrains.kotlinx.dataframe.api.asNumbers
import org.jetbrains.kotlinx.dataframe.api.columnsCount
import org.jetbrains.kotlinx.dataframe.api.isNumber
import org.jetbrains.kotlinx.dataframe.api.take
import org.jetbrains.kotlinx.dataframe.api.toColumn
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.columns.ColumnKind
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.impl.asArrayAsListOrNull
import org.jetbrains.kotlinx.dataframe.impl.columns.addPath
import org.jetbrains.kotlinx.dataframe.impl.emptyPath
import org.jetbrains.kotlinx.dataframe.impl.owner
import org.jetbrains.kotlinx.dataframe.impl.renderType
import org.jetbrains.kotlinx.dataframe.impl.scale
import org.jetbrains.kotlinx.dataframe.impl.truncate
import org.jetbrains.kotlinx.dataframe.index
import org.jetbrains.kotlinx.dataframe.jupyter.RenderedContent
import org.jetbrains.kotlinx.dataframe.ncol
import org.jetbrains.kotlinx.dataframe.nrow
import org.jetbrains.kotlinx.dataframe.size
import java.math.BigDecimal

internal fun AnyFrame.renderToString(
    rowsLimit: Int = 20,
    valueLimit: Int = 40,
    borders: Boolean = false,
    alignLeft: Boolean = false,
    columnTypes: Boolean = false,
    title: Boolean = false,
    rowIndex: Boolean = true,
): String {
    val sb = StringBuilder()

    // title
    if (title) {
        sb.appendLine("Data Frame [${size()}]")
        sb.appendLine()
    }

    // data
    val rowsCount = rowsLimit.coerceAtMost(nrow)
    val cols = if (rowIndex) listOf((0 until rowsCount).toColumn()) + columns() else columns()
    val colsWithPaths: List<ColumnWithPath<*>> = cols.map { it.addPath(emptyPath()) }

    // flattened columns
    val flatCols = flattenColumns(colsWithPaths)
    val maxPath = flatCols.maxOfOrNull { it.path.size } ?: 0

    // Check if the flattened columns are not empty
    if (flatCols.isEmpty()) {
        sb.appendLine("Empty DataFrame")
        return sb.toString()
    }

    val headerRows = (0 until maxPath).map { depth ->
        flatCols.map { col ->
            var name = if (depth < col.path.size) col.path[depth] else ""
            if (columnTypes && depth == col.path.size - 1) {
                name += ":${renderType(col)}"
            }
            name
        }
    }

    // Adjusted to flatCols
    val values = flatCols.map { col ->
        val top = col.take(rowsLimit)
        val precision = if (top.isNumber()) top.asNumbers().scale() else 0
        val decimalFormat =
            if (precision >= 0) RendererDecimalFormat.fromPrecision(precision) else RendererDecimalFormat.of("%e")
        top.values().map { value ->
            renderValueForStdout(value, valueLimit, decimalFormat = decimalFormat).truncatedContent
        }
    }

    // Adjusted to hierarchical columns
    val columnWidths = flatCols.indices.map { i ->
        val maxHeader = headerRows.maxOf { it[i].length }
        val maxValue = values[i].maxOfOrNull { it.length } ?: 0
        kotlin.math.max(maxHeader, maxValue) + 1
    }

    // Remove duplicates from headers
    val mutableHeaderRows = headerRows.map { it.toMutableList() }
    for (row in mutableHeaderRows) {
        var last = ""
        for (i in row.indices) {
            if (row[i] == last) {
                row[i] = ""
            } else {
                last = row[i]
            }
        }
    }

    // top border
    if (borders) {
        sb.append("\u230C")
        for (i in columnWidths.indices) {
            val line = "-".repeat(columnWidths[i])
            sb.append(line)
            if (i == columnWidths.lastIndex) {
                sb.append("\u230D")
            } else {
                sb.append("+")
            }
        }
        sb.appendLine()
    }

    // header
    for (row in mutableHeaderRows) {
        if (borders) sb.append("|")
        for ((i, str) in row.withIndex()) {
            val width = columnWidths[i]
            val padded = if (alignLeft) str.padEnd(width) else str.padStart(width)
            sb.append(padded)
            if (borders) sb.append("|") // if (borders && i == row.lastIndex)
        }
        sb.appendLine()
    }

    // header splitter
    if (borders) {
        sb.append("|")
        for (width in columnWidths) {
            sb.append("-".repeat(width))
            sb.append("|")
        }
        sb.appendLine()
    }

    // data
    for (row in 0 until rowsCount) {
        if (borders) sb.append("|")
        for ((i, value) in values.withIndex()) {
            val width = columnWidths[i]
            val str = value[row]
            val padded = if (alignLeft) str.padEnd(width) else str.padStart(width)
            sb.append(padded)
            if (borders) sb.append("|")
        }
        sb.appendLine()
    }

    // footer
    if (nrow > rowsLimit) {
        sb.appendLine("...")
    } else if (borders) {
        sb.append("\u230E")
        for (i in columnWidths.indices) {
            val line = "-".repeat(columnWidths[i])
            sb.append(line)
            if (i == columnWidths.lastIndex) {
                sb.append("\u230F")
            } else {
                sb.append("+")
            }
        }
        sb.appendLine()
    }
    return sb.toString()
}

// define a function that flattens columns. It accepts columns and produces a list of pairs containing
// path to a column and the column itself. We will use it to create a hierarchical header
private fun flattenColumns(cols: List<ColumnWithPath<*>>): List<ColumnWithPath<*>> =
    cols.flatMap { col ->
        val newPath = col.path.plus(col.name())
        when {
            col.data.kind() == ColumnKind.Group -> {
                val group = col.data as ColumnGroup<*>
                flattenColumns(group.columns().map { it.addPath(newPath) })
            }

            else -> {
                val colWithNewPath = col.data.addPath(newPath)
                listOf(colWithNewPath)
            }
        }
    }

internal val valueToStringLimitDefault = 1000
internal val valueToStringLimitForRowAsTable = 50

internal fun AnyRow.getVisibleValues(): List<Pair<String, Any?>> {
    fun Any?.skip(): Boolean =
        when (this) {
            null -> true
            is List<*> -> this.isEmpty()
            is AnyRow -> values().all { it.skip() }
            else -> false
        }
    return owner.columns().map { it.name() to it[index] }.filter { !it.second.skip() }
}

internal fun AnyRow.renderToString(): String {
    val values = getVisibleValues()
    if (values.isEmpty()) return "{ }"
    return values.joinToString(
        prefix = "{ ",
        postfix = " }",
    ) { "${it.first}:${renderValueForStdout(it.second).truncatedContent}" }
}

internal fun AnyRow.renderToStringTable(forHtml: Boolean = false): String {
    if (columnsCount() == 0) return ""
    val pairs = owner.columns().map { it.name() to renderValueForRowTable(it[index], forHtml) }
    val width = pairs.maxOf { it.first.length + it.second.textLength } + 4
    return pairs.joinToString("\n") {
        it.first + " ".repeat(width - it.first.length - it.second.textLength) + it.second.truncatedContent
    }
}

internal fun renderCollectionName(value: Collection<*>) =
    when (value) {
        is List -> "List"
        is Map<*, *> -> "Map"
        is Set -> "Set"
        else -> value.javaClass.simpleName
    }

internal fun renderValueForRowTable(value: Any?, forHtml: Boolean): RenderedContent =
    when (value) {
        is AnyFrame -> "DataFrame [${value.nrow} x ${value.ncol}]".let {
            val content = if (value.nrow == 1) it + " " + value[0].toString() else it
            RenderedContent.textWithLength(content, "DataFrame".length)
        }

        is AnyRow -> RenderedContent.textWithLength("DataRow $value", "DataRow".length)

        is Collection<*> -> renderCollectionName(value).let { RenderedContent.textWithLength("$it $value", it.length) }

        else -> if (forHtml) {
            renderValueForHtml(value, valueToStringLimitForRowAsTable, RendererDecimalFormat.DEFAULT)
        } else {
            renderValueForStdout(value, valueToStringLimitForRowAsTable)
        }
    }

internal fun renderValueForStdout(
    value: Any?,
    limit: Int = valueToStringLimitDefault,
    decimalFormat: RendererDecimalFormat = RendererDecimalFormat.DEFAULT,
): RenderedContent =
    renderValueToString(value, decimalFormat)
        .truncate(limit)
        .let { it.copy(truncatedContent = it.truncatedContent.escapeNewLines()) }

internal fun renderValueToString(value: Any?, decimalFormat: RendererDecimalFormat): String =
    when (value) {
        is AnyFrame -> "[${value.size}]".let { if (value.nrow == 1) it + " " + value[0].toString() else it }

        is Double -> value.format(decimalFormat)

        is Float -> value.format(decimalFormat)

        is BigDecimal -> value.format(decimalFormat)

        is List<*> -> if (value.isEmpty()) "[ ]" else value.toString()

        is Array<*> -> if (value.isEmpty()) "[ ]" else value.toList().toString()

        else ->
            value?.asArrayAsListOrNull()
                ?.let { renderValueToString(it, decimalFormat) }
                ?: value.toString()
    }

internal fun internallyRenderable(value: Any?): Boolean =
    when (value) {
        is AnyFrame, is Double, is List<*>, null, "" -> true
        else -> false
    }

internal fun Double.format(decimalFormat: RendererDecimalFormat): String = decimalFormat.format.format(this)

internal fun Float.format(decimalFormat: RendererDecimalFormat): String = decimalFormat.format.format(this)

internal fun BigDecimal.format(decimalFormat: RendererDecimalFormat): String = decimalFormat.format.format(this)
