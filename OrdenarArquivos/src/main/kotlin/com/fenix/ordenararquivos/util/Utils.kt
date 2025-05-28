package com.fenix.ordenararquivos.util

import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import javafx.stage.DirectoryChooser
import java.io.File
import java.time.LocalDateTime
import java.util.*


class Utils {

    companion object {
        const val COMICINFO = "ComicInfo.xml"
        const val SEPARADOR_IMAGEM = ";"
        const val SEPARADOR_IMPORTACAO = "#"
        const val SEPARADOR_PAGINA = "-"
        const val SEPARADOR_CAPITULO = "|"

        const val IMAGE_PATTERN = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$"
        const val NUMBER_PATTERN = "[\\d.]+"
        const val NOT_NUMBER_PATTERN = "[^\\d.]+"
        val NUMBER_REGEX = Regex("\\d*")
        val ONLY_NUMBER_REGEX = Regex("^\\d+")

        fun clickTab() = Robot().keyPress(KeyCode.TAB)

        fun toDateTime(dateTime: String): LocalDateTime {
            return if (dateTime.isEmpty()) LocalDateTime.MIN else LocalDateTime.parse(dateTime)
        }

        fun fromDateTime(dateTime: LocalDateTime): String {
            return dateTime.toString()
        }

        fun isRar(filename: String): Boolean = filename.lowercase(Locale.getDefault()).matches(".*\\.(rar|cbr)$".toRegex())

        fun getCaminho(path: String): String {
            var pasta = path
            if (pasta.contains("/"))
                pasta = pasta.substring(0, pasta.lastIndexOf("/"))
            else if (pasta.contains("\\"))
                pasta = pasta.substring(0, pasta.lastIndexOf("\\"))
            return pasta
        }

        fun getNome(path: String): String {
            var name = path
            if (name.contains("/"))
                name = name.substring(name.lastIndexOf("/") + 1)
            else if (name.contains("\\"))
                name = name.substring(name.lastIndexOf("\\") + 1)
            return name
        }

        fun getExtenssao(path: String): String {
            return if (path.contains(".")) path.substring(path.lastIndexOf(".")) else path
        }

        fun getNumber(texto: String): Double? {
            val numero = texto.replace(NOT_NUMBER_PATTERN.toRegex(), "").trim { it <= ' ' }
            return try {
                java.lang.Double.valueOf(numero)
            } catch (e1: NumberFormatException) {
                null
            }
        }

        fun fromNumberJapanese(capitulo: String) : String {
            var numbero = ""
            for (c in capitulo)
                numbero += when (c) {
                    '\uFF10' -> "0"
                    '\uFF11' -> "1"
                    '\uFF12' -> "2"
                    '\uFF13' -> "3"
                    '\uFF14' -> "4"
                    '\uFF15' -> "5"
                    '\uFF16' -> "6"
                    '\uFF17' -> "7"
                    '\uFF18' -> "8"
                    '\uFF19' -> "9"
                    '\uFF0E' -> "."
                    else -> c
                }
            return numbero
        }

        fun toNumberJapanese(capitulo: String) : String {
            var numbero = ""
            for (c in capitulo.lowercase())
                numbero += when (c) {
                    '0' -> "\uFF10"
                    '1' -> "\uFF11"
                    '2' -> "\uFF12"
                    '3' -> "\uFF13"
                    '4' -> "\uFF14"
                    '5' -> "\uFF15"
                    '6' -> "\uFF16"
                    '7' -> "\uFF17"
                    '8' -> "\uFF18"
                    '9' -> "\uFF19"
                    '.' -> "\uFF0E"
                    else -> c
                }
            return numbero
        }

        fun selecionaPasta(pasta: String): File? {
            val fileChooser = DirectoryChooser()
            fileChooser.title = "Selecione o arquivo."
            if (pasta.isNotEmpty()) {
                val defaultDirectory = File(pasta)
                fileChooser.initialDirectory = defaultDirectory
            }
            return fileChooser.showDialog(null)
        }
    }

}