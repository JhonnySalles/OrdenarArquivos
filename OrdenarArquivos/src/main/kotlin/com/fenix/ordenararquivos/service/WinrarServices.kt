package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.process.Winrar
import com.fenix.ordenararquivos.util.Utils
import javafx.util.Callback
import java.io.File

open class WinrarServices {

    open fun insereArquivo(rar: File, arquivos: List<File>): Boolean = Winrar.compactarArquivo(rar, arquivos)

    open fun insereArquivo(rar: File, arquivos: File): Boolean = insereArquivo(rar, listOf(arquivos))

    open fun insereComicInfo(arquivo: File, info: File) = insereArquivo(arquivo, listOf(info))

    open fun extraiComicInfo(arquivo: File): File? = Winrar.extrairArquivo(arquivo, Utils.COMICINFO)

    open fun extrairTudo(arquivo: File, destino: File): Boolean = Winrar.extrairTudo(arquivo, destino)
    
    open fun listarConteudo(arquivo: File): List<String> = Winrar.listarConteudo(arquivo)

    open fun extrairItens(arquivo: File, itens: List<String>, destino: File): Boolean = Winrar.extrairItens(arquivo, itens, destino)

    open fun insereSumario(arquivo: File, sumario: File) = insereArquivo(arquivo, listOf(sumario))

    open fun extraiSumario(arquivo: File, pasta: File): File? {
        for (arquivos in pasta.listFiles()!!) {
            if (arquivos.name.contains("zSumário", ignoreCase = true))
                arquivos.delete()
        }

        return Winrar.extrairArquivo(arquivo, "*zSumário.*")
    }

    open fun compactar(destino: File, zip: File, manga: Manga, comicInfo: ComicInfo, pastas: MutableList<File>, comic: MutableMap<String, File>, linguagem: Linguagem,
                      isCompactar: Boolean, isGerarCapitulos: Boolean, isAtualizarComic: Boolean = true, callback: Callback<Triple<Long, Long, String>, Boolean>): Boolean {
        return Winrar.compactar(destino, zip, manga, comicInfo, pastas, comic, linguagem, isCompactar, isGerarCapitulos, isAtualizarComic, callback)
    }

}
