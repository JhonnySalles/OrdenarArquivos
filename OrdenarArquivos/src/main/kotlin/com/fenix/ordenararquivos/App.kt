package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.model.Argumentos
import com.fenix.ordenararquivos.process.CopiarOpfEpub
import com.fenix.ordenararquivos.process.GerarBancoDados

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        var origem = ""
        var destino = ""
        var tipo: Argumentos? = null
        for (a in args)
            when (a) {
                Argumentos.BANCO.description -> tipo = Argumentos.BANCO
                Argumentos.OPF.description -> tipo = Argumentos.OPF
                else -> {
                    if (a.contains("origem"))
                        origem = a.substring(a.indexOf("=") + 1).replace("\"", "")
                    else if (a.contains("destino"))
                        destino = a.substring(a.indexOf("=") + 1).replace("\"", "")
                }
            }

        if (tipo == null)
            Run().start(args)
        else
            when (tipo) {
                Argumentos.BANCO -> GerarBancoDados.processar(origem)
                Argumentos.OPF -> CopiarOpfEpub.processar(origem, destino)
            }
    }
}