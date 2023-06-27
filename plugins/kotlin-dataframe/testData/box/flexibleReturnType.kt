package org.jetbrains.kotlinx.dataframe

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.experimental.ExperimentalTypeInference
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.annotations.DisableInterpretation
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface ActivePlayer {
    val char: Int
//    val level: Int
//    val race: String
//    val charclass: String
//    val zone: String
//    val guild: Int
    val timestamp: String
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <T, V : Temporal> DataRow<T>.diff(unit: ChronoUnit, expression: RowExpression<T, V>): Long? = prev()?.let { p -> unit.between(expression(this, this), expression(p, p)) }


fun box(): String {
    val df = @DisableInterpretation DataFrame.readDelimStr("""
        char,level,race,charclass,zone,guild,timestamp
        59425,1,Orc,Rogue,Orgrimmar,165,01/01/08 00:02:04
        65494,9,Orc,Hunter,Durotar,-1,01/01/08 00:02:04
    """.trimIndent())
    val df = df.cast<ActivePlayer>()
    val format = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    val df1 = df
        .convert { timestamp }.with { LocalDateTime.parse(it, format)!! }
        .add("tsDiff") { diff(ChronoUnit.MINUTES) { timestamp }?.let { it > 20  } ?: true }
        //.add("charDiff") { diff { char }?.let { it != 0 } ?: true }

    df1.print()
    return "OK"
}
