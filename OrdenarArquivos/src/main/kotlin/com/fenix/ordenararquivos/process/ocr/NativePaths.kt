package com.fenix.ordenararquivos.process.ocr

import java.io.File
import java.nio.file.Paths

object NativePaths {

    /**
     * Subpasta dedicada ao instalador PaddleOCR-json dentro de [nativesDir].
     * O release possui muitos arquivos (exe, models, dependências) e deve ser
     * extraído integralmente apenas aqui, sem misturar com as DLLs do OpenCV
     * que ficam diretamente em `natives/`.
     */
    const val PADDLE_OCR_SUBDIR = "paddleocr"

    private val appRoot: String get() = Paths.get("").toAbsolutePath().toString()

    /** Raiz das bibliotecas nativas (ex.: DLLs OpenCV). */
    val nativesDir: File get() = File("$appRoot/natives")

    val tessdataDir: File get() = File("$appRoot/tessdata")

    /** Pasta isolada do PaddleOCR: `natives/paddleocr/`. */
    val paddleDir: File get() = File(nativesDir, PADDLE_OCR_SUBDIR)

    val paddleExe: File get() = File(paddleDir, "PaddleOCR-json.exe")

    fun paddleConfigFile(name: String): File = File(paddleDir, "models/$name")

    fun paddleOcrInstallHint(): String =
        "Extraia o instalador PaddleOCR-json completo em natives/$PADDLE_OCR_SUBDIR/ " +
            "(subpasta dedicada; não copie os arquivos para natives/ junto com o OpenCV)."
}
