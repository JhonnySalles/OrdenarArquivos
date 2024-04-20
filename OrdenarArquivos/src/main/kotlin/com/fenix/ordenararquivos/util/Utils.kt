package com.fenix.ordenararquivos.util

import java.time.LocalDateTime

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