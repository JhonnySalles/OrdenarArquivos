package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.Caminhos
import com.fenix.ordenararquivos.model.Manga
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
            var matcher = Pattern.compile(CAPA, Pattern.CASE_INSENSITIVE).matcher(processar)
            if (pasta.isFile || matcher.find()) continue
            LOG.info("Processando pasta: $processar")
            matcher = Pattern.compile(VOLUME, Pattern.CASE_INSENSITIVE).matcher(processar)
            volume = if (matcher.find()) matcher.group() else processar.split(
                processar.replace(VOLUME.toRegex(), "").toRegex()
            ).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1]

            matcher = Pattern.compile(EXTRA, Pattern.CASE_INSENSITIVE).matcher(processar)
            if (matcher.find()) capitulo = matcher.group() else {
                matcher = Pattern.compile(CAPITULO, Pattern.CASE_INSENSITIVE).matcher(processar)
                capitulo = if (matcher.find()) matcher.group() else processar.split(
                    processar.replace(CAPITULO.toRegex(), "").toRegex()
                ).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1]
            }

            nome =
                if (processar.contains("]")) processar.substring(processar.indexOf("]")).replace("]", "") else processar
            nome = nome.replace(volume, "").replace(capitulo, "").trim { it <= ' ' }

            if (nome.endsWith("-")) nome = nome.substring(0, nome.length - 1).trim { it <= ' ' }
            manga.addCaminhos(
                Caminhos(
                    0,
                    manga,
                    capitulo.replace("[\\D]".toRegex(), "").trim { it <= ' ' },
                    quantidade,
                    capitulo
                )
            )
            quantidade = quantidade!! + pasta.listFiles().size
            if (!nome.equals(manga.nome, ignoreCase = true) || !volume.equals(manga.volume, ignoreCase = true)) {
                capitulos = ""
                for (caminho in manga.caminhos) capitulos += if (caminho.numero.compareTo(1) == 0) """
     ${caminho.capitulo}-
     
     """.trimIndent() else """
     ${caminho.capitulo}-${caminho.numero}
     
     """.trimIndent()
                if (!capitulos.isEmpty()) capitulos += capitulos.substring(0, capitulos.length - 1)
                manga.capitulos = capitulos
                manga.arquivo = manga.nome + " " + manga.volume + ARQUIVO_SUFIX + ".cbr"
                LOG.info("Salvando manga: " + manga.nome + " - " + manga.volume)
                if (!manga.nome.isEmpty()) SERVICE!!.save(manga)
                manga = Manga(0, nome, volume, capitulo, arquivo, quantidade, "", LocalDateTime.now())
                arquivo = ""
                quantidade = 1
            }
        }
        if (!manga.nome.isEmpty()) {
            capitulos = ""
            for (caminho in manga.caminhos) capitulos += if (caminho.numero.compareTo(1) == 0) """
     ${caminho.capitulo}-
     
     """.trimIndent() else """
     ${caminho.capitulo}-${caminho.numero}
     
     """.trimIndent()
            if (!capitulos.isEmpty()) capitulos += capitulos.substring(0, capitulos.length - 1)
            manga.capitulos = capitulos
            manga.arquivo = manga.nome + " " + manga.volume + ARQUIVO_SUFIX + ".cbr"
            LOG.info("Salvando manga: " + manga.nome + " - " + manga.volume)
            if (!manga.nome.isEmpty()) SERVICE!!.save(manga)
        }
        LOG.info("Fim do processamento.")
        DataBase.closeConnection()
    }
}