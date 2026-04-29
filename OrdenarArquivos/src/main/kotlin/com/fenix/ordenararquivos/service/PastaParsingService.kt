package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.util.Utils

class PastaParsingService {

    data class ParseResult(
        val volume: String = "0",
        val capitulo: String = "0",
        val scan: String = "",
        val titulo: String = "",
        val isCapa: Boolean = false
    )

    private val regexCapitulo = "(?i)(capitulo|capítulo|chapter|chap|ch\\.?|cap\\.?|c\\.?)\\s*([\\d.]+)".toRegex()
    private val regexVolume = "(?i)(volume|vol\\.?|v\\.?)([ \\d.]+)".toRegex()
    private val apenasNumeros = "^[\\d.]+".toRegex()

    fun parse(fileName: String): ParseResult {
        var nome = fileName.replace(",", "").replace("_", " ")
        
        var volume = "0"
        var capitulos = "0"
        var scan = ""
        var titulo = ""
        
        if (nome.contains("[") && nome.contains("]")) {
            scan = nome.substringAfter("[").substringBefore("]").trim()
        }

        regexCapitulo.find(nome)?.let { match ->
            capitulos = match.groups[2]?.value?.trim() ?: "0"
            titulo = nome.substringAfter(match.value).trim()
            val before = nome.substringBefore(match.value).trim()
            if (scan.isEmpty()) {
                scan = regexVolume.find(before)?.let { 
                    if (it.value.isNotEmpty()) before.substringBefore(it.value) else before 
                } ?: before
            }
        }
        
        regexVolume.find(nome)?.let { match ->
            volume = match.groups[2]?.value?.trim() ?: "0"
        }

        if (scan.isEmpty() && nome.contains("]"))
            scan = nome.substringBefore("]").replace("[", "")

        if (scan.contains("]"))
            scan = scan.substringBefore("]").replace("[", "")
        
        if (scan.contains("["))
            scan = scan.substringAfter("[").trim()

        if (titulo.contains("_")) titulo = titulo.replace("_", " ").trim()
        if (titulo.startsWith("-")) titulo = titulo.substring(1).trim()

        val isCapa = fileName.lowercase().contains("capa")
        if (isCapa) capitulos = "0"

        return ParseResult(
            volume = volume,
            capitulo = capitulos,
            scan = scan,
            titulo = titulo,
            isCapa = isCapa
        )
    }
}
