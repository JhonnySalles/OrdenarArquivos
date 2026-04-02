package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.process.Ocr
import java.io.File

open class OcrServices {

    open fun processOcr(sumario: File, separadorPagina: String, separadorCapitulo: String): String {
        return Ocr.process(sumario, separadorPagina, separadorCapitulo)
    }
}
