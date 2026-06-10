package com.fenix.ordenararquivos.process.ocr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class PaddleOcrConfigApplierTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `deve atualizar chaves existentes no config`() {
        val config = File(tempDir.toFile(), "config_japan.txt")
        config.writeText(
            """
            det=true
            cls=false
            use_angle_cls=false
            limit_side_len=960
            """.trimIndent()
        )

        PaddleOcrConfigApplier.applySettings(
            PaddleOcrSettings(cls = true, useAngleCls = true, limitSideLen = 2880),
            tempDir.toFile()
        )

        val parsed = PaddleOcrConfigApplier.parseConfigFile(config)
        assertEquals("true", parsed["cls"])
        assertEquals("true", parsed["use_angle_cls"])
        assertEquals("2880", parsed["limit_side_len"])
    }

    @Test
    fun `deve acrescentar chaves ausentes no config`() {
        val config = File(tempDir.toFile(), "config_en.txt")
        config.writeText("det=true\n")

        PaddleOcrConfigApplier.applySettings(
            PaddleOcrSettings(cls = true, useAngleCls = false, limitSideLen = 1920),
            tempDir.toFile()
        )

        val content = config.readText()
        assertTrue(content.contains("cls=true"))
        assertTrue(content.contains("use_angle_cls=false"))
        assertTrue(content.contains("limit_side_len=1920"))
    }

    @Test
    fun `deve parsear arquivo de config`() {
        val config = File(tempDir.toFile(), "config_latin.txt")
        config.writeText("# comentario\ncls=true\nlimit_side_len=1440\n")

        val parsed = PaddleOcrConfigApplier.parseConfigFile(config)
        assertEquals("true", parsed["cls"])
        assertEquals("1440", parsed["limit_side_len"])
        assertNull(parsed["# comentario"])
    }
}
