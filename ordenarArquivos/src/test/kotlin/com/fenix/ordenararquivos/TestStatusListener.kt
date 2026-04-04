package com.fenix.ordenararquivos

import org.junit.jupiter.api.extension.*
import java.util.*
import java.util.Optional

class TestStatusListener : TestWatcher, BeforeTestExecutionCallback {

    private val ANSI_RESET = "\u001B[0m"
    private val ANSI_GREEN = "\u001B[32m"
    private val ANSI_RED = "\u001B[31m"
    private val ANSI_CYAN = "\u001B[36m"

    override fun beforeTestExecution(context: ExtensionContext) {
        val className = context.requiredTestClass.simpleName
        val methodName = context.requiredTestMethod.name
        println("${ANSI_CYAN}[RUNNING]${ANSI_RESET} $className.$methodName")
    }

    override fun testSuccessful(context: ExtensionContext) {
        val className = context.requiredTestClass.simpleName
        val methodName = context.requiredTestMethod.name
        println("${ANSI_GREEN}[SUCCESS]${ANSI_RESET} $className.$methodName")
    }

    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        val className = context.requiredTestClass.simpleName
        val methodName = context.requiredTestMethod.name
        println("${ANSI_RED}[FAILED ]${ANSI_RESET} $className.$methodName - Cause: ${cause?.message ?: "unknown"}")
    }

    override fun testAborted(context: ExtensionContext, cause: Throwable?) {
        println("[ABORTED] ${context.displayName} - ${cause?.message ?: "unknown"}")
    }

    override fun testDisabled(context: ExtensionContext, reason: Optional<String>?) {
        println("[SKIPPED] ${context.displayName} - Reason: ${reason?.orElse("none") ?: "none"}")
    }
}
