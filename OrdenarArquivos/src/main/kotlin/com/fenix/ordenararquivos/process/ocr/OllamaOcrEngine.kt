package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.Linguagem
import java.io.File

/**
 * Stub reservado para integração futura com Ollama (modelos de visão locais).
 * Não registrado na factory até ser habilitado.
 */
class OllamaOcrEngine : OcrEngineStrategy {

    override fun prepare(linguagem: Linguagem) {
        throw OcrException("Ollama ainda não implementado.")
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        throw OcrException("Ollama ainda não implementado.")
    }

    override fun clear() {}

    override fun isAvailable(): Boolean = false
}
