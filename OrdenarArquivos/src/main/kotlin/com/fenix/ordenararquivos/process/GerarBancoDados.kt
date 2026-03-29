package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.MangaServices
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.regex.Pattern

object GerarBancoDados {

    private val LOG = LoggerFactory.getLogger(GerarBancoDados::class.java)

    private var SERVICE: MangaServices? = null

    private const val CAPA = "[\\d]+ capa$"
    private const val VOLUME = "((?i)\\bvolume\\b [\\d.]+)"
    private const val CAPITULO = "((?i)\\bcapítulo\\b [\\d.]+)"
    private const val EXTRA = "((?i)\\bextra\\b [\\d.]+)"
    private const val ARQUIVO_SUFIX = " (Jap)"

    fun processar(diretorio: String) {
        if (diretorio.isEmpty()) {
            LOG.info("Necessário informar um caminho para processar os arquivos.")
            return
        }

        var origem = File(diretorio)
        if (!origem.exists()) {
            LOG.error("Caminho não localizado.")
            return
        }

        if (origem.isFile)
            origem = File(origem.path)

        SERVICE = MangaServices()

        var manga = Manga()
        var nome = ""
        var volume = ""
        var capitulo = ""
        var arquivo = ""
        var quantidade: Int = 1
        var capitulos: String

        for (pasta in origem.listFiles()!!) {
            val processar = pasta.name
            val matcherCapa = Pattern.compile(CAPA, Pattern.CASE_INSENSITIVE).matcher(processar)
            if (pasta.isFile || matcherCapa.find()) continue
            
            LOG.info("Processando pasta: $processar")
            
            val regex = Regex("""^(.*?)\s+Volume\s+(\d+)\s+Capítulo\s+([\d.]+)""", RegexOption.IGNORE_CASE)
            val match = regex.find(processar)

            val volumeMatcher = Pattern.compile(VOLUME, Pattern.CASE_INSENSITIVE).matcher(processar)
            val pastaVolume = if (match != null) "Volume " + (match.groups[2]?.value ?: "")
                             else if (volumeMatcher.find()) volumeMatcher.group()
                             else ""

            val capituloMatcher = Pattern.compile(CAPITULO, Pattern.CASE_INSENSITIVE).matcher(processar)
            val pastaCapitulo = if (match != null) match.groups[3]?.value ?: ""
                              else if (capituloMatcher.find()) capituloMatcher.group().replace("(?i)capítulo\\s+".toRegex(), "")
                              else ""
            
            var pastaNome = if (match != null) match.groups[1]?.value?.trim() ?: processar
                           else processar.replace(pastaVolume, "").replace("(?i)capítulo\\s+$pastaCapitulo".toRegex(), "").trim { it <= ' ' }

            if (pastaNome.endsWith("-")) pastaNome = pastaNome.substring(0, pastaNome.length - 1).trim { it <= ' ' }
            
            // Verifica se mudou o manga (agrupamento por Nome e Volume)
            if (!pastaNome.equals(manga.nome, ignoreCase = true) || !pastaVolume.equals(manga.volume, ignoreCase = true)) {
                if (!manga.nome.isEmpty()) {
                    finalizarManga(manga)
                }
                manga = Manga(id = 0, nome = pastaNome, volume = pastaVolume, capitulo = pastaCapitulo, arquivo = "", quantidade = 0, capitulos = "", atualizacao = LocalDateTime.now())
            }

            val numArquivos = pasta.listFiles()?.size ?: 0
            manga.addCaminhos(
                Caminhos(
                    manga,
                    pastaCapitulo.replace("[\\D]".toRegex(), "").trim { it <= ' ' },
                    numArquivos,
                    pasta.name,
                    ""
                )
            )
            manga.quantidade += numArquivos
        }

        if (!manga.nome.isEmpty()) {
            finalizarManga(manga)
        }
        LOG.info("Fim do processamento.")
    }

    private fun finalizarManga(manga: Manga) {
        var capitulosStr = ""
        for (caminho in manga.caminhos) {
            capitulosStr += if (caminho.numero <= 1) "${caminho.capitulo}- " else "${caminho.capitulo}-${caminho.numero} "
        }
        manga.capitulos = capitulosStr.trim()
        manga.arquivo = manga.nome + " " + manga.volume + ARQUIVO_SUFIX + ".cbr"
        LOG.info("Salvando manga: ${manga.nome} - ${manga.volume} (${manga.caminhos.size} caminhos)")
        SERVICE!!.save(manga)
    }
}