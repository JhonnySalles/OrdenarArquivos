package com.fenix.ordenararquivos.model.enums

enum class OcrEngine(val configValue: String, val displayName: String) {
    TESSERACT("tesseract", "Tesseract (local)"),
    PADDLE("paddle", "PaddleOCR (local)"),
    GEMINI("gemini", "Gemini (nuvem)"),
    OPENROUTER("openrouter", "OpenRouter (nuvem)"),
    OCR_SPACE("ocrspace", "OCR.space (nuvem)"),
    OPTIIC("optiic", "Optiic (nuvem)");

    // OLLAMA("ollama", "Ollama (local)") — reservado para extensão futura

    companion object {
        fun fromConfigValue(value: String?): OcrEngine =
            values().find { it.configValue.equals(value, ignoreCase = true) } ?: TESSERACT
    }
}
