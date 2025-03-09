package com.fenix.ordenararquivos.util

import java.io.File
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile


class Utils {

    companion object {
        fun toDateTime(dateTime: String): LocalDateTime {
            return if (dateTime.isEmpty()) LocalDateTime.MIN else LocalDateTime.parse(dateTime)
        }

        fun fromDateTime(dateTime: LocalDateTime): String {
            return dateTime.toString()
        }
    }

}