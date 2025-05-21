package com.fenix.ordenararquivos.util

import java.time.LocalDateTime
import java.util.*


class Utils {

    companion object {
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
    }

}