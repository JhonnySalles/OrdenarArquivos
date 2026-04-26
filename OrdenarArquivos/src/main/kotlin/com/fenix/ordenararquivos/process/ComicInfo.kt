package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.fileparse.ParseFactory
import com.fenix.ordenararquivos.fileparse.RarParse
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo as Comic
import com.fenix.ordenararquivos.model.entities.comicinfo.Manga
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicPageType
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.util.Utils
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import javafx.scene.image.Image
import javafx.util.Callback
import javafx.util.Pair
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import kotlin.also
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.jvm.java
import kotlin.ranges.until
import kotlin.text.contains
import kotlin.text.endsWith
import kotlin.text.equals
import kotlin.text.indexOf
import kotlin.text.isEmpty
import kotlin.text.isNotEmpty
import kotlin.text.lastIndexOf
import kotlin.text.lowercase
import kotlin.text.matches
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.substring
import kotlin.text.substringAfterLast
import kotlin.text.substringBefore
import kotlin.text.substringBeforeLast
import kotlin.text.toFloat
import kotlin.text.toInt
import kotlin.text.toRegex
import kotlin.text.trim

object ComicInfo {

    private val mLOG = LoggerFactory.getLogger(ComicInfo::class.java)

    private val PATTERN = ".*\\.(zip|cbz|rar|cbr|tar)$".toRegex()
    private const val COMICINFO = "ComicInfo.xml"
    private var mJaxBc: JAXBContext? = null
    private var nCancelar = false
    private var mMarcarCapitulo: String = ""
    private var mService: ComicInfoServices? = null

    fun cancelar() {
        nCancelar = true
    }

    fun processa(linguagem: Linguagem, path: String, marcaCapitulo: String, callback: Callback<Array<Long>, Boolean>) {
        nCancelar = false
        mMarcarCapitulo = marcaCapitulo
        val arquivos = File(path)
        val size: Array<Long> = arrayOf(0, 0)
        try {
            mJaxBc = JAXBContext.newInstance(Comic::class.java)
            mService = ComicInfoServices()
            if (arquivos.isDirectory) {
                size[0] = 0
                size[1] = arquivos.listFiles()?.size?.toLong() ?: 1
                callback.call(size)
                for (arquivo in arquivos.listFiles()!!) {
                    processa(linguagem, arquivo)
                    size[0]++
                    callback.call(size)
                    if (nCancelar)
                        break
                }
            } else if (arquivos.isFile) {
                size[0] = 0
                size[1] = 1
                callback.call(size)
                processa(linguagem, arquivos)
                size[0]++
                callback.call(size)
            }
        } catch (t: Throwable) {
            mLOG.error("ERRO CRÍTICO no processamento de ComicInfo para o caminho $path: ${t.message}", t)
            throw t
        } finally {
            if (mJaxBc != null)
                mJaxBc = null
            if (mService != null)
                mService = null
        }
    }

    private fun processa(linguagem: Linguagem, arquivo: File) {
        try {
            if (arquivo.name.lowercase(Locale.getDefault()).matches(PATTERN)) {
                var info: File? = null
                try {
                    info = extraiInfo(arquivo)

                    val comic: Comic = if (info == null || !info.exists()) {
                        if (info == null)
                            info = File(arquivo.absolutePath + File.separator + COMICINFO)

                        val nome = arquivo.name.substringBeforeLast(".")
                        val titulo = nome.substringBeforeLast("-").trim()
                        var vol = nome.lowercase().substringAfterLast("volume")
                        if (vol.contains("("))
                            vol = vol.substringBefore("(").trim()
                        val volume = try {
                            vol.toInt()
                        } catch (e: Exception) {
                            0
                        }

                        Comic(id = UUID.randomUUID(), comic = titulo, title = titulo, volume = volume)
                    } else
                        try {
                            val unmarshaller = mJaxBc!!.createUnmarshaller()
                            unmarshaller.unmarshal(info) as Comic
                        } catch (e: Exception) {
                            mLOG.error(e.message, e)
                            return
                        }

                    var nome: String = Utils.getNome(arquivo.name)

                    if (nome.lowercase(Locale.getDefault()).contains("volume"))
                        nome = nome.substring(0, nome.lowercase(Locale.getDefault()).indexOf("volume"))
                    else if (nome.lowercase(Locale.getDefault()).contains("capitulo"))
                        nome = nome.substring(0, nome.lowercase(Locale.getDefault()).indexOf("capitulo"))
                    else if (nome.lowercase(Locale.getDefault()).contains("capítulo"))
                        nome = nome.substring(0, nome.lowercase(Locale.getDefault()).indexOf("capítulo"))
                    else if (nome.contains("-")) nome =
                        nome.substring(0, nome.lastIndexOf("-"))

                    if (nome.endsWith(" - "))
                        nome = nome.substring(0, nome.lastIndexOf(" - "))

                    mLOG.info("Processando o manga $nome")
                    if (nome.contains("-"))
                        comic.comic = nome.substring(0, nome.lastIndexOf("-")).trim()
                    else if (nome.contains("."))
                        comic.comic = nome.substring(0, nome.lastIndexOf(".")).trim()
                    else
                        comic.comic = nome

                    comic.manga = Manga.Yes
                    comic.languageISO = linguagem.sigla

                    if (comic.title.lowercase(Locale.getDefault()).contains("vol.") || comic.title.lowercase(Locale.getDefault()).contains("volume"))
                        comic.title = comic.series
                    else if (!comic.title.equals(comic.series, true))
                        comic.storyArc = comic.title

                    val titulosCapitulo: MutableList<Pair<Float, String>> = mutableListOf()
                    val getChapterFromPages: () -> List<String> = {
                        val chapters = mutableListOf<String>()
                        comic.pages?.filter { !it.bookmark.isNullOrEmpty() }?.forEach { page ->
                            val bookmark = page.bookmark!!
                            if (bookmark.contains("-")) {
                                val parts = bookmark.split("-", limit = 2)
                                val chapterPart = parts[0].trim()
                                val titlePart = parts[1].trim()

                                val numberMatch = "\\d+(\\.\\d+)?".toRegex().find(chapterPart)
                                if (numberMatch != null)
                                    chapters.add("Chapter ${numberMatch.value}: $titlePart")
                            }
                        }
                        if (chapters.isNotEmpty()) {
                            val sb = StringBuilder()
                            sb.append("*Chapter Titles*\n")
                            chapters.forEach { sb.append(it).append("\n") }
                            comic.summary = sb.toString().trim()
                        }
                        chapters
                    }

                    val linhas: List<String> = if (comic.summary != null && comic.summary!!.isNotEmpty()) {
                        val sumary = comic.summary!!.lowercase(Locale.getDefault())
                        if (sumary.contains("*chapter titles manual*"))
                            comic.summary!!.substring(comic.summary!!.lowercase().indexOf("*chapter titles manual*")).split("\n")
                        else if (sumary.contains("chapter titles") || sumary.contains("chapter list") || sumary.contains("contents"))
                            comic.summary!!.split("\n")
                        else
                            getChapterFromPages()
                    } else
                        getChapterFromPages()

                    for (linha in linhas) {
                        var number = 0f
                        var chapter = ""
                        if (linha.matches("([\\w. ]+[\\d][:|.][\\w\\W]++)|([\\d][:|.][\\w\\W]++)".toRegex())) {
                            var aux: List<String> = listOf()
                            if (linha.contains(":"))
                                aux = linha.split(":")
                            else if (linha.contains(". "))
                                aux = linha.replace(". ", ":").split(":")

                            if (aux.isNotEmpty()) {
                                try {
                                    number = if (aux[0].matches("[a-zA-Z ]+[.][\\d]".toRegex())) // Ex: Act.1: Spring of the Dead
                                        aux[0].replace("[^\\d]".toRegex(), "").toFloat()
                                    else if (aux[0].lowercase(Locale.getDefault()).contains("extra") || aux[0].lowercase(Locale.getDefault()).contains("special"))
                                        -1f
                                    else
                                        aux[0].replace("[^\\d.]".toRegex(), "").toFloat()

                                    chapter = aux[1].trim()
                                } catch (e: Exception) {
                                    mLOG.error(e.message, e)
                                }
                            }
                        }
                        if (number.compareTo(0f) > 0)
                            titulosCapitulo.add(Pair(number, chapter))
                    }

                    val parse = ParseFactory.create(arquivo)
                    try {
                        if (linguagem == Linguagem.PORTUGUESE) {
                            var tradutor = ""
                            for (pasta in parse.getPastas().keys) {
                                var item = ""
                                if (pasta.contains("]")) item = pasta.substring(0, pasta.indexOf("]")).replace("[", "")
                                if (item.isNotEmpty()) {
                                    if (item.contains("&")) {
                                        val itens: List<String> = item.split("&")
                                        for (itm in itens)
                                            if (!tradutor.contains(itm.trim()))
                                                tradutor += itm.trim() + "; "
                                    } else if (!tradutor.contains(item))
                                        tradutor += "$item; "
                                }
                            }
                            if (tradutor.isNotEmpty()) {
                                comic.translator = tradutor.substring(0, tradutor.length - 2)
                                comic.scanInformation = tradutor.substring(0, tradutor.length - 2)
                            }
                        } else if (linguagem == Linguagem.JAPANESE) {
                            comic.translator = ""
                            comic.scanInformation = ""
                        }

                        if (comic.pages == null || comic.pages!!.isEmpty()) {
                            val pages = mutableListOf<Pages>()
                            for (i in 0 until parse.getSize())
                                pages.add(Pages(image = i))
                            comic.pages = pages
                        } else
                            comic.pages?.forEach {
                                it.bookmark = null
                                it.type = null
                            }

                        if (comic.pageCount == null || comic.pageCount == 0)
                            comic.pageCount = comic.pages!!.size

                        val pastas: Map<String, Int> = parse.getPastas()
                        var index = 0
                        for (i in 0 until parse.getSize()) {
                            if (index >= comic.pages!!.size)
                                continue

                            if (Utils.isImage(parse.getPaginaPasta(i))) {
                                val imagem: String = parse.getPaginaPasta(i).lowercase(Locale.getDefault())
                                val page = comic.pages!!.get(index)
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
                                    if (pastas.containsValue(i)) {
                                        var capitulo = ""
                                        for (entry in pastas.entries) {
                                            if (entry.value == i) {
                                                if (entry.key.lowercase(Locale.getDefault()).contains("capitulo"))
                                                    capitulo = entry.key.substring(entry.key.lowercase(Locale.getDefault()).indexOf("capitulo"))
                                                else if (entry.key.lowercase(Locale.getDefault()).contains("capítulo"))
                                                    capitulo = entry.key.substring(entry.key.lowercase(Locale.getDefault()).indexOf("capítulo"))

                                                if (capitulo.isNotEmpty()) {
                                                    if (mMarcarCapitulo.isNotEmpty()) {
                                                        capitulo = if (capitulo.lowercase(Locale.getDefault()).contains("capítulo"))
                                                            capitulo.substring(capitulo.lowercase(Locale.getDefault()).indexOf("capítulo") + 8)
                                                        else
                                                            capitulo.substring(capitulo.lowercase(Locale.getDefault()).indexOf("capitulo") + 8)

                                                        capitulo = if (mMarcarCapitulo.lowercase(Locale.getDefault()).contains("%s")) // Japanese
                                                            mMarcarCapitulo.lowercase(Locale.getDefault()).replace("%s", Utils.toNumberJapanese(capitulo.trim()))
                                                        else
                                                            mMarcarCapitulo + capitulo
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                        if (capitulo.isNotEmpty()) {
                                            if (titulosCapitulo.isNotEmpty()) {
                                                try {
                                                    val number = Utils.fromNumberJapanese(capitulo).replace("[^\\d.]".toRegex(), "").toFloat()
                                                    val titulo: Optional<Pair<Float, String>> = titulosCapitulo.stream().filter { it.key == number }.findFirst()
                                                    if (titulo.isPresent && titulo.get().value.isNotEmpty()) {
                                                        capitulo += " - " + titulo.get().value
                                                        titulosCapitulo.remove(titulo.get())
                                                    }
                                                } catch (e: Exception) {
                                                    mLOG.error(e.message, e)
                                                }
                                            }
                                            page.bookmark = capitulo
                                        }
                                    }
                                    if (page.imageWidth == null || page.imageHeight == null) {
                                        try {
                                            parse.getPagina(i).use { input ->
                                                val image = Image(input)
                                                page.imageWidth = image.width.toInt()
                                                page.imageHeight = image.height.toInt()
                                            }
                                        } catch (e: Exception) {
                                            mLOG.error("Erro ao processar imagem: ${e.message}", e)
                                        }
                                    }
                                    if (page.imageWidth != null && page.imageHeight != null && page.imageHeight!! > 0)
                                        if (page.imageWidth!! / page.imageHeight!! > 0.9)
                                            page.doublePage = true
                                }
                                index++
                            }
                        }
                    } finally {
                        parse?.let {
                            if (it is RarParse)
                                try {
                                    it.destroir()
                                } catch (e: IOException) {
                                    mLOG.error(e.message, e)
                                }
                        }
                    }

                    try {
                        val marshaller = mJaxBc!!.createMarshaller()
                        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                        val out = FileOutputStream(info)
                        marshaller.marshal(comic, out)
                        out.close()
                    } catch (e: Exception) {
                        mLOG.error(e.message, e)
                        return
                    }
                    insereInfo(arquivo, info)
                } finally {
                    info?.delete()
                }
            }
        } catch (t: Throwable) {
            mLOG.error("ERRO CRÍTICO ao processar arquivo ${arquivo.absolutePath}: ${t.message}", t)
            throw t
        }
    }

    private fun extraiInfo(arquivo: File): File? {
        var comicInfo: File? = null
        var proc: Process? = null
        val comando = arrayOf("rar", "e", "-ma4", "-y", arquivo.path, Utils.getCaminho(arquivo.path), COMICINFO)
        try {
            val pb = ProcessBuilder(*comando)
            pb.redirectErrorStream(true)
            proc = pb.start()
            
            val output = StringBuilder()
            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }
            
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                mLOG.info("Erro ao extrair $COMICINFO (Exit Code: $exitCode). Output:\n$output")
            } else {
                comicInfo = File(Utils.getCaminho(arquivo.path) + File.separator + COMICINFO)
            }
        } catch (e: Exception) {
            mLOG.error("Falha ao extrair ComicInfo: ${e.message}", e)
        } finally {
            proc?.destroy()
        }
        return comicInfo
    }

    private fun insereInfo(arquivo: File, info: File) {
        val comando = arrayOf("rar", "a", "-ma4", "-ep1", arquivo.path, info.path)
        mLOG.info("Inserindo $COMICINFO em ${arquivo.name}")
        var proc: Process? = null
        try {
            val pb = ProcessBuilder(*comando)
            pb.redirectErrorStream(true)
            proc = pb.start()

            val output = StringBuilder()
            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }

            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                mLOG.info("Erro ao inserir $COMICINFO (Exit Code: $exitCode). Output:\n$output")
                info.renameTo(File(arquivo.path + Utils.getNome(arquivo.name) + Utils.getExtenssao(info.name)))
            } else {
                info.delete()
            }
        } catch (e: Exception) {
            mLOG.error("Falha ao inserir ComicInfo: ${e.message}", e)
        } finally {
            proc?.destroy()
        }
    }

}