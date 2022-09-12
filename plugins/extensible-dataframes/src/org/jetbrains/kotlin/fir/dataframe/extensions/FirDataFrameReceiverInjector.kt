/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.*
import org.jetbrains.kotlin.fir.dataframe.Names.COLUM_GROUP_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.Names.DF_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.loadInterpreter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn

class SchemaContext(val properties: List<SchemaProperty>)

class FirDataFrameReceiverInjector(
    session: FirSession,
    private val state: MutableMap<ClassId, SchemaContext>,
    private val ids: ArrayDeque<ClassId>
) : FirExpressionResolutionExtension(session), KotlinTypeFacade {

    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        return coneKotlinTypes(functionCall, state, ids, id)
    }

    object DataFramePluginKey : GeneratedDeclarationKey()
}

typealias RootMarkerStrategy = KotlinTypeFacade.(FirFunctionCall) -> ConeTypeProjection


val id: RootMarkerStrategy = {
    val callReturnType = it.typeRef.coneTypeSafe<ConeClassLikeType>()
    callReturnType!!.typeArguments[0]
}

val any: RootMarkerStrategy = {
    session.builtinTypes.anyType.type
}

fun KotlinTypeFacade.coneKotlinTypes(
    functionCall: FirFunctionCall,
    state: MutableMap<ClassId, SchemaContext>,
    ids: ArrayDeque<ClassId>,
    getRootMarker: KotlinTypeFacade.(FirFunctionCall) -> ConeTypeProjection,
    reporter: InterpretationErrorReporter = InterpretationErrorReporter { _, _ -> }
): List<ConeKotlinType> {
    val callReturnType = functionCall.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return emptyList()
    if (callReturnType.classId != DF_CLASS_ID) return emptyList()
    val processor = functionCall.loadInterpreter(session) ?: return emptyList()

    val dataFrameSchema =
        interpret(functionCall, processor, reporter = reporter)?.let { it.value as PluginDataFrameSchema } ?: return emptyList()

    val types: MutableList<ConeClassLikeType> = mutableListOf()

    // TODO: generate a new marker for each call when there is an API to cast functionCall result to this type
    val rootMarker = getRootMarker(functionCall)
    fun PluginDataFrameSchema.materialize(rootMarker: ConeTypeProjection? = null): ConeTypeProjection {
        val id = ids.removeLast()
        val marker = rootMarker ?: ConeClassLikeLookupTagImpl(id)
            .constructClassType(emptyArray(), isNullable = false)
        val properties = columns().map {
            @Suppress("USELESS_IS_CHECK")
            when (it) {
                is SimpleColumnGroup -> {
                    val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize()
                    val columnsContainerReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(COLUM_GROUP_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = false
                        )

                    val dataRowReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = false
                        )

                    SchemaProperty(marker, it.name, dataRowReturnType, columnsContainerReturnType)
                }

                is SimpleFrameColumn -> {
                    val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize()
                    val frameColumnReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(DF_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = it.nullable
                        )

                    SchemaProperty(
                        marker = marker,
                        name = it.name,
                        dataRowReturnType = frameColumnReturnType,
                        columnContainerReturnType = frameColumnReturnType.toFirResolvedTypeRef().projectOverDataColumnType()
                    )
                }

                is SimpleCol -> SchemaProperty(
                    marker = marker,
                    name = it.name,
                    dataRowReturnType = it.type.type(),
                    columnContainerReturnType = it.type.type().toFirResolvedTypeRef().projectOverDataColumnType()
                )

                else -> TODO("shouldn't happen")
            }
        }
        state[id] = SchemaContext(properties)
        types += ConeClassLikeLookupTagImpl(id).constructClassType(emptyArray(), isNullable = false)
        return marker
    }

    dataFrameSchema.materialize(rootMarker)
    return types
}

fun FirFunctionCall.functionSymbol(): FirNamedFunctionSymbol {
    val firResolvedNamedReference = calleeReference as FirResolvedNamedReference
    return firResolvedNamedReference.resolvedSymbol as FirNamedFunctionSymbol
}

class Arguments(val refinedArguments: List<RefinedArgument>) : List<RefinedArgument> by refinedArguments

data class RefinedArgument(val name: Name, val expression: FirExpression) {

    override fun toString(): String {
        return "RefinedArgument(name=$name, expression=${expression})"
    }
}

data class SchemaProperty(
    val marker: ConeTypeProjection,
    val name: String,
    val dataRowReturnType: ConeKotlinType,
    val columnContainerReturnType: ConeKotlinType,
    val override: Boolean = false
)