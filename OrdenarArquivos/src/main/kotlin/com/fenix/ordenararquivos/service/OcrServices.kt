package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.process.Ocr
import java.io.File

open class OcrServices {

    @JvmOverloads
    open fun processOcr(
        sumario: File,
        separadorPagina: String,
        separadorCapitulo: String,
        linguagem: Linguagem = Linguagem.JAPANESE
    ): String {
        if (!sumario.exists() || sumario.length() == 0L) return ""
        if (Ocr.isTeste) return Ocr.testSuggestion
        Ocr.prepare(linguagem)
        try {
            return Ocr.process(sumario, separadorPagina, separadorCapitulo, linguagem)
        } finally {
            Ocr.clear()
        }
    }
}
