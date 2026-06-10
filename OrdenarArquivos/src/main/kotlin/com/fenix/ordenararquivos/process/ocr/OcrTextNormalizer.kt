package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.util.Utils

data class ParsedChapterLine(
    val capitulo: Int,
    val pagina: Int,
    val titulo: String
)

object OcrTextNormalizer {

    internal val OCR_CHAPTER_REGEX = Regex("第?([\\d]+)話?[\\D]*([\\d]+)")

    private val JP_HORIZONTAL = Regex("""第?(\d+)話?\s*[-:.\s]+\s*(\d+)\s*(.*)""")
    private val JP_INVERTED = Regex("""(\d+)\s*話?\s*第?\s*(\d+)\s*(.*)""")
    private val EN_CHAPTER = Regex("""(?i)chapter\s*(\d+)\s*[-:.\s]+\s*(\d+)\s*(.*)""")
    private val PT_CAPITULO = Regex("""(?i)cap[ií]tulo\s*(\d+)\s*[-:.\s]+\s*(\d+)\s*(.*)""")
    private val NUMERIC_GENERIC = Regex("""(\d{1,3})\s*[-|:.]\s*(\d{1,4})\s*(.*)""")
    private val FORMATTED_LINE = Regex("""^(\d+)\s*[-|]\s*(\d+)(?:\|(.*))?$""")

    fun normalize(textos: String, separadorPagina: String = "-", separadorCapitulo: String = "|"): List<String> {
        if (textos.isBlank()) return emptyList()

        val parsed = mutableMapOf<Int, ParsedChapterLine>()
        for (linha in textos.lines()) {
            parseLine(linha.trim(), separadorPagina, separadorCapitulo)?.let { parsed[it.capitulo] = it }
        }
        return parsed.keys.sorted().map { cap ->
            val line = parsed[cap]!!
            formatLine(line, separadorPagina, separadorCapitulo)
        }
    }

    fun normalizeToString(textos: String, separadorPagina: String = "-", separadorCapitulo: String = "|"): String =
        normalize(textos, separadorPagina, separadorCapitulo).joinToString("\n")

    /** Compatibilidade com testes legados e Ocr.ocrToCapitulo */
    fun ocrToCapitulo(textos: String, separadorPagina: String = "-", separadorCapitulo: String = "|"): String {
        if (textos.isEmpty()) return ""

        val linhas = textos.split("\n")
        val capitulos = mutableMapOf<Int, Int>()
        for (linha in linhas) {
            OCR_CHAPTER_REGEX.matchEntire(linha.trim())?.let {
                if (it.groups.size > 2 && it.groups[1] != null && it.groups[2] != null) {
                    capitulos[Utils.safeToInt(it.groups[1]!!.value)] = Utils.safeToInt(it.groups[2]!!.value)
                }
            }
        }

        val normalized = normalize(textos, separadorPagina, separadorCapitulo)
        if (normalized.isNotEmpty()) return normalized.joinToString("\n")

        var capAnterior = 0
        var pagAnterior = 0
        val sugestao = StringBuilder()
        capitulos.keys.sorted().forEach {
            if (it > capAnterior && capitulos[it]!! > pagAnterior) {
                capAnterior = it
                pagAnterior = capitulos[it]!!
                sugestao.append(it.toString()).append(separadorPagina).append(capitulos[it]).append("\n")
            }
        }
        return sugestao.toString().trim()
    }

    private fun parseLine(linha: String, separadorPagina: String, separadorCapitulo: String): ParsedChapterLine? {
        if (linha.isBlank()) return null

        val withCustomSeps = tryCustomSeparators(linha, separadorPagina, separadorCapitulo)
        if (withCustomSeps != null) return withCustomSeps

        FORMATTED_LINE.matchEntire(linha)?.let { m ->
            return ParsedChapterLine(
                Utils.safeToInt(m.groupValues[1]),
                Utils.safeToInt(m.groupValues[2]),
                m.groupValues.getOrElse(3) { "" }.trim()
            )
        }

        for (regex in listOf(JP_HORIZONTAL, JP_INVERTED, EN_CHAPTER, PT_CAPITULO, NUMERIC_GENERIC)) {
            regex.find(linha)?.let { m ->
                val cap = Utils.safeToInt(sanitizeNumber(m.groupValues[1]))
                val pag = Utils.safeToInt(sanitizeNumber(m.groupValues[2]))
                val titulo = m.groupValues.getOrElse(3) { "" }
                    .replace("第", "")
                    .replace("話", "")
                    .trim()
                if (cap > 0 && pag > 0) return ParsedChapterLine(cap, pag, titulo)
            }
        }

        OCR_CHAPTER_REGEX.matchEntire(linha)?.let { m ->
            val cap = Utils.safeToInt(m.groupValues.getOrNull(1))
            val pag = Utils.safeToInt(m.groupValues.getOrNull(2))
            if (cap > 0 && pag > 0) return ParsedChapterLine(cap, pag, "")
        }

        return null
    }

    private fun tryCustomSeparators(linha: String, separadorPagina: String, separadorCapitulo: String): ParsedChapterLine? {
        if (!linha.contains(separadorPagina)) return null

        val beforePage = linha.substringBefore(separadorPagina).trim()
        val afterPage = linha.substringAfter(separadorPagina).trim()
        val cap = Utils.safeToInt(sanitizeNumber(beforePage.replace("第", "").replace("話", "")))
        if (cap <= 0) return null

        val pag: Int
        val titulo: String
        if (afterPage.contains(separadorCapitulo)) {
            pag = Utils.safeToInt(sanitizeNumber(afterPage.substringBefore(separadorCapitulo).trim()))
            titulo = afterPage.substringAfter(separadorCapitulo).trim()
        } else {
            val parts = afterPage.split(Regex("\\s+"), limit = 2)
            pag = Utils.safeToInt(sanitizeNumber(parts[0]))
            titulo = parts.getOrElse(1) { "" }.trim()
        }

        return if (pag > 0) ParsedChapterLine(cap, pag, titulo) else null
    }

    private fun formatLine(line: ParsedChapterLine, separadorPagina: String, separadorCapitulo: String): String {
        val cap = line.capitulo.toString().padStart(3, '0')
        val pag = line.pagina.toString().padStart(2, '0')
        return if (line.titulo.isNotEmpty()) {
            "$cap$separadorPagina$pag$separadorCapitulo${line.titulo}"
        } else {
            "$cap$separadorPagina$pag"
        }
    }

    private fun sanitizeNumber(raw: String): String =
        raw.trim().replace(" ", "").replace("I", "1").replace("l", "1")
}
