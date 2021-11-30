package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.Column
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.impl.columns.toColumns
import org.jetbrains.kotlinx.dataframe.impl.getType
import org.jetbrains.kotlinx.dataframe.math.cumSum
import org.jetbrains.kotlinx.dataframe.math.defaultCumSumSkipNA
import org.jetbrains.kotlinx.dataframe.typeClass
import java.math.BigDecimal
import kotlin.reflect.KProperty

// region cumSum

public fun <T : Number?> DataColumn<T>.cumSum(skipNA: Boolean = defaultCumSumSkipNA): DataColumn<T> = when (type()) {
    getType<Double>() -> cast<Double>().cumSum(skipNA).cast()
    getType<Double?>() -> cast<Double?>().cumSum(skipNA).cast()
    getType<Float>() -> cast<Float>().cumSum(skipNA).cast()
    getType<Float?>() -> cast<Float?>().cumSum(skipNA).cast()
    getType<Int>() -> cast<Int>().cumSum().cast()
    getType<Int?>() -> cast<Int?>().cumSum(skipNA).cast()
    getType<Long>() -> cast<Long>().cumSum().cast()
    getType<Long?>() -> cast<Long?>().cumSum(skipNA).cast()
    getType<BigDecimal>() -> cast<BigDecimal>().cumSum().cast()
    getType<BigDecimal?>() -> cast<BigDecimal?>().cumSum(skipNA).cast()
    getType<Number?>(), getType<Number>() -> convertToDouble().cumSum(skipNA).cast()
    else -> error("Cumsum for type ${type()} is not supported")
}

private val supportedClasses = setOf(Double::class, Float::class, Int::class, Long::class, BigDecimal::class)

public fun <T, C> DataFrame<T>.cumSum(skipNA: Boolean = defaultCumSumSkipNA, columns: ColumnsSelector<T, C>): DataFrame<T> =
    convert(columns).to { if (it.typeClass in supportedClasses) it.cast<Number?>().cumSum(skipNA) else it }
public fun <T> DataFrame<T>.cumSum(vararg columns: String, skipNA: Boolean = defaultCumSumSkipNA): DataFrame<T> = cumSum(skipNA) { columns.toColumns() }
public fun <T> DataFrame<T>.cumSum(vararg columns: Column, skipNA: Boolean = defaultCumSumSkipNA): DataFrame<T> = cumSum(skipNA) { columns.toColumns() }
public fun <T> DataFrame<T>.cumSum(vararg columns: KProperty<*>, skipNA: Boolean = defaultCumSumSkipNA): DataFrame<T> = cumSum(skipNA) { columns.toColumns() }

public fun <T> DataFrame<T>.cumSum(skipNA: Boolean = defaultCumSumSkipNA): DataFrame<T> = cumSum(skipNA) { allDfs() }

// endregion