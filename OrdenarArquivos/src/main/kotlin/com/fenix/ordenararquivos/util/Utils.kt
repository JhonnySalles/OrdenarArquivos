package com.fenix.ordenararquivos.util

import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import javafx.stage.DirectoryChooser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern

class Utils {

    companion object {
        private val mLOG: Logger = LoggerFactory.getLogger(Utils::class.java)

        const val COMICINFO = "ComicInfo.xml"
        const val SEPARADOR_IMAGEM = ";"
        const val SEPARADOR_IMPORTACAO = "$#"
        const val SEPARADOR_PAGINA = "-"
        const val SEPARADOR_CAPITULO = "|"

        const val IMAGE_PATTERN = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$"
        const val NUMBER_PATTERN = "[\\d.]+"
        const val NOT_NUMBER_PATTERN = "[^\\d.]+"
        const val JAPANESE_PATTERN = ".*[\u3041-\u9FAF].*"
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

        fun isImage(filename: String): Boolean = filename.lowercase(Locale.getDefault()).matches(".*\\.(jpg|jpeg|bmp|gif|png|webp)$".toRegex())

        fun isZip(filename: String): Boolean = filename.lowercase(Locale.getDefault()).matches(".*\\.(zip|cbz)$".toRegex())
        
        fun isTarball(filename: String): Boolean = filename.lowercase(Locale.getDefault()).matches(".*\\.(cbt)$".toRegex())

        fun isSevenZ(filename: String): Boolean = filename.lowercase(Locale.getDefault()).matches(".*\\.(cb7|7z)$".toRegex())

        fun getPasta(path: String): String {
            var pasta = path
            if (pasta.contains("/"))
                pasta = pasta.substring(0, pasta.lastIndexOf("/"))
            else if (pasta.contains("\\"))
                pasta = pasta.substring(0, pasta.lastIndexOf("\\"))

            if (pasta.contains("/"))
                pasta = pasta.substring(pasta.lastIndexOf("/") + 1)
            else if (pasta.contains("\\"))
                pasta = pasta.substring(pasta.lastIndexOf("\\") + 1)

            return pasta
        }

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

        fun getNomeNormalizadoOrdenacao(path: String): String {
            var nome = path
            if (nome.contains("/"))
                nome = nome.substring(nome.lastIndexOf("/") + 1)
            else if (nome.contains("\\"))
                nome = nome.substring(nome.lastIndexOf("\\") + 1)

            if (nome.contains("."))
                nome = nome.substring(0, nome.lastIndexOf("."))

            var numeros = ""
            val m = Pattern.compile("\\d+$").matcher(nome)
            while (m.find())
                numeros = m.group()

            return if (numeros.isEmpty())
                getNome(path)
            else
                (nome.substring(0, nome.lastIndexOf(numeros)) + String.format("%10s", numeros).replace(' ', '0') + getExtenssao(path))
        }

        fun getNumber(texto: String): Double? {
            // Usa regex para encontrar o primeiro padrão numérico válido (opcionalmente com decimal)
            val match = "[0-9]+(\\.[0-9]+)?".toRegex().find(texto)
            return try {
                match?.value?.toDouble()
            } catch (e1: Exception) {
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

        fun normaliza(texto: String, locale: Locale = Locale.getDefault()): String {
            if (texto.isBlank() || texto.isEmpty())
                return texto

            val sb = StringBuilder()
            var capitalizeNext = true
            for (char in texto.trim().lowercase(locale)) {
                when {
                    // Se for um separador (espaço, hífen), anexa e marca a próxima para capitalizar.
                    // O apóstrofo (') foi removido para evitar "It'S" em vez de "It's".
                    char.isWhitespace() || char == '-' -> {
                        sb.append(char)
                        capitalizeNext = true
                    }
                    // Se for para capitalizar, anexa a versão maiúscula e desmarca
                    capitalizeNext -> {
                        sb.append(char.titlecaseChar())
                        capitalizeNext = false
                    }
                    // Caso contrário, apenas anexa o caractere minúsculo
                    else -> sb.append(char)
                }
            }
            return sb.toString()
        }

        fun MD5(string: String): String {
            return try {
                val digest = MessageDigest.getInstance("MD5")
                digest.update(string.toByteArray(), 0, string.length)
                BigInteger(1, digest.digest()).toString(16)
            } catch (e: NoSuchAlgorithmException) {
                string.replace("/", ".")
            }
        }

        fun MD5(image: InputStream): String {
            return try {
                val buffer = ByteArray(1024)
                val digest = MessageDigest.getInstance("MD5")
                var numRead = 0
                while (numRead != -1) {
                    numRead = image.read(buffer)
                    if (numRead > 0) digest.update(buffer, 0, numRead)
                }
                val md5Bytes = digest.digest()
                var md5 = ""
                for (i in md5Bytes.indices)
                    md5 += Integer.toString((md5Bytes[i].toInt() and 0xff) + 0x100, 16).substring(1)

                md5
            } catch (e: Exception) {
                mLOG.error(e.message, e)
                throw e
            } finally {
                try {
                    image.close()
                } catch (e: IOException) {
                    mLOG.error(e.message, e)
                }
            }
        }

        @Throws(IOException::class)
        fun toByteArray(`is`: InputStream): ByteArray {
            val output = ByteArrayOutputStream()
            return try {
                val b = ByteArray(4096)
                var n = 0
                while (`is`.read(b).also { n = it } != -1)
                    output.write(b, 0, n)

                output.toByteArray()
            } finally {
                output.close()
            }
        }
    }

}