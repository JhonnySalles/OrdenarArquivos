package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.process.GerarBancoDados

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        var caminho = ""
        var gerarBanco = false
        for (a in args) if (a.contains("gerarBanco")) {
            gerarBanco = true
            caminho = a.substring(a.indexOf("=") + 1).replace("\"", "")
        }
        if (gerarBanco)
            GerarBancoDados.processar(caminho)
        else
            Run().start(args)
    }
}