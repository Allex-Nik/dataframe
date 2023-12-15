package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.AllExceptColumnsSelectionDsl.Usage.ColumnGroupName
import org.jetbrains.kotlinx.dataframe.api.AllExceptColumnsSelectionDsl.Usage.ColumnSetName
import org.jetbrains.kotlinx.dataframe.api.AllExceptColumnsSelectionDsl.Usage.PlainDslName
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnSet
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnsResolver
import org.jetbrains.kotlinx.dataframe.columns.SingleColumn
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.documentation.Indent
import org.jetbrains.kotlinx.dataframe.documentation.LineBreak
import org.jetbrains.kotlinx.dataframe.documentation.UsageTemplateColumnsSelectionDsl.UsageTemplate
import org.jetbrains.kotlinx.dataframe.impl.aggregation.toColumns
import org.jetbrains.kotlinx.dataframe.impl.columns.allColumnsExceptKeepingStructure
import org.jetbrains.kotlinx.dataframe.impl.columns.changePath
import org.jetbrains.kotlinx.dataframe.impl.columns.createColumnSet
import org.jetbrains.kotlinx.dataframe.impl.columns.transformSingle
import org.jetbrains.kotlinx.dataframe.util.ALL_COLS_EXCEPT
import org.jetbrains.kotlinx.dataframe.util.ALL_COLS_REPLACE
import org.jetbrains.kotlinx.dataframe.util.ALL_COLS_REPLACE_VARARG
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_EXCEPT
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_EXCEPT_REPLACE_RESOLVER
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_EXCEPT_REPLACE_SELECTOR
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_SINGLE_COL_EXCEPT
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHER
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHERS
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_RESOLVER
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_RESOLVERS
import org.jetbrains.kotlinx.dataframe.util.COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_SELECTOR
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty

// region ColumnsSelectionDsl

public interface AllExceptColumnsSelectionDsl {

    /**
     * ## (All) (Cols) Except Usage
     *
     * @include [UsageTemplate]
     * {@setArg [UsageTemplate.DefinitionsArg]
     *  {@include [UsageTemplate.ColumnSetDef]}
     *  {@include [LineBreak]}
     *  {@include [UsageTemplate.ColumnGroupDef]}
     *  {@include [LineBreak]}
     *  {@include [UsageTemplate.ColumnsSelectorDef]}
     *  {@include [LineBreak]}
     *  {@include [UsageTemplate.ColumnDef]}
     *  {@include [LineBreak]}
     *  {@include [UsageTemplate.ColumnNoAccessorDef]}
     *  {@include [LineBreak]}
     *  {@include [UsageTemplate.ColumnsResolverDef]}
     * }
     *
     * {@setArg [UsageTemplate.PlainDslFunctionsArg]
     *  {@include [PlainDslName]} **`{ `**{@include [UsageTemplate.ColumnsSelectorRef]}**` \\}`**
     *
     *  `|` {@include [PlainDslName]}**`(`**{@include [UsageTemplate.ColumnRef]}**`, ..)`**
     * }
     * {@setArg [UsageTemplate.ColumnSetFunctionsArg]
     *  {@include [Indent]}{@include [ColumnSetName]} `[`**` { `**`]` {@include [UsageTemplate.ColumnsResolverRef]} `[`**` \\} `**`]`
     *
     *  {@include [Indent]}`|` {@include [ColumnSetName]} {@include [UsageTemplate.ColumnRef]}
     *
     *  {@include [Indent]}`|` .{@include [ColumnSetName]}**`(`**{@include [UsageTemplate.ColumnRef]}**`, ..)`**
     * }
     * {@setArg [UsageTemplate.ColumnGroupFunctionsArg]
     *  {@include [Indent]}{@include [ColumnGroupName]} **` { `**{@include [UsageTemplate.ColumnsSelectorRef]}**` \\} `**
     *
     *  {@include [Indent]}`|` {@include [ColumnGroupName]}**`(`**{@include [UsageTemplate.ColumnNoAccessorRef]}**`, ..)`**
     * }
     */
    public interface Usage {

        /** [**allExcept**][ColumnsSelectionDsl.allExcept] */
        public interface PlainDslName

        /** [**except**][ColumnsSelectionDsl.except] */
        public interface ColumnSetName

        /** .[**allColsExcept**][ColumnsSelectionDsl.allColsExcept] */
        public interface ColumnGroupName
    }

    /**
     * ## (All) (Cols) Except
     *
     * Exclude the given columns from the current selection.
     *
     * ### On [ColumnSets][ColumnSet]
     * This function can be explained the easiest with [ColumnSets][ColumnSet]. Let's say we want all
     * integer columns apart from `age` and `height`.
     *
     * We can do:
     *
     * `df.`[select][DataFrame.select]` { `[colsOf][colsOf]`<`[Int][Int]`>() `[except][ColumnSet.except]` { age `[and][ColumnsSelectionDsl.and]` height } }`
     *
     * Which will remove the [ColumnSet] created by `age `[and][ColumnsSelectionDsl.and]` height` from the [ColumnSet] created by [colsOf][colsOf]`<`[Int][Int]`>()`.
     * {@include [LineBreak]}
     * This operation can also be done with columns that are from inside a [ColumnGroup].
     *
     * For instance:
     *
     * `df.`[select][DataFrame.select]` { `[colsAtAnyDepth][ColumnsSelectionDsl.colsAtAnyDepth]` { "a" `[in][String.contains]` it.`[name][DataColumn.name]`() } `[except][ColumnSet.except]` userData.age }`
     * {@include [LineBreak]}
     * NOTE: If a column that needs to be removed appears multiple times, it is excepted
     * each time it is encountered (including inside [ColumnGroups][ColumnGroup]). You could say the receiver [ColumnSet]
     * is [simplified][ColumnsSelectionDsl.simplify] before the operation is performed:
     *
     * `(`[cols][ColumnsSelectionDsl.cols]`(userData, userData, userData.age, userData.age) `[except][ColumnSet.except]` userData.age) == (`[cols][ColumnsSelectionDsl.cols]`(userData) `[except][ColumnSet.except]` userData.age)`
     *
     * ### In the [ColumnsSelectionDsl][ColumnsSelectionDsl]
     * Instead of having to write [all][ColumnsSelectionDsl.all]`() `[except][ColumnsSelectionDsl.except]` { ... }` in the DSL,
     * you can use [allExcept][ColumnsSelectionDsl.allExcept]` { ... }` to achieve the same result.
     *
     * For example:
     *
     * `df.`[select][DataFrame.select]` { `[allExcept][ColumnsSelectionDsl.allExcept]` { userData.age `[and][ColumnsSelectionDsl.and]` height } }`
     *
     * ### On [ColumnGroups][ColumnGroup]
     * The variant of this function on [ColumnGroups][ColumnGroup] is a bit different as it operates on the columns
     * inside the group instead of in the DSL-scope. In other words, `myColGroup.`[allColsExcept][SingleColumn.allColsExcept]` { col } ` is
     * a shortcut for `myColGroup.`[select][ColumnsSelectionDsl.select]` { `[all][ColumnsSelectionDsl.all]`() `[except][ColumnSet]` col }`.
     *
     * Also note the name change, similar to [allCols][ColumnsSelectionDsl.allCols], this makes it clearer that your operating
     * on the columns in the group.
     *
     * ### Examples for this overload
     * {@getArg [ExampleArg]}
     *
     * {@getArg [ParamArg]}
     * @return A [ColumnSet] containing all columns in [this\] except the specified ones.
     */
    private interface CommonExceptDocs {

        /* Example argument */
        interface ExampleArg

        /* Parameter argument  */
        interface ParamArg
    }

    // region ColumnSet

    /**
     * @include [CommonExceptDocs]
     * {@setArg [CommonExceptDocs.ExampleArg]
     *  `df.`[select][ColumnsSelectionDsl.select] `{` [colsOf][ColumnsSelectionDsl.colsOf]`<`[Number][Number]`>()` [except][ColumnSet.except] {@getArg [ArgumentArg1]} `\\\\}`
     *
     *  `df.`[select][ColumnsSelectionDsl.select] `{` [cols][ColumnsSelectionDsl.cols]`(name, age)` [except][ColumnSet.except] {@getArg [ArgumentArg2]} `\\\\}`
     * }
     */
    private interface ColumnSetInfixDocs {

        /* argument */
        interface ArgumentArg1

        /* argument */
        interface ArgumentArg2
    }

    /**
     * @include [CommonExceptDocs]
     * {@setArg [CommonExceptDocs.ExampleArg]
     *  `df.`[select][ColumnsSelectionDsl.select] `{` [colsOf][ColumnsSelectionDsl.colsOf]`<`[Number][Number]`>().`[except][ColumnSet.except]{@getArg [ArgumentArg1]} `\\\\}`
     *
     *  `df.`[select][ColumnsSelectionDsl.select] `{` [cols][ColumnsSelectionDsl.cols]`(name, age).`[except][ColumnSet.except]{@getArg [ArgumentArg2]} `\\\\}`
     * }
     */
    private interface ColumnSetVarargDocs {

        /* argument */
        interface ArgumentArg1

        /* argument */
        interface ArgumentArg2
    }

    /**
     * @include [ColumnSetInfixDocs]
     * @setArg [CommonExceptDocs.ParamArg] @param [selector\] A lambda in which you specify the columns that need to be
     *   excluded from the [ColumnSet]. The scope of the selector is the same as the outer scope.
     * @setArg [ColumnSetInfixDocs.ArgumentArg1] `{ "age" `[and][ColumnsSelectionDsl.and]` height }`
     * @setArg [ColumnSetInfixDocs.ArgumentArg2] `{ name.firstName }`
     */
    public infix fun <C> ColumnSet<C>.except(selector: () -> ColumnsResolver<*>): ColumnSet<C> =
        except(selector())

    /**
     * @include [ColumnSetInfixDocs]
     * @setArg [CommonExceptDocs.ParamArg] @param [other\] A [ColumnsResolver] containing the columns that need to be
     *   excluded from the [ColumnSet].
     * @setArg [ColumnSetInfixDocs.ArgumentArg1] `("age" `[and][ColumnsSelectionDsl.and]` height)`
     * @setArg [ColumnSetInfixDocs.ArgumentArg2] `(name.firstName)`
     */
    public infix fun <C> ColumnSet<C>.except(other: ColumnsResolver<*>): ColumnSet<C> =
        exceptInternal(other)

    /**
     * @include [ColumnSetVarargDocs]
     * @setArg [CommonExceptDocs.ParamArg] @param [other\] Any number of [ColumnsResolvers][ColumnsResolver] containing
     *  the columns that need to be excluded from the [ColumnSet].
     * @setArg [ColumnSetVarargDocs.ArgumentArg1] `(age, userData.height)`
     * @setArg [ColumnSetVarargDocs.ArgumentArg2] `(name.firstName, name.middleName)`
     */
    public fun <C> ColumnSet<C>.except(vararg other: ColumnsResolver<*>): ColumnSet<C> =
        except(other.toColumnSet())

    public infix fun <C> ColumnSet<C>.except(other: String): ColumnSet<C> =
        except(column<Any?>(other))

    public fun <C> ColumnSet<C>.except(vararg others: String): ColumnSet<C> =
        except(others.toColumnSet())

    public infix fun <C> ColumnSet<C>.except(other: KProperty<C>): ColumnSet<C> =
        except(column(other))

    public fun <C> ColumnSet<C>.except(vararg others: KProperty<C>): ColumnSet<C> =
        except(others.toColumnSet())

    public infix fun <C> ColumnSet<C>.except(other: ColumnPath): ColumnSet<C> =
        except(column<Any?>(other))

    public fun <C> ColumnSet<C>.except(vararg others: ColumnPath): ColumnSet<C> =
        except(others.toColumnSet())

    // endregion

    // region ColumnsSelectionDsl

    public fun <C> ColumnsSelectionDsl<C>.allExcept(selector: ColumnsSelector<C, *>): ColumnSet<*> =
        this.asSingleColumn().allColsExcept(selector)

    // no scoping issues, this can exist for legacy purposes
    public fun ColumnsSelectionDsl<*>.allExcept(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        asSingleColumn().allColsExceptInternal(others.toColumnSet())

    public fun ColumnsSelectionDsl<*>.allExcept(vararg others: String): ColumnSet<*> =
        asSingleColumn().allColsExceptInternal(others.toColumnSet())

    public fun ColumnsSelectionDsl<*>.allExcept(vararg others: KProperty<*>): ColumnSet<*> =
        asSingleColumn().allColsExceptInternal(others.toColumnSet())

    public fun ColumnsSelectionDsl<*>.allExcept(vararg others: ColumnPath): ColumnSet<*> =
        asSingleColumn().allColsExceptInternal(others.toColumnSet())

    // endregion

    // region SingleColumn

    public fun <C> SingleColumn<DataRow<C>>.allColsExcept(selector: ColumnsSelector<C, *>): ColumnSet<*> =
        allColsExceptInternal(selector.toColumns())

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun SingleColumn<DataRow<*>>.allColsExcept(other: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { other }

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE_VARARG),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun SingleColumn<DataRow<*>>.allColsExcept(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { others.toColumnSet() }

    public fun SingleColumn<DataRow<*>>.allColsExcept(vararg others: String): ColumnSet<*> =
        allColsExceptInternal(others.toColumnSet())

    public fun SingleColumn<DataRow<*>>.allColsExcept(vararg others: KProperty<*>): ColumnSet<*> =
        allColsExceptInternal(others.toColumnSet())

    public fun SingleColumn<DataRow<*>>.allColsExcept(vararg other: ColumnPath): ColumnSet<*> =
        allColsExceptInternal(other.toColumnSet())

    // endregion

    // region String

    public fun String.allColsExcept(selector: ColumnsSelector<*, *>): ColumnSet<*> =
        columnGroup(this).allColsExcept(selector)

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun String.allColsExcept(other: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { other }

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE_VARARG),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun String.allColsExcept(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { others.toColumnSet() }

    public fun String.allColsExcept(vararg others: String): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    public fun String.allColsExcept(vararg others: KProperty<*>): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    public fun String.allColsExcept(vararg others: ColumnPath): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    // endregion

    // region KProperty

    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    // TODO: [KT-64092](https://youtrack.jetbrains.com/issue/KT-64092/OVERLOADRESOLUTIONAMBIGUITY-caused-by-lambda-argument)
    public fun <C> KProperty<C>.allColsExcept(selector: ColumnsSelector<C, *>): ColumnSet<*> =
        columnGroup(this).allColsExcept(selector)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowAllColsExcept")
    public fun <C> KProperty<DataRow<C>>.allColsExcept(selector: ColumnsSelector<C, *>): ColumnSet<*> =
        columnGroup(this).allColsExcept(selector)

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun KProperty<*>.allColsExcept(other: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { other }

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE_VARARG),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun KProperty<*>.allColsExcept(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { others.toColumnSet() }

    public fun KProperty<*>.allColsExcept(vararg others: String): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    public fun KProperty<*>.allColsExcept(vararg others: KProperty<*>): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    public fun KProperty<*>.allColsExcept(vararg others: ColumnPath): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    // endregion

    // region ColumnPath

    public fun ColumnPath.allColsExcept(selector: ColumnsSelector<*, *>): ColumnSet<*> =
        columnGroup(this).allColsExcept(selector)

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun ColumnPath.allColsExcept(other: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { other }

    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith(ALL_COLS_REPLACE_VARARG),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun ColumnPath.allColsExcept(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { others.toColumnSet() }

    public fun ColumnPath.allColsExcept(vararg others: String): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    public fun ColumnPath.allColsExcept(vararg others: KProperty<*>): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    public fun ColumnPath.allColsExcept(vararg others: ColumnPath): ColumnSet<*> =
        columnGroup(this).allColsExceptInternal(others.toColumnSet())

    // endregion

    // region experiments

    @ExperimentalExceptCsDsl
    public infix fun <C> SingleColumn<DataRow<C>>.exceptNew(selector: ColumnsSelector<C, *>): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(selector.toColumns())

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { other }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public infix fun <C> SingleColumn<DataRow<C>>.exceptNew(other: ColumnsResolver<*>): SingleColumn<DataRow<C>> =
        exceptNew { other }

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { others.toColumnSet() }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun <C> SingleColumn<DataRow<C>>.exceptNew(vararg others: ColumnsResolver<*>): SingleColumn<DataRow<C>> =
        exceptNew { others.toColumnSet() }

    @ExperimentalExceptCsDsl
    public infix fun <C> SingleColumn<DataRow<C>>.exceptNew(other: String): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun <C> SingleColumn<DataRow<C>>.exceptNew(vararg others: String): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun <C> SingleColumn<DataRow<C>>.exceptNew(other: KProperty<C>): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(column(other))

    @ExperimentalExceptCsDsl
    public fun <C> SingleColumn<DataRow<C>>.exceptNew(vararg others: KProperty<*>): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun <C> SingleColumn<DataRow<C>>.exceptNew(other: ColumnPath): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun <C> SingleColumn<DataRow<C>>.exceptNew(vararg others: ColumnPath): SingleColumn<DataRow<C>> =
        exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun String.exceptNew(selector: ColumnsSelector<*, *>): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptNew(selector)

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { other }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public infix fun String.exceptNew(other: ColumnsResolver<*>): SingleColumn<DataRow<*>> =
        exceptNew { other }

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { others.toColumnSet() }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun String.exceptNew(vararg others: ColumnsResolver<*>): SingleColumn<DataRow<*>> =
        exceptNew { others.toColumnSet() }

    @ExperimentalExceptCsDsl
    public infix fun String.exceptNew(other: String): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun String.exceptNew(vararg others: String): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun String.exceptNew(other: KProperty<*>): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(column(other))

    @ExperimentalExceptCsDsl
    public fun String.exceptNew(vararg others: KProperty<*>): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun String.exceptNew(other: ColumnPath): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun String.exceptNew(vararg others: ColumnPath): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    // TODO: [KT-64092](https://youtrack.jetbrains.com/issue/KT-64092/OVERLOADRESOLUTIONAMBIGUITY-caused-by-lambda-argument)
    public infix fun <C> KProperty<C>.exceptNew(selector: ColumnsSelector<C, *>): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(selector.toColumns())

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public infix fun <C> KProperty<DataRow<C>>.exceptNew(selector: ColumnsSelector<C, *>): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(selector.toColumns())

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { other }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public infix fun KProperty<*>.exceptNew(other: ColumnsResolver<*>): SingleColumn<DataRow<*>> =
        exceptNew { other }

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { others.toColumnSet() }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun KProperty<*>.exceptNew(vararg others: ColumnsResolver<*>): SingleColumn<DataRow<*>> =
        exceptNew { others.toColumnSet() }

    @ExperimentalExceptCsDsl
    public infix fun <C> KProperty<C>.exceptNew(other: String): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun <C> KProperty<C>.exceptNew(vararg others: String): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public infix fun <C> KProperty<DataRow<C>>.exceptNew(other: String): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public fun <C> KProperty<DataRow<C>>.exceptNew(vararg others: String): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun <C> KProperty<C>.exceptNew(other: KProperty<*>): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(column(other))

    @ExperimentalExceptCsDsl
    public fun <C> KProperty<C>.exceptNew(vararg others: KProperty<*>): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public infix fun <C> KProperty<DataRow<C>>.exceptNew(other: KProperty<*>): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(column(other))

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public fun <C> KProperty<DataRow<C>>.exceptNew(vararg others: KProperty<*>): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun <C> KProperty<C>.exceptNew(other: ColumnPath): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun <C> KProperty<C>.exceptNew(vararg others: ColumnPath): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public infix fun <C> KProperty<DataRow<C>>.exceptNew(other: ColumnPath): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("KPropertyDataRowExceptNew")
    public fun <C> KProperty<DataRow<C>>.exceptNew(vararg others: ColumnPath): SingleColumn<DataRow<C>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun ColumnPath.exceptNew(selector: ColumnsSelector<*, *>): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(selector.toColumns<Any?, Any?>())

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { other }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public infix fun ColumnPath.exceptNew(other: ColumnsResolver<*>): SingleColumn<DataRow<*>> =
        exceptNew { other }

    @ExperimentalExceptCsDsl
    @Deprecated(
        message = ALL_COLS_EXCEPT,
        replaceWith = ReplaceWith("exceptNew { others.toColumnSet() }"),
        level = DeprecationLevel.ERROR,
    ) // present solely to redirect users to the right function
    public fun ColumnPath.exceptNew(vararg others: ColumnsResolver<*>): SingleColumn<DataRow<*>> =
        exceptNew { others.toColumnSet() }

    @ExperimentalExceptCsDsl
    public infix fun ColumnPath.exceptNew(other: String): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun ColumnPath.exceptNew(vararg others: String): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun ColumnPath.exceptNew(other: KProperty<*>): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(column(other))

    @ExperimentalExceptCsDsl
    public fun ColumnPath.exceptNew(vararg others: KProperty<*>): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    @ExperimentalExceptCsDsl
    public infix fun ColumnPath.exceptNew(other: ColumnPath): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(column<Any?>(other))

    @ExperimentalExceptCsDsl
    public fun ColumnPath.exceptNew(vararg others: ColumnPath): SingleColumn<DataRow<*>> =
        columnGroup(this).exceptExperimentalInternal(others.toColumnSet())

    // endregion

    // region deprecated

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_SELECTOR),
        level = DeprecationLevel.WARNING,
    )
    public infix fun <C> SingleColumn<DataRow<C>>.except(selector: ColumnsSelector<C, *>): ColumnSet<*> =
        allColsExcept(selector)

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_RESOLVER),
        level = DeprecationLevel.WARNING,
    )
    public infix fun SingleColumn<DataRow<*>>.except(other: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { other }

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_RESOLVERS),
        level = DeprecationLevel.WARNING,
    )
    public fun SingleColumn<DataRow<*>>.except(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        allColsExcept { others.toColumnSet() }

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHER),
        level = DeprecationLevel.WARNING,
    )
    public infix fun SingleColumn<DataRow<*>>.except(other: String): ColumnSet<*> =
        allColsExcept(other)

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHERS),
        level = DeprecationLevel.WARNING,
    )
    public fun SingleColumn<DataRow<*>>.except(vararg others: String): ColumnSet<*> =
        allColsExcept(*others)

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHER),
        level = DeprecationLevel.WARNING,
    )
    public infix fun SingleColumn<DataRow<*>>.except(other: KProperty<*>): ColumnSet<*> =
        allColsExcept(other)

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHERS),
        level = DeprecationLevel.WARNING,
    )
    public fun SingleColumn<DataRow<*>>.except(vararg others: KProperty<*>): ColumnSet<*> =
        allColsExcept(*others)

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHER),
        level = DeprecationLevel.WARNING,
    )
    public infix fun SingleColumn<DataRow<*>>.except(other: ColumnPath): ColumnSet<*> =
        allColsExcept(other)

    @Deprecated(
        message = COL_SELECT_DSL_SINGLE_COL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_SINGLE_COL_EXCEPT_REPLACE_OTHERS),
        level = DeprecationLevel.WARNING,
    )
    public fun SingleColumn<DataRow<*>>.except(vararg others: ColumnPath): ColumnSet<*> =
        allColsExcept(*others)

    @Deprecated(
        message = COL_SELECT_DSL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_EXCEPT_REPLACE_SELECTOR),
        level = DeprecationLevel.WARNING,
    )
    public fun <C> ColumnsSelectionDsl<C>.except(selector: ColumnsSelector<C, *>): ColumnSet<*> =
        allExcept(selector)

    @Deprecated(
        message = COL_SELECT_DSL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_EXCEPT_REPLACE_RESOLVER),
        level = DeprecationLevel.WARNING,
    )
    public fun ColumnsSelectionDsl<*>.except(vararg others: ColumnsResolver<*>): ColumnSet<*> =
        allExcept(*others)

    @Deprecated(
        message = COL_SELECT_DSL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_EXCEPT_REPLACE_RESOLVER),
        level = DeprecationLevel.WARNING,
    )
    public fun ColumnsSelectionDsl<*>.except(vararg others: String): ColumnSet<*> =
        allExcept(*others)

    @Deprecated(
        message = COL_SELECT_DSL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_EXCEPT_REPLACE_RESOLVER),
        level = DeprecationLevel.WARNING,
    )
    public fun ColumnsSelectionDsl<*>.except(vararg others: KProperty<*>): ColumnSet<*> =
        allExcept(*others)

    @Deprecated(
        message = COL_SELECT_DSL_EXCEPT,
        replaceWith = ReplaceWith(COL_SELECT_DSL_EXCEPT_REPLACE_RESOLVER),
        level = DeprecationLevel.WARNING,
    )
    public fun ColumnsSelectionDsl<*>.except(vararg others: ColumnPath): ColumnSet<*> =
        allExcept(*others)

    // endregion
}

/**
 * Removes the columns in the "other" ColumnsResolver from the current ColumnSet while keeping the structure intact.
 * Returns a new ColumnSet with the remaining columns.
 *
 * @param other The ColumnsResolver containing the columns to be removed.
 * @return The new ColumnSet with the remaining columns.
 */
@Suppress("UNCHECKED_CAST")
internal fun <C> ColumnSet<C>.exceptInternal(other: ColumnsResolver<*>): ColumnSet<C> =
    createColumnSet { context ->
        val resolvedCols = this.resolve(context)
        val resolvedColsToExcept = other.resolve(context)
        resolvedCols.allColumnsExceptKeepingStructure(resolvedColsToExcept)
    } as ColumnSet<C>

/**
 * Returns a new ColumnSet that contains all columns from inside the receiver column group
 * except those specified in the "other" ColumnsResolver.
 *
 * @param other The ColumnsResolver containing the columns to be removed.
 * @return The new ColumnSet with the remaining columns.
 */
internal fun SingleColumn<DataRow<*>>.allColsExceptInternal(other: ColumnsResolver<*>): ColumnSet<Any?> =
    selectInternal { all().exceptInternal(other) }

/**
 * Returns a new SingleColumn<DataRow<C>> that has the same structure as the receiver, but excludes columns
 * specified in the "other" ColumnsResolver.
 *
 * @param other The [ColumnsResolver] to use for excluding columns.
 * @return A new [SingleColumn] with the filtered columns excluded.
 */
internal fun <C> SingleColumn<DataRow<C>>.exceptExperimentalInternal(other: ColumnsResolver<*>): SingleColumn<DataRow<C>> =
    this.ensureIsColumnGroup().transformSingle { singleCol ->
        val columnsToExcept = singleCol.asColumnGroup()
            .getColumnsWithPaths { other }
            .map { it.changePath(singleCol.path + it.path) }

        val newCols = listOf(singleCol).allColumnsExceptKeepingStructure(columnsToExcept)

        newCols as List<ColumnWithPath<DataRow<*>>>
    }.singleInternal() as SingleColumn<DataRow<C>>

/**
 * Functions annotated with this annotation are experimental and will be removed or renamed in the future.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.FUNCTION)
public annotation class ExperimentalExceptCsDsl

// endregion
