package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.io.renderToString

public fun AnyFrame.toStringWithParams(
    rowsLimit: Int = 20,
    valueLimit: Int = 40,
    borders: Boolean = false,
    alignLeft: Boolean = false,
    columnTypes: Boolean = false,
    title: Boolean = false,
    rowIndex: Boolean = true,
): String =
    renderToString(
        rowsLimit = rowsLimit,
        valueLimit = valueLimit,
        borders = borders,
        alignLeft = alignLeft,
        columnTypes = columnTypes,
        title = title,
        rowIndex = rowIndex,
    )
