package com.fenix.ordenararquivos.process.ocr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaddleOcrJsonEngineTest {

    private val engine = PaddleOcrJsonEngine()

    @Test
    fun `deve parsear json com blocos ordenados`() {
        val json = """
            {
              "code": 100,
              "data": [
                {"text": "第2話 15", "box": [[10,50],[100,50],[100,70],[10,70]]},
                {"text": "第1話 5", "box": [[10,10],[100,10],[100,30],[10,30]]}
              ]
            }
        """.trimIndent()

        val text = engine.parseJsonOutput(json, TextOrientation.HORIZONTAL)
        val lines = text.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("第1話"))
    }

    @Test
    fun `deve retornar vazio para json invalido`() {
        assertEquals("", engine.parseJsonOutput("not json", TextOrientation.HORIZONTAL))
    }
}
