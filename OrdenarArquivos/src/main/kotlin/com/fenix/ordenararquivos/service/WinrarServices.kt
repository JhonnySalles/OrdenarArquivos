package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.process.Winrar
import com.fenix.ordenararquivos.util.Utils
import java.io.File

open class WinrarServices {

    open fun insereArquivo(rar: File, arquivos: List<File>): Boolean = Winrar.compactarArquivo(rar, arquivos)

    open fun insereArquivo(rar: File, arquivos: File): Boolean = insereArquivo(rar, listOf(arquivos))

    open fun insereComicInfo(arquivo: File, info: File) = insereArquivo(arquivo, listOf(info))

    open fun extraiComicInfo(arquivo: File): File? = Winrar.extrairArquivo(arquivo, Utils.COMICINFO)

    open fun insereSumario(arquivo: File, sumario: File) = insereArquivo(arquivo, listOf(sumario))

    open fun extraiSumario(arquivo: File, pasta: File): File? {
        for (arquivos in pasta.listFiles()!!) {
            if (arquivos.name.contains("zSumário", ignoreCase = true))
                arquivos.delete()
        }

        return Winrar.extrairArquivo(arquivo, "*zSumário.*")
    }

}
