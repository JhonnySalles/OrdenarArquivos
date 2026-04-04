package com.fenix.ordenararquivos

import org.junit.jupiter.api.extension.*
import java.util.*
import java.util.Optional

private data class TestResult(val name: String, val status: String, val color: String)

class TestStatusListener : TestWatcher, BeforeTestExecutionCallback, AfterAllCallback {

    private val ANSI_RESET = "\u001B[0m"
    private val ANSI_GREEN = "\u001B[32m"
    private val ANSI_RED = "\u001B[31m"
    private val ANSI_CYAN = "\u001B[36m"
    private val ANSI_YELLOW = "\u001B[33m"

    companion object {
        private val results = mutableListOf<TestResult>()
    }

    override fun beforeTestExecution(context: ExtensionContext) {
        val className = context.requiredTestClass.simpleName
        val methodName = context.requiredTestMethod.name
        println("${ANSI_CYAN}[RUNNING]${ANSI_RESET} $className.$methodName")
    }

    override fun testSuccessful(context: ExtensionContext) {
        val name = "${context.requiredTestClass.simpleName}.${context.requiredTestMethod.name}"
        results.add(TestResult(name, "SUCCESS", ANSI_GREEN))
        println("${ANSI_GREEN}[SUCCESS]${ANSI_RESET} $name")
    }

    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        val name = "${context.requiredTestClass.simpleName}.${context.requiredTestMethod.name}"
        results.add(TestResult(name, "FAILED ", ANSI_RED))
        println("${ANSI_RED}[FAILED ]${ANSI_RESET} $name - Cause: ${cause?.message ?: "unknown"}")
    }

    override fun testAborted(context: ExtensionContext, cause: Throwable?) {
        val name = context.displayName
        results.add(TestResult(name, "ABORTED", ANSI_YELLOW))
        println("${ANSI_YELLOW}[ABORTED]${ANSI_RESET} $name - ${cause?.message ?: "unknown"}")
    }

    override fun testDisabled(context: ExtensionContext, reason: Optional<String>?) {
        val name = context.displayName
        results.add(TestResult(name, "SKIPPED", ANSI_RESET))
        println("[SKIPPED] $name - Reason: ${reason?.orElse("none") ?: "none"}")
    }

    override fun afterAll(context: ExtensionContext) {
        if (results.isEmpty()) return

        println("\n${ANSI_CYAN}================================================================================${ANSI_RESET}")
        println("${ANSI_CYAN}                            TEST EXECUTION SUMMARY                              ${ANSI_RESET}")
        println("${ANSI_CYAN}================================================================================${ANSI_RESET}")
        
        results.forEach { result ->
            // Pintando a linha inteira com a cor correspondente
            println("${result.color}[${result.status}] ${result.name}${ANSI_RESET}")
        }
        
        println("${ANSI_CYAN}================================================================================${ANSI_RESET}\n")
        
        // Limpa para a próxima classe de teste
        results.clear()
    }
}
