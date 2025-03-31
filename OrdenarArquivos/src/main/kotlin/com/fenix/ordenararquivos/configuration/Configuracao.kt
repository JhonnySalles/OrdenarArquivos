package com.fenix.ordenararquivos.configuration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object Configuracao {

    private val mLOG: Logger = LoggerFactory.getLogger(Configuracao::class.java)

    private val secrets: Properties = Properties()
    private val properties: Properties = Properties()

    init {
        loadProperties()
        loadSecrets()
    }

    fun saveProperties() {
        try {
            FileOutputStream("app.properties").use { os ->
                properties.store(os, "")
            }
        } catch (e: IOException) {
            mLOG.error(e.message, e)
        }
    }

    private fun createProperties() {
        try {
            FileOutputStream("app.properties").use { os ->
                properties.clear()
                properties.setProperty("caminho.commictagger", "")
                properties.store(os, "")
            }
        } catch (e: IOException) {
            mLOG.error(e.message, e)
        }
    }

    private fun loadProperties(): Properties {
        val f = File("app.properties")
        if (!f.exists())
            createProperties()
        try {
            FileInputStream("app.properties")
                .use { fs ->
                    properties.load(fs)
                    return properties
                }
        } catch (e: IOException) {
            mLOG.error(e.message, e)
            throw Exception("Erro ao carregar o properties")
        }
    }

    var caminhoCommicTagger: String = ""
        set(value) {
            properties["caminho.commictagger"] = value
            field = value
        }
        get() = properties.getProperty("caminho.commictagger", "")

    var registrosConsultaMal: Int = 50
        set(value) {
            properties["mal.registros_consulta"] = value
            field = value
        }
        get() = properties.getProperty("mal.registros_consulta", "50").toInt()

    // -------------------------------------------------------------------------------------------------
    private fun loadSecrets(): Properties {
        val f = File("secrets.properties")
        if (!f.exists()) {
            try {
                FileOutputStream("secrets.properties").use { os ->
                    secrets.setProperty("my_anime_list_client_id", "")
                    secrets.store(os, "")
                }
            } catch (e: IOException) {
                mLOG.error(e.message, e)
            }
        }
        try {
            FileInputStream("secrets.properties").use { fs ->
                secrets.load(fs)
                return secrets
            }
        } catch (e: IOException) {
            mLOG.error(e.message, e)
            throw Exception("Erro ao carregar o secrets")
        }
    }

    val myAnimeListClient: String get() = secrets.getProperty("my_anime_list_client_id", "")
}