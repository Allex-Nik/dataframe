@file:JvmName("WriteTsvDeephavenKt")

package org.jetbrains.kotlinx.dataframe.io

import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.documentation.CommonWriteDelimDocs
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.ADJUST_CSV_FORMAT
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.COMMENT_CHAR
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.ESCAPE_CHAR
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.FILE_WRITE
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.HEADER_COMMENTS
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.INCLUDE_HEADER
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.PATH_WRITE
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.QUOTE
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.QUOTE_MODE
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.RECORD_SEPARATOR
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.TSV_DELIMITER
import org.jetbrains.kotlinx.dataframe.documentation.DelimParams.WRITER_WRITE
import org.jetbrains.kotlinx.dataframe.impl.io.writeDelimImpl
import java.io.File
import java.io.FileWriter

/**
 * @include [CommonWriteDelimDocs.TsvDocs]
 * @set [CommonWriteDelimDocs.WriteOrConvertArg] Write
 * @set [CommonWriteDelimDocs.DataTitleArg] File
 * @set [CommonWriteDelimDocs.DataArg] file
 * @include [FILE_WRITE]
 * @include [TSV_DELIMITER]
 * @include [CommonWriteDelimDocs.CommonWriteParams]
 */
@ExperimentalCsv
public fun AnyFrame.writeTsv(
    file: File,
    delimiter: Char = TSV_DELIMITER,
    includeHeader: Boolean = INCLUDE_HEADER,
    quote: Char? = QUOTE,
    quoteMode: QuoteMode = QUOTE_MODE,
    escapeChar: Char? = ESCAPE_CHAR,
    commentChar: Char? = COMMENT_CHAR,
    headerComments: List<String> = HEADER_COMMENTS,
    recordSeparator: String = RECORD_SEPARATOR,
): Unit =
    writeDelimImpl(
        df = this,
        writer = FileWriter(file),
        delimiter = delimiter,
        includeHeader = includeHeader,
        quote = quote,
        quoteMode = quoteMode,
        escapeChar = escapeChar,
        commentChar = commentChar,
        headerComments = headerComments,
        recordSeparator = recordSeparator,
        adjustCsvFormat = ADJUST_CSV_FORMAT,
    )

/**
 * @include [CommonWriteDelimDocs.TsvDocs]
 * @set [CommonWriteDelimDocs.WriteOrConvertArg] Write
 * @set [CommonWriteDelimDocs.DataTitleArg] File
 * @set [CommonWriteDelimDocs.DataArg] file
 * @include [PATH_WRITE]
 * @include [TSV_DELIMITER]
 * @include [CommonWriteDelimDocs.CommonWriteParams]
 */
@ExperimentalCsv
public fun AnyFrame.writeTsv(
    path: String,
    delimiter: Char = TSV_DELIMITER,
    includeHeader: Boolean = INCLUDE_HEADER,
    quote: Char? = QUOTE,
    quoteMode: QuoteMode = QUOTE_MODE,
    escapeChar: Char? = ESCAPE_CHAR,
    commentChar: Char? = COMMENT_CHAR,
    headerComments: List<String> = HEADER_COMMENTS,
    recordSeparator: String = RECORD_SEPARATOR,
): Unit =
    writeDelimImpl(
        df = this,
        writer = FileWriter(path),
        delimiter = delimiter,
        includeHeader = includeHeader,
        quote = quote,
        quoteMode = quoteMode,
        escapeChar = escapeChar,
        commentChar = commentChar,
        headerComments = headerComments,
        recordSeparator = recordSeparator,
        adjustCsvFormat = ADJUST_CSV_FORMAT,
    )

/**
 * {@comment only one with adjustCsvFormat}
 * @include [CommonWriteDelimDocs.TsvDocs]
 * @set [CommonWriteDelimDocs.WriteOrConvertArg] Write
 * @set [CommonWriteDelimDocs.DataTitleArg] Appendable
 * @set [CommonWriteDelimDocs.DataArg] [Appendable]
 * @include [WRITER_WRITE]
 * @include [TSV_DELIMITER]
 * @include [CommonWriteDelimDocs.CommonWriteParams]
 * @include [ADJUST_CSV_FORMAT]
 */
@ExperimentalCsv
public fun AnyFrame.writeTsv(
    writer: Appendable,
    delimiter: Char = TSV_DELIMITER,
    includeHeader: Boolean = INCLUDE_HEADER,
    quote: Char? = QUOTE,
    quoteMode: QuoteMode = QUOTE_MODE,
    escapeChar: Char? = ESCAPE_CHAR,
    commentChar: Char? = COMMENT_CHAR,
    headerComments: List<String> = HEADER_COMMENTS,
    recordSeparator: String = RECORD_SEPARATOR,
    adjustCsvFormat: AdjustCSVFormat = ADJUST_CSV_FORMAT,
): Unit =
    writeDelimImpl(
        df = this,
        writer = writer,
        delimiter = delimiter,
        includeHeader = includeHeader,
        quote = quote,
        quoteMode = quoteMode,
        escapeChar = escapeChar,
        commentChar = commentChar,
        headerComments = headerComments,
        recordSeparator = recordSeparator,
        adjustCsvFormat = adjustCsvFormat,
    )
