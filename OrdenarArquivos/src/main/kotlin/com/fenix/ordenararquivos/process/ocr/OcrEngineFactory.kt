package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.OcrEngine
import com.fenix.ordenararquivos.process.Ocr

object OcrEngineFactory {

    private val tesseractEngine = TesseractOcrEngine()
    private val paddleEngine = PaddleOcrJsonEngine()
    private val geminiEngine = GeminiOcrEngine()
    private val openRouterEngine = OpenRouterOcrEngine()
    private val ocrSpaceEngine = OcrSpaceOcrEngine()
    private val optiicEngine = OptiicOcrEngine()

    fun currentEngine(): OcrEngine = Configuracao.ocrEngine

    fun isAvailable(engine: OcrEngine): Boolean = when (engine) {
        OcrEngine.TESSERACT -> Ocr.mLibs && tesseractEngine.isAvailable()
        OcrEngine.PADDLE -> paddleEngine.isAvailable()
        OcrEngine.GEMINI -> geminiEngine.isAvailable()
        OcrEngine.OPENROUTER -> openRouterEngine.isAvailable()
        OcrEngine.OCR_SPACE -> ocrSpaceEngine.isAvailable()
        OcrEngine.OPTIIC -> optiicEngine.isAvailable()
    }

    fun resolve(engine: OcrEngine = currentEngine()): OcrEngineStrategy = when (engine) {
        OcrEngine.TESSERACT -> tesseractEngine
        OcrEngine.PADDLE -> paddleEngine
        OcrEngine.GEMINI -> geminiEngine
        OcrEngine.OPENROUTER -> openRouterEngine
        OcrEngine.OCR_SPACE -> ocrSpaceEngine
        OcrEngine.OPTIIC -> optiicEngine
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
            OcrEngine.OPENROUTER -> {
                if (!openRouterEngine.isAvailable()) {
                    throw OcrException("Chave da API OpenRouter não configurada em secrets.properties.")
                }
            }
            OcrEngine.OCR_SPACE -> {
                if (!ocrSpaceEngine.isAvailable()) {
                    throw OcrException("Chave da API OCR.space não configurada em secrets.properties.")
                }
            }
            OcrEngine.OPTIIC -> {
                if (!optiicEngine.isAvailable()) {
                    throw OcrException("Chave da API Optiic não configurada em secrets.properties.")
                }
            }
        }
    }
}
