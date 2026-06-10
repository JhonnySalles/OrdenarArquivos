package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.enums.OcrEngine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OcrEngineFactoryTest {

    @Test
    fun `deve resolver motor tesseract`() {
        val original = Configuracao.ocrEngine
        try {
            Configuracao.ocrEngine = OcrEngine.TESSERACT
            assertTrue(OcrEngineFactory.resolve() is TesseractOcrEngine)
        } finally {
            Configuracao.ocrEngine = original
        }
    }

    @Test
    fun `deve resolver motor gemini`() {
        val original = Configuracao.ocrEngine
        try {
            Configuracao.ocrEngine = OcrEngine.GEMINI
            assertTrue(OcrEngineFactory.resolve() is GeminiOcrEngine)
        } finally {
            Configuracao.ocrEngine = original
        }
    }
}
