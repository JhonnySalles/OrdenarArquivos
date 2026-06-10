package com.fenix.ordenararquivos.process.ocr

import org.opencv.core.Rect

enum class TextOrientation {
    HORIZONTAL,
    VERTICAL,
    UNCERTAIN
}

data class OcrTextBlock(
    val text: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val aspectRatio: Double
        get() = if (height > 0) width.toDouble() / height else 1.0
}

object JapaneseLayoutHelper {

    fun detectOrientation(blocks: List<OcrTextBlock>): TextOrientation {
        if (blocks.isEmpty()) return TextOrientation.UNCERTAIN

        val verticalCount = blocks.count { it.height > it.width * 1.2 }
        val horizontalCount = blocks.count { it.width >= it.height }

        return when {
            verticalCount > horizontalCount * 1.5 -> TextOrientation.VERTICAL
            horizontalCount > verticalCount * 1.5 -> TextOrientation.HORIZONTAL
            else -> TextOrientation.UNCERTAIN
        }
    }

    fun detectOrientationFromRects(rects: List<Rect>): TextOrientation {
        return detectOrientation(rects.map { OcrTextBlock("", it.x, it.y, it.width, it.height) })
    }

    fun sortBlocks(blocks: List<OcrTextBlock>, orientation: TextOrientation): List<OcrTextBlock> {
        return when (orientation) {
            TextOrientation.VERTICAL -> blocks.sortedWith(compareByDescending<OcrTextBlock> { it.x }.thenBy { it.y })
            TextOrientation.HORIZONTAL, TextOrientation.UNCERTAIN ->
                blocks.sortedWith(compareBy<OcrTextBlock> { it.y }.thenBy { it.x })
        }
    }

    fun sortRects(rects: List<Rect>, orientation: TextOrientation): List<Rect> {
        return when (orientation) {
            TextOrientation.VERTICAL -> rects.sortedWith(compareByDescending<Rect> { it.x }.thenBy { it.y })
            TextOrientation.HORIZONTAL, TextOrientation.UNCERTAIN ->
                rects.sortedWith(compareBy<Rect> { it.y }.thenBy { it.x })
        }
    }

    fun mergeDualPassResults(
        primary: String,
        secondary: String,
        separadorPagina: String,
        separadorCapitulo: String
    ): String {
        val merged = LinkedHashSet<String>()
        OcrTextNormalizer.normalize(primary, separadorPagina, separadorCapitulo).forEach { merged.add(it) }
        OcrTextNormalizer.normalize(secondary, separadorPagina, separadorCapitulo).forEach { merged.add(it) }
        return merged.joinToString("\n")
    }

    fun pickBestResult(
        results: List<String>,
        separadorPagina: String,
        separadorCapitulo: String
    ): String {
        if (results.isEmpty()) return ""
        if (results.size == 1) return results.first()

        val scored = results.map { text ->
            val lines = OcrTextNormalizer.normalize(text, separadorPagina, separadorCapitulo)
            text to lines.size
        }

        val maxScore = scored.maxOf { it.second }
        val best = scored.filter { it.second == maxScore }.map { it.first }

        return if (best.size == 1) {
            best.first()
        } else {
            mergeDualPassResults(best[0], best.drop(1).joinToString("\n"), separadorPagina, separadorCapitulo)
        }
    }
}
