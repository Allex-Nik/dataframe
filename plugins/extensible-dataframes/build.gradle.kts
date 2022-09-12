plugins {
    id("java")
    kotlin("jvm") version "1.8.0-dev-446"
    kotlin("libs.publisher") version "0.0.60-dev-30"
}

group = "org.jetbrains.kotlinx.dataframe"
version = "0.9.0-dev"

val kotlinVersion: String by project.properties

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test", "test-gen"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

dependencies {
    "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion".let {
        compileOnly(it)
        testImplementation(it)
    }

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:$kotlinVersion")

    constraints {
        implementation(kotlin("compiler-embeddable", version = "1.8.0-dev-446"))
        testImplementation(kotlin("compiler-embeddable", version = "1.8.0-dev-446"))
        runtimeOnly(kotlin("compiler-embeddable", version = "1.8.0-dev-446"))
    }

    implementation("org.jetbrains.kotlinx:dataframe")
    implementation("org.jetbrains.kotlinx.dataframe:bridge-generator:0.9.0-dev")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinVersion")
    //testImplementation("junit:junit:4.12")

    testImplementation(platform("org.junit:junit-bom:5.8.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.platform:junit-platform-suite-api")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        useK2 = true
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.create<JavaExec>("generateTests") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.jetbrains.kotlinx.dataframe.GenerateTestsKt")
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
    val path = project.configurations
        .testRuntimeClasspath.get()
        .files
        .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: return
    systemProperty(propName, path)
}
