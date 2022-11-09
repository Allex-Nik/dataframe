/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.dataframe.services.BaseTestRunner
import org.jetbrains.kotlin.fir.dataframe.services.classpathFromClassloader
import org.jetbrains.kotlin.fir.dataframe.services.commonFirWithPluginFrontendConfiguration
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTreeVerifierHandler
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.fir2IrStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

open class AbstractDataFrameBlackBoxCodegenTest : BaseTestRunner()/*, RunnerWithTargetBackendForTestGeneratorMarker*/ {

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            targetBackend = TargetBackend.JVM_IR
        }
        defaultDirectives {
            JvmEnvironmentConfigurationDirectives.JDK_KIND with TestJdkKind.FULL_JDK
            +JvmEnvironmentConfigurationDirectives.WITH_REFLECT
        }
        commonFirWithPluginFrontendConfiguration()
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }
        fir2IrStep()
        irHandlersStep {
            useHandlers(
                ::IrTextDumpHandler,
                ::IrTreeVerifierHandler,
            )
        }
        facadeStep(::JvmIrBackendFacade)
        jvmArtifactsHandlersStep {
            useHandlers(::JvmBoxRunner)
        }
        useCustomRuntimeClasspathProviders(::MyClasspathProvider)
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }

    class MyClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
        override fun runtimeClassPaths(module: TestModule): List<File> {
            return (classpathFromClassloader(javaClass.classLoader) ?: error("no classpath"))
        }
    }
}
