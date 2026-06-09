package com.fenix.ordenararquivos.process

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class WinrarRarConversionTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testConverterArquivoInexistenteRetornaFalse() {
        val missing = File(tempDir.toFile(), "missing.rar")
        assertFalse(Winrar.converterRar5ParaRar4(missing))
    }
}
