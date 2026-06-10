package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.OcrEngine
import com.fenix.ordenararquivos.process.Ocr

object OcrEngineFactory {

    private val tesseractEngine = TesseractOcrEngine()
    private val paddleEngine = PaddleOcrJsonEngine()
    private val geminiEngine = GeminiOcrEngine()

    fun currentEngine(): OcrEngine = Configuracao.ocrEngine

    fun isAvailable(engine: OcrEngine): Boolean = when (engine) {
        OcrEngine.TESSERACT -> Ocr.mLibs && tesseractEngine.isAvailable()
        OcrEngine.PADDLE -> paddleEngine.isAvailable()
        OcrEngine.GEMINI -> geminiEngine.isAvailable()
    }

    fun resolve(engine: OcrEngine = currentEngine()): OcrEngineStrategy = when (engine) {
        OcrEngine.TESSERACT -> tesseractEngine
        OcrEngine.PADDLE -> paddleEngine
        OcrEngine.GEMINI -> geminiEngine
        // OcrEngine.OLLAMA -> OllamaOcrEngine() quando habilitado
    }

    fun validateAvailable(engine: OcrEngine = currentEngine()) {
        when (engine) {
            OcrEngine.TESSERACT -> {
                if (!Ocr.mLibs) throw OcrException("Bibliotecas OpenCV não instanciadas.")
                if (!tesseractEngine.isAvailable()) throw OcrException("Tessdata não encontrado.")
            }
            OcrEngine.PADDLE -> {
                if (!Ocr.mLibs) throw OcrException("Bibliotecas OpenCV não instanciadas (necessárias para rotação).")
                if (!paddleEngine.isAvailable()) {
                    throw OcrException(
                        "PaddleOCR-json não encontrado em ${NativePaths.paddleExe.absolutePath}. " +
                            NativePaths.paddleOcrInstallHint()
                    )
                }
            }
            OcrEngine.GEMINI -> {
                if (!geminiEngine.isAvailable()) {
                    throw OcrException("Chave da API Gemini não configurada em secrets.properties.")
                }
            }
        }
    }
}
