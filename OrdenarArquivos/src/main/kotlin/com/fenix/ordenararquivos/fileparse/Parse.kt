package com.fenix.ordenararquivos.fileparse

import java.io.File
import java.io.IOException
import java.io.InputStream

interface Parse {

    @Throws(IOException::class)
    fun parse(arquivo: File)

    @Throws(IOException::class)
    fun destroir()

    fun getTipo(): String

    @Throws(IOException::class)
    fun getPagina(numero: Int): InputStream

    fun getSize(): Int

    fun getLegenda(): List<String>

    fun getLegendaNomes(): Map<String, Int>

    fun getPaginaPasta(num: Int): String

    fun getPastas(): Map<String, Int>

}