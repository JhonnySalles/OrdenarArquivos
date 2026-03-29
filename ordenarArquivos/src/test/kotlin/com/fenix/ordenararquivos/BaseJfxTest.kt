package com.fenix.ordenararquivos

import javafx.application.Platform
import org.junit.jupiter.api.BeforeAll
import java.util.concurrent.CountDownLatch

abstract class BaseJfxTest : BaseTest() {

    @BeforeAll
    fun setupJfx() {
        if (!isJfxInitialized) {
            try {
                val latch = CountDownLatch(1)
                Platform.startup {
                    latch.countDown()
                }
                latch.await()
                isJfxInitialized = true
            } catch (e: IllegalStateException) {
                // Toolkit already initialized
                isJfxInitialized = true
            }
        }
    }

    companion object {
        private var isJfxInitialized = false
    }
}
