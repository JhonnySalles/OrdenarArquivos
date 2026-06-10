package com.fenix.ordenararquivos.process.ocr

data class PaddleOcrSettings(
    val cls: Boolean = true,
    val useAngleCls: Boolean = true,
    val limitSideLen: Int = 2880
)
