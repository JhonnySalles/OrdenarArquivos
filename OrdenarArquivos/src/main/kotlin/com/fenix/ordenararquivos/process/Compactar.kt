package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.comet.CoMet
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicPageType
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.util.Utils
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.util.Callback
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*


object Compactar {

    private val mLOG = LoggerFactory.getLogger(Compactar::class.java)

    private val mServiceComicInfo = ComicInfoServices()

    private var mProcess: Process? = null
    private fun compactaArquivo(rar: File, arquivos: List<File>): Boolean {
        var success = true
        var compactar = ""
        for (arquivo in arquivos)
            compactar += '"'.toString() + arquivo.path + '"' + ' '
        val comando = "rar a -ma4 -ep1 " + '"' + rar.path + '"' + " " + compactar
        mLOG.info(comando)
        return try {
            val rt = Runtime.getRuntime()
            mProcess = rt.exec(comando)
            Platform.runLater {
                try {
                    mLOG.info("Resultado: " + mProcess!!.waitFor())
                } catch (e: InterruptedException) {
                    mLOG.error("Erro ao executar o comando.", e)
                }
            }
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(mProcess!!.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null) resultado += "$s"
            if (resultado.isNotEmpty())
                mLOG.info("Output comand:\n$resultado")
            s = null
            resultado = ""
            val stdError = BufferedReader(InputStreamReader(mProcess!!.errorStream))
            while (stdError.readLine().also { s = it } != null) resultado += "$s"
            if (resultado.isNotEmpty()) {
                success = false
                mLOG.info("Error comand: $resultado Necessário adicionar o rar no path e reiniciar a aplicação. ".trimIndent())
            }
            success
        } catch (e: Exception) {
            mLOG.error("Erro ao compactar o arquivo.", e)
            false
        } finally {
            if (mProcess != null)
                mProcess!!.destroy()
        }
    }

    fun compactar(destino : File, zip : File, manga : Manga, comicInfo: ComicInfo, pastas: MutableList<File>, comic : MutableMap<String, File>, linguagem: Linguagem,
                  isCompactar : Boolean, isGerarCapitulos: Boolean, isAtualizarComic: Boolean = true, callback: Callback<Triple<Long, Long, String>, Boolean>) : Boolean {
        if (callback.call(Triple(0 ,0, "Gerando o comic info..")))
            return false

        val imagens = ".*\\.(jpg|jpeg|bmp|gif|png|webp)$".toRegex()

        var i = 0L
        var max = 0L
        for (key in comic.keys) {
            if (comic[key]!!.listFiles() != null)
                for (capa in comic[key]!!.listFiles()!!)
                    max++
        }

        var pagina = 0
        val pages = mutableListOf<Pages>()
        var capitulo = ""
        for (key in comic.keys) {
            val pasta = comic[key]!!
            if (pasta.listFiles() != null)
                for (capa in pasta.listFiles()!!) {
                    if (!capa.name.lowercase(Locale.getDefault()).matches(imagens))
                        continue

                    mLOG.info("Gerando pagina do ComicInfo: " + capa.name)
                    i++
                    if (callback.call(Triple(i ,max, "Processando item " + i + " de " + max + ". Gerando ComicInfo - Capítulo $key | " + capa.name)))
                        return false

                    val page = Pages()
                    val imagem: String = capa.name.lowercase(Locale.getDefault())
                    if (imagem.contains("frente")) {
                        page.bookmark = "Cover"
                        page.type = ComicPageType.FrontCover
                    } else if (imagem.contains("tras")) {
                        page.bookmark = "Back"
                        page.type = ComicPageType.BackCover
                    } else if (imagem.contains("tudo")) {
                        page.bookmark = "All cover"
                        page.doublePage = true
                        page.type = ComicPageType.Other
                    } else if (imagem.contains("zsumário") || imagem.contains("zsumario")) {
                        page.bookmark = "Sumary"
                        page.type = ComicPageType.InnerCover
                    } else {
                        if (!capitulo.equals(key, true) && !key.equals("000", true) && !key.lowercase().contains("extra")) {
                            capitulo = key
                            val tag = if (isGerarCapitulos) {
                                val caminho = manga.caminhos.stream().filter { it.capitulo.equals(key, ignoreCase = true) }.findFirst()
                                if (caminho.isPresent && caminho.get().tag.isNotEmpty())
                                    " - " + caminho.get().tag
                                else
                                    ""
                            } else
                                ""
                            page.bookmark = when (linguagem) {
                                Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(capitulo)}話$tag"
                                Linguagem.ENGLISH -> "Chapter $capitulo$tag"
                                Linguagem.PORTUGUESE -> "Capítulo $capitulo$tag"
                                else -> "Capítulo $capitulo$tag"
                            }
                        }
                    }

                    try {
                        val input = FileInputStream(capa)
                        val image = Image(input)
                        page.imageWidth = image.width.toInt()
                        page.imageHeight = image.height.toInt()
                        input.close()
                    } catch (e: IOException) {
                        mLOG.error("Erro ao obter os tamanhos da imagem.", e)
                    }
                    if (page.imageWidth != null && page.imageHeight != null && page.imageHeight!! > 0)
                        if (page.imageWidth!! / page.imageHeight!! > 0.9)
                            page.doublePage = true

                    page.imageSize = capa.length()
                    page.image = pagina++
                    pages.add(page)
                }
        }

        max = 3
        i = 1
        if (callback.call(Triple(i ,max, "Salvando xml do ComicInfo...")))
            return false
        comicInfo.pages = pages
        comicInfo.pageCount = pages.size

        if (manga.caminhos.stream().anyMatch { it.tag.isNotEmpty() }) {
            var sumary = "*Chapter Titles*\n"
            for (key in comic.keys) {
                if (key.equals("000", ignoreCase = true))
                    continue

                val caminho = manga.caminhos.stream().filter { it.capitulo.equals(key, ignoreCase = true) }.findFirst()
                sumary += "Chapter $key" + (if (caminho.isPresent) ": " + caminho.get().tag else "") + "\n"
            }
            comicInfo.summary = if (comicInfo.summary.isNullOrEmpty())
                sumary
            else if (comicInfo.summary!!.contains("*Chapter Titles*")) {
                val old = comicInfo.summary!!.substringBeforeLast("*Chapter Titles*").trim()
                if (old.isEmpty())
                    sumary
                else
                    old + "\n\n" + sumary
            } else
                comicInfo.summary + "\n\n" + sumary
        }

        comicInfo.let {
            if (it.comic.isEmpty())
                it.comic = manga.nome
            if (it.title.isEmpty())
                it.title = manga.nome

            if (isAtualizarComic) {
                it.number = manga.volume.replace(Regex("\\D"), "").toFloat()
                it.volume = it.number.toInt()
                it.count = it.volume
            }

            it.languageISO = when (linguagem) {
                Linguagem.JAPANESE -> "ja"
                Linguagem.ENGLISH -> "en"
                Linguagem.PORTUGUESE -> "pt"
                else -> "pt"
            }
        }

        mServiceComicInfo.save(comicInfo)

        val arquivoComic = File(destino.path.trim { it <= ' ' }, "ComicInfo.xml")
        if (arquivoComic.exists())
            arquivoComic.delete()

        try {
            mLOG.info("Salvando xml do ComicInfo.")
            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(arquivoComic)
            marshaller.marshal(comicInfo, out)
            out.close()
            pastas.add(arquivoComic)
        } catch (e: Exception) {
            mLOG.error("Erro ao gerar o xml do ComicInfo.", e)
        }

        i++
        if (callback.call(Triple(i ,max, "Salvando xml do CoMet...")))
            return false

        val arquivoComet = File(destino.path.trim { it <= ' ' }, "CoMet.xml")
        if (arquivoComet.exists())
            arquivoComet.delete()

        val paths = mutableListOf<String>()
        for (pasta in comic) {
            val path = pasta.value.path.substringAfter(destino.path).replace("\\", "/")
            if (pasta.value.listFiles() != null) {
                for (image in pasta.value.listFiles()!!)
                    paths.add(path + "/" + image.name)
            }
        }

        try {
            mLOG.info("Salvando xml do CoMet.")
            val marshaller = JAXBContext.newInstance(CoMet::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(arquivoComet)
            marshaller.marshal(CoMet(comicInfo, paths), out)
            out.close()
            pastas.add(arquivoComet)
        } catch (e: Exception) {
            mLOG.error("Erro ao gerar o xml do CoMet.", e)
        }

        comic.clear()

        return if (isCompactar) {
            i++
            callback.call(Triple(i ,max, "Compactando arquivo: $zip"))
            if (zip.exists())
                zip.delete()

            if (!compactaArquivo(zip, pastas))
                callback.call(Triple(-1,-1, "Erro ao gerar o arquivo, necessário compacta-lo manualmente."))
            true
        } else
            false

    }

}