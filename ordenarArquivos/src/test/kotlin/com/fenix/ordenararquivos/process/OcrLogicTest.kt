package com.fenix.ordenararquivos.process

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OcrLogicTest {

    @Test
    fun `deve identificar capitulo e pagina com regex japones`() {
        val regex = Ocr.OCR_CHAPTER_REGEX
        
        val match1 = regex.matchEntire("第1話 5")
        assertNotNull(match1)
        assertEquals("1", match1!!.groups[1]?.value)
        assertEquals("5", match1.groups[2]?.value)

        val match2 = regex.matchEntire("2話-10")
        assertNotNull(match2)
        assertEquals("2", match2!!.groups[1]?.value)
        assertEquals("10", match2.groups[2]?.value)
    }

    @Test
    fun `deve converter texto OCR para formato de capitulo sugerido`() {
        val ocrOutput = """
            第1話 5
            Capa
            第2話 15
            Outro texto
            第3話 25
        """.trimIndent()

        val sugestao = Ocr.ocrToCapitulo(ocrOutput, "-", "|")
        
        // Espera-se que apenas as linhas que dão match no regex sejam processadas
        // E que a lógica de ordenação e filtro funcione
        assertTrue(sugestao.contains("1-5"))
        assertTrue(sugestao.contains("2-15"))
        assertTrue(sugestao.contains("3-25"))
    }

    @Test
    fun `deve retornar vazio para texto sem correspondencia`() {
        val ocrOutput = "Texto qualquer sem numeros"
        val sugestao = Ocr.ocrToCapitulo(ocrOutput)
        assertEquals("", sugestao)
    }
}
