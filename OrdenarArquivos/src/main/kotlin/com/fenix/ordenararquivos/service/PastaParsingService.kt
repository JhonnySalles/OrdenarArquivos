package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.util.Utils
import java.io.File

class PastaParsingService {

    data class ParseResult(
        val volume: String = "0",
        val capitulo: String = "0",
        val scan: String = "",
        val titulo: String = "",
        val isCapa: Boolean = false
    )

    private val regexCapitulo = "(?i)(\\bcapitulo|\\bcapítulo|\\bchapter|\\bchap|\\bch\\.?|\\bcap\\.?|\\bc\\.?)\\s*([\\d.]+)".toRegex()
    private val regexVolume = "(?i)(volume|vol\\.?| v\\.?)([ \\d.]+)".toRegex()
    private val apenasNumeros = "^[\\d.]+".toRegex()

    fun parse(fileName: String): ParseResult {
        var nome = fileName
        if (nome.contains(",")) nome = nome.replace(",", "")
        if (nome.contains("_")) nome = nome.replace("_", " ")
        
        var volume = "0"
        var capitulos = "0"
        var scan = ""
        var titulo = ""
        
        if (nome.matches(apenasNumeros)) {
            capitulos = nome
        } else {
            regexCapitulo.find(nome)?.let { match ->
                capitulos = if (match.groups.size > 2 && match.groups[2] != null)
                    match.groups[2]!!.value.trim()
                else
                    match.value.lowercase().replace("ch.", "").replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "")
                
                val before = nome.substringBefore(match.value).trim()
                scan = regexVolume.find(before)?.let { 
                    if (it.value.isNotEmpty()) before.substringBefore(it.value) else before 
                } ?: before
                titulo = nome.substringAfter(match.value).trim()
            }
            
            regexVolume.find(nome)?.let { match ->
                if (match.value.isNotEmpty())
                    volume = if (match.groups.size > 2 && match.groups[2] != null)
                        match.groups[2]!!.value.trim()
                    else
                        match.value.replace("vol.", "").replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "")
            }

            if (scan.contains("_"))
                scan = scan.replace("_", " ").trim()
            else if (scan.isEmpty() && nome.contains("]"))
                scan = nome.substringBefore("]").replace("[", "")

            if (scan.contains("]"))
                scan = scan.substringBefore("]").replace("[", "")

            // No controller original, existia uma lógica que sobrescrevia 'nome' (o nome que estamos manipulando localmente), 
            // mas aqui retornamos as partes isoladas.
            
            if (titulo.contains("_"))
                titulo = titulo.replace("_", " ").trim()

            if (titulo.startsWith("-"))
                titulo = titulo.substring(1).trim()
        }

        val isCapa = fileName.lowercase().contains("capa")
        if (isCapa)
            capitulos = "0"

        return ParseResult(
            volume = volume,
            capitulo = capitulos,
            scan = scan,
            titulo = titulo,
            isCapa = isCapa
        )
    }
}
