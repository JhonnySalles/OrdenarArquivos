package com.fenix.ordenararquivos.process.ocr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OcrTextNormalizerTest {

    @Test
    fun `deve parsear japones horizontal`() {
        val result = OcrTextNormalizer.normalize("第1話 5 Intro\n第2話 15 Cap", "-", "|")
        assertTrue(result.any { it.startsWith("001-05") })
        assertTrue(result.any { it.startsWith("002-15") })
    }

    @Test
    fun `deve parsear japones ordem invertida`() {
        val result = OcrTextNormalizer.normalize("5 話 1 Resto", "-", "|")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `deve parsear ingles`() {
        val result = OcrTextNormalizer.normalize("Chapter 1 - 10 Title", "-", "|")
        assertEquals("001-10|Title", result.first())
    }

    @Test
    fun `deve parsear portugues`() {
        val result = OcrTextNormalizer.normalize("Capítulo 2: 20 Nome", "-", "|")
        assertEquals("002-20|Nome", result.first())
    }

    @Test
    fun `deve mesclar dual-pass sem duplicatas`() {
        val merged = JapaneseLayoutHelper.mergeDualPassResults(
            "第1話 5\n第2話 15",
            "第1話 5\n第3話 25",
            "-",
            "|"
        )
        assertTrue(merged.contains("001-05"))
        assertTrue(merged.contains("002-15"))
        assertTrue(merged.contains("003-25"))
    }

    @Test
    fun `deve retornar vazio para texto sem correspondencia`() {
        assertEquals("", OcrTextNormalizer.ocrToCapitulo("Texto qualquer sem numeros"))
    }

    @Test
    fun `compatibilidade regex legado`() {
        val regex = OcrTextNormalizer.OCR_CHAPTER_REGEX
        val match = regex.matchEntire("第1話 5")
        assertNotNull(match)
        assertEquals("1", match!!.groups[1]?.value)
        assertEquals("5", match.groups[2]?.value)
    }
}
