import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.jetbrains.dataframe.impl.codeGen.CodeGenerator
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.annotations.GenerateConstructor
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.pluginJsonFormat
import org.jetbrains.kotlinx.dataframe.plugin.testing.schemaRender.toPluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.schema.ColumnSchema
import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.junit.jupiter.api.Test
import java.util.*

class Prototype {

    /**
     * also this:
     * @see DataFrame.distinctBy
     */
    @Test
    fun `print link to the sources`() {
        println(".(mappings.kt:5)")
        println(".(move.kt:5)")
        println("kotlin/org/jetbrains/kotlinx/dataframe/plugin/ColumnAccessorApproximation.kt :10")
        println("/home/nikitak/IdeaProjects/dataframe/core/src/main/kotlin/org/jetbrains/kotlinx/dataframe/plugin/ColumnAccessorApproximation.kt :10")

    }

    @DataSchema
    class Function(
        val receiverType: String,
        val function: String,
        val functionReturnType: Type,
        val parameters: List<Parameter>
    ) : DataRowSchema {
    }

    @DataSchema
    class Parameter(
        val name: String,
        val returnType: Type,
        val defaultValue: String?,
    ) : DataRowSchema

    @DataSchema
    data class Type(val name: String, val vararg: Boolean)

    val id by column<Int>()

    val otherFunctions = dataFrameOf(
        Function("DataFrame<T>", "insert", Type("InsertClause<T>", false), listOf(Parameter("column", Type("DataColumn<C>", false), null))),
        Function("DataFrame<T>", "insert", Type("InsertClause<T>", false), listOf(
            Parameter("name", Type("String", false), null),
            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
            Parameter("expression", Type("RowExpression<T, R>", false), null)
        )),
        Function("DataFrame<T>", "insert", Type("InsertClause<T>", false), listOf(
            Parameter("column", Type("ColumnAccessor<R>", false), null),
            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
            Parameter("expression", Type("RowExpression<T, R>", false), null)
        )),
        Function("DataFrame<T>", "insert", Type("InsertClause<T>", false), listOf(
            Parameter("column", Type("KProperty<R>", false), null),
            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
            Parameter("expression", Type("RowExpression<T, R>", false), null)
        )),
    )

    val dfFunctions = dataFrameOf(
        /**
         * @see InsertClause.under
         */
        Function("InsertClause<T>", "under", Type("DataFrame<T>", false), listOf(
            Parameter("column", Type("ColumnSelector<T, *>", false), null)
        )),
        Function("InsertClause<T>", "under", Type("DataFrame<T>", false), listOf(
            Parameter("columnPath", Type("ColumnPath", false), null)
        )),
        Function("InsertClause<T>", "under", Type("DataFrame<T>", false), listOf(
            Parameter("column", Type("ColumnAccessor<*>", false), null)
        )),
        Function("InsertClause<T>", "under", Type("DataFrame<T>", false), listOf(
            Parameter("column", Type("KProperty<*>", false), null)
        )),
        Function("InsertClause<T>", "under", Type("DataFrame<T>", false), listOf(
            Parameter("column", Type("String", false), null)
        )),
//        Function("DataFrame<T>", "add", Type("DataFrame<T>", false), listOf(
//            Parameter("columns", Type("AnyBaseCol", vararg = true), null)
//        )),
//        Function("DataFrame<T>", "addAll", Type("DataFrame<T>", false), listOf(
//            Parameter("columns", Type("Iterable<AnyBaseCol>", false), null)
//        )),
//        Function("DataFrame<T>", "addAll", Type("DataFrame<T>", false), listOf(
//            Parameter("dataFrames", Type("AnyFrame", vararg = true), null)
//        )),
        Function("DataFrame<T>", "add", Type("DataFrame<T>", false), listOf(
            Parameter("name", Type("String", false), null),
            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
            Parameter("expression", Type("AddExpression<T, R>", false), null),
        )),
//        Function("DataFrame<T>", "add", Type("DataFrame<T>", false), listOf(
//            Parameter("property", Type("KProperty<R>", false), null),
//            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
//            Parameter("expression", Type("AddExpression<T, R>", false), null),
//        )),
//        Function("DataFrame<T>", "add", Type("DataFrame<T>", false), listOf(
//            Parameter("column", Type("ColumnAccessor<R>", false), null),
//            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
//            Parameter("expression", Type("AddExpression<T, R>", false), null),
//        )),
//        Function("DataFrame<T>", "add", Type("DataFrame<T>", false), listOf(
//            Parameter("path", Type("ColumnPath", false), null),
//            Parameter("infer", Type("Infer", false), "Infer.Nulls"),
//            Parameter("expression", Type("AddExpression<T, R>", false), null),
//        ))
    )

    val functions = (otherFunctions concat dfFunctions)
        .groupBy { function }
        .updateGroups { it.addId() }
        .concat()
        .update { parameters }.with { it.append(Parameter("<this>", Type(receiverType, false), null)) }

    // region classes

    @DataSchema
    interface ClassDeclaration : DataRowSchema {
        val name: String
        val parameters: List<Parameter>

        @GenerateConstructor
        companion object
    }

    // "val df: DataFrame<T>, val column: AnyCol"
    val classes = dataFrameOf(
        ClassDeclaration("InsertClause<T>", listOf(
            Parameter("df", Type("DataFrame<T>", false), null),
            Parameter("column", Type("AnyCol", false), null),
        )),
    )

    // endregion

    val returnType by columnGroup<Type>()

    val uniqueReturnTypes: DataFrame<Type> = functions
        .explode { parameters }
        .ungroup { parameters }[returnType]
        .concat(functions.functionReturnType)
        .concat(
            classes
                .select { parameters }
                .explode { parameters }
                .ungroup { parameters }[returnType]
        )
        .concat(classes.name.distinct().map { Type(it, false) }.asIterable().toDataFrame())
        .distinct()

    @Test
    fun `class cast exception`() {
        val returnType by column<Type>()
        // when actual returnType is in fact ColumnGroup<Type>, not DataColumn<Type>
        // compiler doesn't complain, but it's an error
        val res = functions
            .explode { parameters }
            .ungroup { parameters }[returnType]
        res.map { it.name }

        // and we can do this, still no error
        //res.concat(functions.functionReturnType.toListOf<Type>().toColumn(""))
    }

    @Test
    fun `generate bridges`() {
        val df = uniqueReturnTypes
            .rename { name }.into("name")
            .join(bridges) {
                // join keeps only left column!!
                "name".match(right.type.map(Infer.Nulls) { it.name })
            }
            //.rename { "type"["type"] }.into("name")
            .fillNulls(Bridge::supported).with { false }
//            .fillNulls(Bridge::).with { false }
            .remove("name", "vararg")
            .cast<Bridge>(verify = true)

        df.writeJson("bridges.json", prettyPrint = true)
    }

    @Test
    fun `join removes column from column group`() {
        val a by columnOf(1)
        val group by columnOf(a)

        val groupReference by columnGroup("group")
        val aa by groupReference.column<Int>("a")

        val df = dataFrameOf(a)
        val df1 = dataFrameOf(group)
        val res = df.join(df1) {
            "a".match(aa.map { it })
        }

        val res1 = df.join(df1) {
            "a".match(aa)
        }
        println(res1.schema())
    }

    @DataSchema
    class Data(val a: Int)
    class Record(val data: Data)
    @Test
    fun `cast and convert error should print schema maybe`() {
        val b by columnOf(1)
        val data by columnOf(b)
        dataFrameOf(data).cast<Record>(verify = true)
    }

    // region bridges
    val type by column<String>()
    val approximation by column<String>()
    val converter by column<String>()

//    @DataSchema
//    interface Bridge : DataRowSchema {
//        val type: String
//        val approximation: String
//        val converter: String
//        val lens: String
//        val supported: Boolean
//
//        @GenerateConstructor
//        companion object
//    }

    @DataSchema
    class Bridge(val type: Type,
                 val approximation: String,
                 val converter: String,
                 val lens: String,
                 val supported: Boolean = false) : DataRowSchema

//    @DataSchema
//    interface ValueExample : DataRowSchema {
//        val constructor: String?
//        val usage: String
//
//        @GenerateConstructor
//        companion object
//    }

    private val bridges by lazy { DataFrame.readJson("bridges.json").cast<Bridge>(verify = true) }

    // endregion

    /**
     * @see ConvertInterpreter
     */
    @Test
    fun `generate interpreters`() {
        `generate interpreters`(functions, bridges)
    }

    private fun `generate interpreters`(functions: DataFrame<Function>, bridges: DataFrame<Bridge>) {
        println(functions)

        functions
            .leftJoin(bridges) { functions.functionReturnType.match(right.type) }
            .convert { parameters }.with { it.leftJoin(bridges) { it[returnType].map(Infer.Nulls) { it.name }.match(right.type.name) } }
            .schema()
            .print()

        val interpreters = functions
            .leftJoin(bridges) {
                //functions.functionReturnType.match(type) TODO: Shouldn't compile
                functions.functionReturnType.name.match(right.type.name)
            }
            .convert { parameters }.with { it.leftJoin(bridges) { it[returnType].name.match(right.type.name) } }
            .convert { parameters }.with {
                it.add("arguments") {
                    val (name, runtimeName) = if (name == "<this>") {
                        "receiver" to "THIS"
                    } else {
                        name to ""
                    }

                    "val Arguments.${name}: ${approximation()} by ${converter()}($runtimeName)"
                }
            }
            .add("argumentsStr") {
                // generate deprecated property with name argumentStr
                it.parameters["arguments"].values().joinToString("\n") { "|   $it" }
            }
            .add("interpreterName") {
                val name = it.function.replaceFirstChar { it.uppercaseChar() }
                "$name${it[id]}"
            }
            .mapToColumn("interpreters") {
                """
                    |internal class ${it["interpreterName"]} : AbstractInterpreter<${approximation()}>() {
                        ${it["argumentsStr"]}
                    |    
                    |    override fun Arguments.interpret(): ${approximation()} {
                    |        TODO()
                    |    }
                    |}
                    """.trimMargin()
            }
        println(interpreters.values().joinToString("\n\n"))
    }

    @Test
    fun `generate approximations`() {
        val df = classes
            .leftJoin(bridges) { name.match(right.type.name) }
            .convert { parameters }.with {
                it.leftJoin(bridges) { returnType.match(right.type) }
            }
            .add("code") {
                val parameters = parameters.rows().joinToString(", ") { "val ${it.name}: ${it["approximation"]}" }
                "internal class ${approximation()}($parameters)"
            }

        println(df["code"].toList())

    }

    @Test
    fun `generate tests stubs`() {
        val name by column<String>()
        bridges
            .distinctBy { expr { type.name.substringBefore("<") } }
            .groupBy { converter }
            .updateGroups { df ->
                add(name) {
                    type.name.substringBefore("<")
                }
            }
            .concat()
            .filter { supported }
            .forEach {
                val testSubjectName = name().replaceFirstChar { it.lowercase() }
                println(name())
                println()
                val interpreterName = name()
                println("""
                    package org.jetbrains.kotlinx.dataframe.plugin.testing
                    
                    import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
                    import org.jetbrains.kotlinx.dataframe.annotations.Arguments
                    import org.jetbrains.kotlinx.dataframe.annotations.Interpretable
                    import org.jetbrains.kotlinx.dataframe.plugin.*
                    
                    @Interpretable(${interpreterName}Identity::class)
                    public fun ${testSubjectName}(v: ${type.name}): ${type.name} {
                        return v
                    }
                    
                    public class ${interpreterName}Identity : AbstractInterpreter<$approximation>() {
                        internal val Arguments.v: $approximation by $converter()
                        
                        override fun Arguments.interpret(): $approximation {
                            return v
                        }
                    }
                    
                    internal fun ${testSubjectName}Test() {
                        
                    }
                """.trimIndent())
                println()
            }
    }

    //dataFrameOf\((".+",?)+\)\(.*\)

    @Test
    fun printFunctionsThatShouldWork() {
        val supportedFunctions = functions
            .leftJoin(bridges) { functionReturnType.name.match(right.type.name) }
            .convert { parameters }.with {
                it.leftJoin(bridges) {
                    it.returnType.name.match<String>(right.type.name)
                }
            }
            .also {
                it.forEach {
                    parameters.print()
                }
            }
            .filter {
                it.parameters.all { it[Bridge::supported] }
            }

        supportedFunctions
            .remove(Bridge::supported, Bridge::lens, Bridge::converter, Bridge::approximation)
            .convert { parameters }.with { df ->
                df.map {
                    val default = it.defaultValue?.let { " = ${it}" } ?: ""
                    "${it.name}: ${it.returnType}$default"
                }
            }
            .print(valueLimit = 500)
    }

    @Test
    fun test() {
        val dfExpression = """dataFrameOf("test")(123)"""
        val repl = object : JupyterReplTestCase() {

        }
        val functionCall = """df.insert("age") { 42 }.under("test")"""
        //val df = dataFrameOf("test")(123)
        val df = repl.exec<DataFrame<*>>("val df = $dfExpression; df")
        val df1 = repl.exec("""
                try {
                    $functionCall
                } catch (e: Exception) {
                    e
                }
            """.trimIndent())
//        when (df1) {
//            is DataFrame<*> ->
//
//        }
    }

    @Test
    fun `schema1 test`() {
        generateSchemaTestStub("schema1", "DataFrame.readJson(\"functions.json\")")
    }

    @Test
    fun `schema2 test`() {
        generateSchemaTestStub("schema2", """run {
            |val name by columnOf("name")
            |val returnType by columnOf("")
            |val df = dataFrameOf(name, returnType)
            |val functions by columnOf(df)
            |val function by columnOf(name, returnType)
            |val nestedGroup by columnOf(name)
            |val group by columnOf(nestedGroup)
            |dataFrameOf(name, functions, function, group)
            |}""".trimMargin())
    }

    private fun generateSchemaTestStub(name: String, s: String) {
        val capitalizedName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val repl = object : JupyterReplTestCase() {

        }
        val df = repl.execRaw("val df = $s; df") as DataFrame<*>
        val expressions = mutableListOf<String>()
        var i = 0
        fun accept(s: String, columns: Map<String, ColumnSchema>) {
            columns.forEach { (t, u) ->
                when (u) {
                    is ColumnSchema.Frame -> {
                        accept("${s}.$t[0]", u.schema.columns)
                    }

                    is ColumnSchema.Value -> {
                        val funName = "col${i}"
                        expressions.add("fun $funName(v: ${u.type.toString()}) {}")
                        i++
                        expressions.add("${funName}(${s}.$t[0])")
                    }

                    is ColumnSchema.Group -> {
                        accept("${s}.$t", u.schema.columns)
                    }
                }
            }
        }

        val generator = CodeGenerator.create(useFqNames = false)
        val declaration =
                generator.generate(df.schema(), name = capitalizedName, fields = true, extensionProperties = true, isOpen = true)
                    .code.declarations
                    .replace(Regex("@JvmName\\(.*\"\\)"), "")
        accept("df", df.schema().columns)
        val schemaTestCode = expressions.joinToString("\n")
        println(schemaTestCode)
        repl.exec(schemaTestCode)

        val pluginSchema = df.schema().toPluginDataFrameSchema()

        val jsonString = pluginJsonFormat.encodeToString(pluginSchema)
        val decodedSchema = pluginJsonFormat.decodeFromString<PluginDataFrameSchema>(jsonString)
        decodedSchema shouldBe pluginSchema

        println("")
        println(jsonString)
        println()
        bridges.first { it.type.name == "DataFrame<T>" }.run {
            println("""
                        package org.jetbrains.kotlinx.dataframe.plugin.testing.schemaRender
                        
                        import kotlinx.serialization.decodeFromString
                        import org.jetbrains.kotlinx.dataframe.DataFrame
                        import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
                        import org.jetbrains.kotlinx.dataframe.annotations.Arguments
                        import org.jetbrains.kotlinx.dataframe.annotations.Interpretable
                        import org.jetbrains.kotlinx.dataframe.plugin.*
                        
                        @Interpretable(${capitalizedName}::class)
                        public fun $name(): DataFrame<*> {
                            return TODO("won't run")
                        }
                        
                        public class ${capitalizedName} : AbstractInterpreter<$approximation>() {
                            override fun Arguments.interpret(): $approximation {
                                return pluginJsonFormat.decodeFromString<PluginDataFrameSchema>(
                                    ${"\"\"\""}$jsonString${"\"\"\""}
                                )
                            }
                        }
                """.trimIndent())
        }
        val schemaDeclaration = declaration.lineSequence().joinToString("\n|")
        println(name)
        println()
        println("""
                |import org.jetbrains.kotlinx.dataframe.*
                |import org.jetbrains.kotlinx.dataframe.api.*
                |import org.jetbrains.kotlinx.dataframe.annotations.*
                |import org.jetbrains.kotlinx.dataframe.plugin.testing.*
                |import org.jetbrains.kotlinx.dataframe.plugin.testing.schemaRender.*
                |
                |/*
                |$schemaDeclaration
                |*/
                |
                |internal fun schemaTest() {
                |    val df = $name()
                |    ${expressions.joinToString("\n|    ")}
                |}
            """.trimMargin())
    }

    val expressions = listOf(
        """dataFrameOf("age")(17)""",
        """dataFrameOf("name", "age")("Name", 17).group("name", "age").into("person")""",
        """dataFrameOf("persons")(dataFrameOf("name", "age")("Name", 17).group("name", "age").into("person"))"""
    )

    @DataSchema
    interface TestCase : DataRowSchema {
        val dfExpression: String
        val functionCalls: List<String>

        @GenerateConstructor
        companion object
    }

    val expressions1 = dataFrameOf(
        TestCase("""dataFrameOf("age", "name")(17, "Name")""", listOf(
            """df.insert("col1") { 42 }.after("age")""",
            """df.insert("col1") { 42 }.after("name")"""
        )),
    )

    fun wfetest() {
        val repl = object : JupyterReplTestCase() {

        }
        val df = expressions.toDataFrame {
            "expression" from { it }
            "df" from { repl.exec<DataFrame<*>>(it) }
        }.add {
            "schema" from {
                "df"<DataFrame<*>>().schema()
            }
        }
    }
}

