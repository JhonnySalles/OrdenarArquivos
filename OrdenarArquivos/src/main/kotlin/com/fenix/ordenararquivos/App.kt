package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.model.enums.Argumentos
import com.fenix.ordenararquivos.process.CopiarOpfEpub
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*

object App {

    private val mLog = LoggerFactory.getLogger(Run::class.java)

    private fun inicializarSentry() {
        if (System.getProperty("is.test") == "true") {
            mLog.info("Sentry ignorado em ambiente de teste.")
            return
        }

        try {
            if (!File("secrets.properties").exists()) {
                mLog.warn("Aviso: Arquivo secrets.properties não encontrado.")
                return
            }

            val properties = Properties()
            properties.load(FileInputStream("secrets.properties"))
            val dsn = properties.getProperty("sentry_dns")
            val environment = properties.getProperty("sentry_environment")

            if (!dsn.isNullOrBlank()) {
                Sentry.init { options ->
                    options.dsn = dsn
                    options.environment = if (!environment.isNullOrBlank()) environment else "development"
                    options.tracesSampleRate = 1.0
                }
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    mLog.error("Erro fatal não capturado na thread ${thread.name} -- ${throwable.message}", throwable)
                }
                mLog.info("Sentry inicializado com sucesso!")
            } else
                mLog.warn("Aviso: Chave do Sentry não encontrada no secrets.properties.")
        } catch (e: Exception) {
            mLog.error("Falha ao inicializar o Sentry: ${e.message}")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        inicializarSentry()
        var origem = ""
        var destino = ""
        var tipo: Argumentos? = null
        for (a in args) when (a) {
            Argumentos.OPF.description -> tipo = Argumentos.OPF
            else -> {
                if (a.contains("origem")) origem = a.substring(a.indexOf("=") + 1).replace("\"", "")
                else if (a.contains("destino"))
                        destino = a.substring(a.indexOf("=") + 1).replace("\"", "")
            }
        }

        if (tipo == null) Run().start(args)
        else
                when (tipo) {
                    Argumentos.OPF -> CopiarOpfEpub.processar(origem, destino)
                }
    }
}
