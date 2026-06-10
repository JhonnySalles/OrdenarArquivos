package com.fenix.ordenararquivos.process.ocr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JapaneseLayoutHelperTest {

    @Test
    fun `deve detectar orientacao vertical`() {
        val blocks = listOf(
            OcrTextBlock("a", 100, 10, 20, 80),
            OcrTextBlock("b", 100, 100, 20, 90),
            OcrTextBlock("c", 50, 10, 15, 85)
        )
        assertEquals(TextOrientation.VERTICAL, JapaneseLayoutHelper.detectOrientation(blocks))
    }

    @Test
    fun `deve detectar orientacao horizontal`() {
        val blocks = listOf(
            OcrTextBlock("a", 10, 50, 120, 25),
            OcrTextBlock("b", 10, 90, 130, 30)
        )
        assertEquals(TextOrientation.HORIZONTAL, JapaneseLayoutHelper.detectOrientation(blocks))
    }

    @Test
    fun `deve ordenar blocos vertical direita para esquerda`() {
        val blocks = listOf(
            OcrTextBlock("col1", 50, 10, 20, 80),
            OcrTextBlock("col2", 100, 10, 20, 80)
        )
        val sorted = JapaneseLayoutHelper.sortBlocks(blocks, TextOrientation.VERTICAL)
        assertEquals("col2", sorted.first().text)
    }

    @Test
    fun `pickBestResult escolhe passagem com mais linhas`() {
        val best = JapaneseLayoutHelper.pickBestResult(
            listOf("第1話 5", "第1話 5\n第2話 10\n第3話 15"),
            "-",
            "|"
        )
        assertTrue(best.contains("第3話"))
    }
}
