package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.model.enums.Linguagem
import java.io.File

interface OcrEngineStrategy {
    fun prepare(linguagem: Linguagem)
    fun recognize(image: File, linguagem: Linguagem): String
    fun clear()
    fun isAvailable(): Boolean
}
