package com.fenix.ordenararquivos.configuration

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object Configuracao {
    private const val ARQUIVO = "app.properties"
    fun createProperties(winrar: String) {
        try {
            FileOutputStream(ARQUIVO).use { os ->
                val props = Properties()
                props.setProperty("caminho_winrar", winrar)
                props.store(os, "")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
	fun loadProperties(): Properties? {
        val f = File(ARQUIVO)
        if (!f.exists()) createProperties("")
        try {
            FileInputStream(ARQUIVO).use { fs ->
                val props = Properties()
                props.load(fs)
                return props
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}