package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.util.Utils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

open class WinrarServices {
    private val mLOG = LoggerFactory.getLogger(WinrarServices::class.java)

    open fun extraiComicInfo(arquivo: File): File? {
        var comicInfo: File? = null
        var proc: Process? = null
        val comando = "rar e -ma4 -y " + '"' + arquivo.path + '"' + " " + '"' + Utils.getCaminho(arquivo.path) + '"' + " " + '"' + Utils.COMICINFO + '"'
        try {
            val rt: Runtime = Runtime.getRuntime()
            proc = rt.exec(comando)
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null)
                resultado += "$s"

            s = null
            var error = ""
            val stdError = BufferedReader(InputStreamReader(proc.errorStream))
            while (stdError.readLine().also { s = it } != null)
                error += "$s"
            if (resultado.isEmpty() && error.isNotEmpty())
                mLOG.info("Error comand: $resultado Não foi possível extrair o arquivo ${Utils.COMICINFO}.")
            else
                comicInfo = File(Utils.getCaminho(arquivo.path) + '\\' + Utils.COMICINFO)
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        } finally {
            proc?.destroy()
        }
        return comicInfo
    }

    open fun insereComicInfo(arquivo: File, info: File) {
        val comando = "rar a -ma4 -ep1 " + '"' + arquivo.path + '"' + " " + '"' + info.path + '"'
        var proc: Process? = null
        try {
            val rt: Runtime = Runtime.getRuntime()
            proc = rt.exec(comando)
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
            var s: String? = null
            while (stdInput.readLine().also { s = it } != null)
                resultado += "$s"

            if (resultado.isNotEmpty())
                mLOG.info("Output comand:\n$resultado")
            s = null
            var error = ""
            val stdError = BufferedReader(InputStreamReader(proc.errorStream))
            while (stdError.readLine().also { s = it } != null)
                error += "$s"

            if (resultado.isEmpty() && error.isNotEmpty()) {
                info.renameTo(File(arquivo.path + Utils.getNome(arquivo.name) + Utils.getExtenssao(info.name)))
                mLOG.info("Error comand:\n$resultado\nNecessário adicionar o rar no path e reiniciar a aplicação.")
            } else
                info.delete()
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        } finally {
            proc?.destroy()
        }
    }

    open fun extraiSumario(arquivo: File, pastaTemporaria: File): File? {
        var sumario: File? = null
        var proc: Process? = null

        for (arquivos in pastaTemporaria.listFiles()!!) {
            if (arquivos.name.contains("zSumário", ignoreCase = true))
                arquivos.delete()
        }

        val comando = "rar e -ma4 -y " + '"' + arquivo.path + '"' + " " + '"' + pastaTemporaria.path + '"' + " " + '"' + "*zSumário.*" + '"'
        try {
            val rt: Runtime = Runtime.getRuntime()
            proc = rt.exec(comando)
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null)
                resultado += "$s"

            s = null
            var error = ""
            val stdError = BufferedReader(InputStreamReader(proc.errorStream))
            while (stdError.readLine().also { s = it } != null)
                error += "$s"
            if (resultado.isEmpty() && error.isNotEmpty())
                mLOG.info("Error comand: $resultado Não foi possível extrair o sumário.")
            else {
                for (arquivos in pastaTemporaria.listFiles()!!) {
                    if (arquivos.name.contains("zSumário", ignoreCase = true)) {
                        sumario = arquivos
                        break
                    }
                }
            }
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        } finally {
            proc?.destroy()
        }
        return sumario
    }

    open fun processOcr(sumario: File, separadorPagina: String, separadorCapitulo: String): String {
        return Ocr.process(sumario, separadorPagina, separadorCapitulo)
    }
}
