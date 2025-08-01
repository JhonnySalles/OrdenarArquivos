package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.CheckBoxTableCellCustom
import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.util.Callback
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


class AbaComicInfoController : Initializable {

    private val mLOG = LoggerFactory.getLogger(AbaComicInfoController::class.java)

    //<--------------------------  PRINCIPAL   -------------------------->

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var cbLinguagem: JFXComboBox<Linguagem>

    @FXML
    private lateinit var txtPastaProcessar: JFXTextField

    @FXML
    private lateinit var btnPesquisarPastaProcessar: JFXButton

    @FXML
    private lateinit var btnCarregar: JFXButton

    @FXML
    private lateinit var btnOcrProcessar: JFXButton

    @FXML
    private lateinit var btnTagsProcessar: JFXButton

    @FXML
    private lateinit var btnTagsNormaliza: JFXButton

    @FXML
    private lateinit var btnTagsAplicar: JFXButton

    @FXML
    private lateinit var btnSalvarTodos: JFXButton

    @FXML
    private lateinit var btnCapitulos: JFXButton

    @FXML
    private lateinit var tbViewProcessar: TableView<Processar>

    @FXML
    private lateinit var clProcessado: TableColumn<Processar, Boolean>

    @FXML
    private lateinit var clArquivo: TableColumn<Processar, String>

    @FXML
    private lateinit var clSerie: TableColumn<Processar, String>

    @FXML
    private lateinit var clTitulo: TableColumn<Processar, String>

    @FXML
    private lateinit var clEditora: TableColumn<Processar, String>

    @FXML
    private lateinit var clPublicacao: TableColumn<Processar, String>

    @FXML
    private lateinit var clResumo: TableColumn<Processar, String>

    @FXML
    private lateinit var clTags: TableColumn<Processar, String>

    @FXML
    private lateinit var clProcessarOcr: TableColumn<Processar, JFXButton?>

    @FXML
    private lateinit var clProcessarAmazon: TableColumn<Processar, JFXButton?>

    @FXML
    private lateinit var clSalvarComicInfo: TableColumn<Processar, JFXButton?>

    private lateinit var controller: TelaInicialController
    var controllerPai: TelaInicialController
        get() = controller
        set(controller) {
            this.controller = controller
        }

    private var mObsListaProcessar: ObservableList<Processar> = FXCollections.observableArrayList()
    private val mDecimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))

    @FXML
    private fun onBtnCapitulos() {
        val callback: Callback<ObservableList<Volume>, Boolean> = Callback<ObservableList<Volume>, Boolean> { param ->
            val linguagem = cbLinguagem.value
            val separador = Utils.SEPARADOR_CAPITULO
            for (volume in param)
                if (volume.marcado && volume.arquivo.isNotEmpty()) {
                    val item = mObsListaProcessar.find { it.arquivo == volume.arquivo } ?: continue
                    val capitulos = volume.capitulos.toMutableList()
                    val tags = mutableListOf<String>()
                    for (tag in item.tags.split("\n")) {
                        var capitulo = if (tag.contains(Utils.SEPARADOR_IMPORTACAO)) tag.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim() else tag

                        capitulo.lowercase().substringAfter(Utils.SEPARADOR_IMAGEM).let {
                            if (it.contains("第") || it.contains("chapter") || it.contains("capítulo")) {
                                val numero = Utils.getNumber(if (it.lowercase().contains("第")) Utils.fromNumberJapanese(it) else it)
                                capitulos.find { c -> c.capitulo == numero }?.run {
                                    capitulos.remove(this)
                                    capitulo += " ${Utils.SEPARADOR_IMPORTACAO} " + mDecimal.format(this.capitulo) + separador + if (linguagem == Linguagem.JAPANESE && this.japones.isNotEmpty()) this.japones else this.ingles
                                }
                            }
                        }
                        tags.add(capitulo)
                    }

                    if (capitulos.isNotEmpty())
                        for (capitulo in capitulos)
                            tags.add("-1${Utils.SEPARADOR_IMAGEM} Capítulo novo ${Utils.SEPARADOR_IMPORTACAO} ${mDecimal.format(capitulo.capitulo) + separador + if (linguagem == Linguagem.JAPANESE && capitulo.japones.isNotEmpty()) capitulo.japones else capitulo.ingles}")

                    item.tags = tags.joinToString("\n")
                }
            tbViewProcessar.refresh()
            null
        }
        PopupCapitulos.abreTelaCapitulos(controllerPai.rootStack, controllerPai.rootTab, callback, cbLinguagem.value, mObsListaProcessar)
    }

    @FXML
    private fun onBtnCarregar() {
        carregarItens()
    }

    @FXML
    private fun onBtnOcrProcessar() {
        if (mObsListaProcessar.isNotEmpty()) {
            if (btnOcrProcessar.accessibleTextProperty().value.equals("PROCESSA", ignoreCase = true)) {
                btnOcrProcessar.accessibleTextProperty().set("CANCELA")
                btnOcrProcessar.text = "Cancelar"
                controllerPai.setCursor(Cursor.WAIT)
                desabilita()
                processaOCR()
            } else
                mCANCELAR = true
        }
    }

    @FXML
    private fun onBtnTagsProcessar() {
        if (mObsListaProcessar.isNotEmpty()) {
            controllerPai.setCursor(Cursor.WAIT)
            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
            for (item in mObsListaProcessar)
                gerarTagItem(item, language)
            tbViewProcessar.refresh()
            controllerPai.setCursor(null)
        }
    }

    @FXML
    private fun onBtnTagsNormaliza() {
        if (mObsListaProcessar.isNotEmpty()) {
            controllerPai.setCursor(Cursor.WAIT)
            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
            for (item in mObsListaProcessar)
                normalizarTagItem(item, language)
            tbViewProcessar.refresh()
            controllerPai.setCursor(null)
        }
    }

    @FXML
    private fun onBtnTagsAplicar() {
        if (mObsListaProcessar.isNotEmpty()) {
            controllerPai.setCursor(Cursor.WAIT)
            for (item in mObsListaProcessar) {
                if (item.tags.isEmpty() || !item.tags.contains("\n") || !item.tags.contains(Utils.SEPARADOR_IMPORTACAO))
                    continue

                val linhas = mutableListOf<String>()
                val separador = Utils.SEPARADOR_CAPITULO
                for (linha in item.tags.split("\n"))
                    linhas.add(if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) linha.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim() + " - " + linha.substringAfterLast(separador) else linha)

                item.tags = linhas.joinToString(separator = "\n")
            }
            tbViewProcessar.refresh()
            controllerPai.setCursor(null)
        }
    }

    @FXML
    private fun onBtnSalvarTodos() {
        if (mObsListaProcessar.isNotEmpty())
            salvarItens(startIndex = 0, endIndex = mObsListaProcessar.size)
    }

    @FXML
    private fun onBtnCarregarPasta() {
        val caminho = Utils.selecionaPasta(txtPastaProcessar.text)
        if (caminho != null)
            txtPastaProcessar.text = caminho.absolutePath
        else
            txtPastaProcessar.text = ""
        carregarItens()
    }

    private fun desabilita() {
        txtPastaProcessar.isDisable = true
        btnPesquisarPastaProcessar.isDisable = true
        btnCarregar.isDisable = false
        btnTagsProcessar.isDisable = false
        btnTagsNormaliza.isDisable = false
        btnTagsAplicar.isDisable = false
        btnSalvarTodos.isDisable = false
        btnCapitulos.isDisable = false
        tbViewProcessar.isDisable = false
    }

    private fun habilita() {
        txtPastaProcessar.isDisable = false
        btnPesquisarPastaProcessar.isDisable = false
        btnCarregar.isDisable = false
        btnTagsProcessar.isDisable = false
        btnTagsNormaliza.isDisable = false
        btnTagsAplicar.isDisable = false
        btnSalvarTodos.isDisable = false
        btnCapitulos.isDisable = false
        tbViewProcessar.isDisable = false
        btnOcrProcessar.accessibleTextProperty().set("PROCESSA")
        btnOcrProcessar.text = "OCR proximos 10"
        controllerPai.setCursor(null)
    }

    private var mCANCELAR = false
    private fun processaOCR() {
        val separador = Utils.SEPARADOR_CAPITULO
        val processaOCR: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                try {
                    mCANCELAR = false
                    val max = 10L
                    var i = 0
                    updateMessage("Processando o OCR...")

                    for (item in mObsListaProcessar) {
                        if (item.isProcessado)
                            continue
                        i++
                        if (mCANCELAR || i > max)
                            break

                        updateProgress(i.toLong(), max)
                        updateMessage("Processando item " + i + " de " + max + ". Processando OCR - " + item.arquivo)

                        val sumario = extraiSumario(item.file!!)
                        if (sumario == null) {
                            i--
                            continue
                        }

                        val capitulos = Ocr.processaGemini(sumario, separador).split("\n")
                        val newTag = mutableSetOf<String>()
                        val tags = item.comicInfo!!.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toList() ?: emptyList()

                        var index = -1
                        for (tag in tags) {
                            if (tag.contains("capítulo", ignoreCase = true) || tag.contains("第", ignoreCase = true)) {
                                index++
                                val capitulo = if (index < capitulos.size) capitulos[index] else ""
                                newTag.add("$tag ${Utils.SEPARADOR_IMPORTACAO} $capitulo")
                            } else
                                newTag.add(tag)
                        }

                        if (index < capitulos.size) {
                            for (i in index + 1 until capitulos.size)
                                newTag.add("0${Utils.SEPARADOR_IMAGEM} Capítulo novo ${Utils.SEPARADOR_IMPORTACAO} ${capitulos[i]}")
                        }

                        item.tags = if (newTag.isNotEmpty()) newTag.joinToString(separator = "\n") else item.tags
                        item.isProcessado = true
                    }

                    if (!mCANCELAR) {
                        updateProgress(max, max)
                        updateMessage("Processamento de OCR finalizado.")
                    }
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar o OCR.", e)
                    Platform.runLater { AlertasPopup.erroModal("Erro ao processar o OCR", e.stackTrace.toString()) }
                }
                return true
            }

            override fun succeeded() {
                updateMessage("OCR processado com sucesso.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                habilita()
                tbViewProcessar.refresh()
                Notificacoes.notificacao(Notificacao.SUCESSO, "OCR Capítulos", "OCR processado com sucesso.")
            }

            override fun failed() {
                super.failed()
                updateMessage("Erro ao processar o OCR.")
                AlertasPopup.erroModal("Erro ao processar o OCR", super.getMessage())
                habilita()
                tbViewProcessar.refresh()
            }
        }
        controllerPai.rootProgress.progressProperty().bind(processaOCR.progressProperty())
        controllerPai.rootMessage.textProperty().bind(processaOCR.messageProperty())
        val t = Thread(processaOCR)
        t.isDaemon = true
        t.start()
    }

    private fun extraiComicInfo(arquivo: File): File? {
        var comicInfo : File? = null
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

    private fun insereComicInfo(arquivo: File, info: File) {
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

    private fun carregarItens() {
        val pasta = File(txtPastaProcessar.text)
        if (txtPastaProcessar.text.isNotEmpty() && pasta.exists()) {
            btnCarregar.isDisable = true
            controllerPai.setCursor(Cursor.WAIT)

            val processar: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        val lista = mutableListOf<Processar>()
                        val max = pasta.listFiles()?.size ?: 0
                        var i = 0
                        updateMessage("Carregando arquivos...")

                        val jaxb = JAXBContext.newInstance(ComicInfo::class.java)
                        for (arquivo in pasta.listFiles()!!) {
                            i++
                            updateProgress(i.toLong(), max.toLong())
                            updateMessage("Carregando item $i de $max.")

                            if (!Utils.isRar(arquivo.name))
                                continue

                            val info: File = extraiComicInfo(arquivo) ?: continue
                            val comic: ComicInfo = try {
                                val unmarshaller = jaxb.createUnmarshaller()
                                unmarshaller.unmarshal(info) as ComicInfo
                            } catch (e: Exception) {
                                mLOG.error(e.message, e)
                                continue
                            }

                            val bookMarks = comic.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toSet() ?: emptySet()
                            val processar = JFXButton("Processar")
                            val amazon = JFXButton("Amazon")
                            val salvar = JFXButton("Salvar")
                            val tags = bookMarks.joinToString(separator = "\n")
                            val item = Processar(arquivo.name, tags, arquivo, comic, processar, amazon, salvar)

                            processar.styleClass.add("background-White1")
                            processar.setOnAction { processarOcrItem(item) }
                            amazon.styleClass.add("background-White1")
                            amazon.setOnAction { openSiteAmazon(item)}
                            salvar.styleClass.add("background-White1")
                            salvar.setOnAction { salvarComicInfoItem(item) }

                            lista.add(item)
                        }

                        mObsListaProcessar = FXCollections.observableArrayList(lista)
                        Platform.runLater { tbViewProcessar.items = mObsListaProcessar }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao carregar itens para processamento de mangas.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "Mangas Processamento", "Erro ao carregar itens para processamento de mangas. " + e.message)
                        }
                    }
                    return null
                }
                override fun succeeded() {
                    updateMessage("Arquivos carregados com sucesso.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    controllerPai.setCursor(null)
                    btnCarregar.isDisable = false
                }
            }
            controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
            Thread(processar).start()
        } else {
            AlertasPopup.alertaModal("Alerta", "Necessário informar uma pasta para processar.")
            txtPastaProcessar.requestFocus()
        }
    }

    private fun salvarComicInfoItem(item: Processar) {
        try {
            val info = File(item.file!!.parent, "ComicInfo.xml")
            if (info.exists())
                info.delete()

            item.comicInfo?.pages?.forEach { it.bookmark = null }

            var sumario = "*Chapter Titles Manual*\n"
            val tags = item.tags.split("\n")
            for (tag in tags) {
                val imagem = tag.substringBefore(Utils.SEPARADOR_IMAGEM)
                var capitulo = tag.substringAfter(Utils.SEPARADOR_IMAGEM).trim()

                if (capitulo.endsWith(Utils.SEPARADOR_IMPORTACAO))
                    capitulo = capitulo.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()

                if (capitulo.isEmpty())
                    continue

                capitulo.lowercase().let {
                    if (it.contains("第") || it.contains("chapter") || it.contains("capítulo")) {
                        val numero = if (it.contains("第"))
                            Utils.fromNumberJapanese(it.substringBefore("-").replace("第", "Chapter ").replace("話", "").trim())
                        else
                            it.substringBefore("-").replace("capítulo", "Chapter").replace("chapter", "Chapter").trim()
                        sumario += numero + ": " + capitulo.substringAfter("-").trim() + "\n"
                    }
                }

                val page = item.comicInfo?.pages?.firstOrNull { it.image.toString() == imagem } ?: continue
                page.bookmark = capitulo
            }

            item.comicInfo?.run {
                summary = if (summary.isNullOrEmpty())
                    sumario
                else {
                    if (summary!!.lowercase().contains("*chapter titles manual*"))
                        summary!!.substring(0, summary!!.lowercase().indexOf("*chapter titles manual*")).trim() + "\n\n" + sumario
                    else
                        summary!! + "\n\n" + sumario
                }
            }

            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(info)
            marshaller.marshal(item.comicInfo, out)
            out.close()
            insereComicInfo(item.file!!, info)
            mObsListaProcessar.remove(item)
        } catch (e: Exception) {
            mLOG.error(e.message, e)
            AlertasPopup.alertaModal("Erro", "Erro ao salvar o ComicInfo no arquivo ComicInfo.xml. " + e.message)
        }
    }

    private fun salvarItens(startIndex: Int = 0, endIndex : Int = 0) {
        controllerPai.setCursor(Cursor.WAIT)
        desabilita()
        val processar: Task<Void> = object : Task<Void>() {
            override fun call(): Void? {
                try {
                    updateMessage("Salvando ComicInfo...")
                    val list = mObsListaProcessar.toList()
                    for (i in startIndex until endIndex) {
                        updateProgress(i.toLong(), endIndex.toLong())
                        updateMessage("Salvando ComicInfo $i de $endIndex.")
                        salvarComicInfoItem(list[i])
                    }
                } catch (e: Exception) {
                    mLOG.info("Erro ao salvar o ComicInfo.", e)
                    Platform.runLater {
                        Notificacoes.notificacao(Notificacao.ERRO, "Salvar ComicInfo", "Erro ao salvar o ComicInfo. " + e.message)
                    }
                }
                return null
            }
            override fun succeeded() {
                updateMessage("ComicInfo salvo com sucesso.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                controllerPai.setCursor(null)
                habilita()
            }
        }
        controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
        controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
        Thread(processar).start()
    }

    private val mPASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private fun extraiSumario(arquivo: File): File? {
        var sumario : File? = null
        var proc: Process? = null

        for (arquivos in mPASTA_TEMPORARIA.listFiles()!!) {
            if (arquivos.name.contains("zSumário", ignoreCase = true))
                arquivos.delete()
        }

        val comando = "rar e -ma4 -y " + '"' + arquivo.path + '"' + " " + '"' + mPASTA_TEMPORARIA.path + '"' + " " + '"' + "*zSumário.*" + '"'
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
                for (arquivos in mPASTA_TEMPORARIA.listFiles()!!) {
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

    private fun processarOcrItem(item: Processar) {
        val sumario = extraiSumario(item.file!!) ?: return
        val capitulos = Ocr.processaGemini(sumario, Utils.SEPARADOR_CAPITULO).split("\n")
        val newTag = mutableSetOf<String>()
        val tags = item.comicInfo!!.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toList() ?: emptyList()

        var index = -1
        for (tag in tags) {
            if (tag.contains("capítulo", ignoreCase = true) || tag.contains("第", ignoreCase = true)) {
                index++
                val capitulo = if (index < capitulos.size) capitulos[index] else ""
                newTag.add("$tag ${Utils.SEPARADOR_IMPORTACAO} $capitulo")
            } else
                newTag.add(tag)
        }

        if (index < capitulos.size) {
            for (i in index + 1 until capitulos.size)
                newTag.add("-1${Utils.SEPARADOR_IMAGEM} Capítulo novo ${Utils.SEPARADOR_IMPORTACAO} ${capitulos[i]}")
        }

        item.tags = if (newTag.isNotEmpty()) newTag.joinToString(separator = "\n") else item.tags
        item.isProcessado = true
        tbViewProcessar.refresh()
    }

    private fun gerarTagItem(item: Processar, language: Linguagem, isAjustar : Boolean = false) {
        val tags = item.comicInfo!!.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { p ->
            p.image.toString() + Utils.SEPARADOR_IMAGEM + p.bookmark!!.substringBefore("-", p.bookmark!!).trim()
                .let { b ->
                    val mark = b.lowercase()
                    if (mark.contains("第") || mark.contains("capítulo") || mark.contains("capitulo") || mark.contains("chapter")) {
                    val capitulo = Utils.getNumber(if (mark.contains("第")) Utils.fromNumberJapanese(b) else b) ?: 0.0
                    when (language) {
                        Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(capitulo))}話"
                        Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(capitulo)}"
                        else -> "Chapter ${mDecimal.format(capitulo)}"
                    } + if (isAjustar) " - " + p.bookmark!!.substringAfterLast("-").trim() else ""
                } else b }
        }?.toList() ?: emptyList()
        item.tags = tags.joinToString(separator = "\n")
    }

    private fun normalizarTagItem(item: Processar, language: Linguagem) {
        val linhas = item.tags.split("\n")
        val tags = mutableListOf<String>()
        for (linha in linhas) {
            val ln = linha.lowercase()
            if (ln.contains("capítulo Novo", ignoreCase = true)) {
                if (linha.contains(Utils.SEPARADOR_CAPITULO))
                    tags.add(linha.substringBefore(Utils.SEPARADOR_CAPITULO) + Utils.SEPARADOR_CAPITULO + Utils.normaliza(linha.substringAfter(Utils.SEPARADOR_CAPITULO)))
                else
                    tags.add(linha)
            } else if (ln.contains("第") || ln.contains("capítulo")  || ln.contains("capitulo") || ln.contains("chapter")) {
                val imagem = linha.substringBefore(Utils.SEPARADOR_IMAGEM)
                var capitulo = linha.substringAfter(Utils.SEPARADOR_IMAGEM).substringBefore("-").trim()
                val numero = Utils.getNumber(if (capitulo.contains("第")) Utils.fromNumberJapanese(capitulo) else capitulo) ?: 0.0
                capitulo = when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(numero))}話"
                    Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(numero)}"
                    else -> "Chapter ${mDecimal.format(numero)}"
                }
                val titulo = if (linhas.contains(Utils.SEPARADOR_IMPORTACAO))
                    " ${Utils.SEPARADOR_IMPORTACAO} " + linha.substringAfter(Utils.SEPARADOR_IMPORTACAO).substringBefore(Utils.SEPARADOR_CAPITULO).trim() + Utils.SEPARADOR_CAPITULO + Utils.normaliza(linha.substringAfterLast(Utils.SEPARADOR_CAPITULO).trim())
                else if (linha.contains("-"))
                    " - " + Utils.normaliza(linha.substringAfter("-").trim())
                else
                    ""
                tags.add(imagem + Utils.SEPARADOR_IMAGEM + capitulo + titulo)
            } else
                tags.add(linha)
        }
        item.tags = tags.joinToString(separator = "\n")
    }

    private fun openSiteAmazon(item: Processar) {
        val callback: Callback<ComicInfo, Boolean> = Callback<ComicInfo, Boolean> { param ->
            item.comicInfo = param
            tbViewProcessar.refresh()
            null
        }
        PopupAmazon.abreTelaAmazon(controllerPai.rootStack, controllerPai.rootTab, callback, item.comicInfo, cbLinguagem.value)
    }

    private fun editaColunas() {
        clTitulo.cellFactory = TextAreaTableCell.forTableColumn()
        clSerie.cellFactory = TextAreaTableCell.forTableColumn()

        clProcessado.setCellValueFactory { param ->
            val item = param.value

            val booleanProp = SimpleBooleanProperty(item.isProcessado)
            booleanProp.addListener { _, _, newValue ->
                item.isProcessado = newValue
                tbViewProcessar.refresh()
            }
            return@setCellValueFactory booleanProp
        }
        clProcessado.setCellFactory {
            val cell : CheckBoxTableCellCustom<Processar, Boolean> = CheckBoxTableCellCustom()
            cell.alignment = Pos.CENTER
            cell
        }

        clTags.cellFactory = TextAreaTableCell.forTableColumn(Tooltip("Com o shift e alt pressionados poderá ser executado algumas funções no texto apresentado, são eles:\n" +
                "Shift + Alt + Enter: Aplicação da tag gerada aos capítulos.\nShift + Alt + Delete: Apaga a linha selecionada.\nShift + Alt + Left: Aplicar as tags da linha selecionada.\n" +
                "Shift + Alt + Acima/Baixo: Move a tag da linha selecionada para cima ou baixo, movimentando outras tags subjacentes."))
        clTags.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            e.tableView.items[e.tablePosition.row].tags = e.newValue
        }

        val menu = ContextMenu()
        val salvar = MenuItem("Salvar ComicInfo")
        salvar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                salvarComicInfoItem(tbViewProcessar.selectionModel.selectedItem)
        }
        val salvarAnteriores = MenuItem("Salvar ComicInfo do inicio até o item atual")
        salvarAnteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                salvarItens(startIndex = 0, endIndex = index + 1)
            }
        }
        val salvarPosteriores = MenuItem("Salvar ComicInfo do atual até o item final")
        salvarPosteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                salvarItens(startIndex = index, endIndex = mObsListaProcessar.size)
            }
        }

        val processar = MenuItem("Processar OCR")
        processar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                processarOcrItem(tbViewProcessar.selectionModel.selectedItem)
        }
        val tags = MenuItem("Gerar Tags")
        tags.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem, language)
                tbViewProcessar.refresh()
            }
        }
        val tagsAnteriores = MenuItem("Gerar Tags até o item atual")
        tagsAnteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                controllerPai.setCursor(Cursor.WAIT)
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                for (i in 0 until index + 1)
                    gerarTagItem(mObsListaProcessar[i], language)
                tbViewProcessar.refresh()
                controllerPai.setCursor(null)
            }
        }
        val tagsAjustar = MenuItem("Ajustar Tags")
        tagsAjustar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem, language, isAjustar = true)
                tbViewProcessar.refresh()
            }
        }
        val tagsNormalizar = MenuItem("Normalizar Tags")
        tagsNormalizar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                normalizarTagItem(tbViewProcessar.selectionModel.selectedItem, language)
                tbViewProcessar.refresh()
            }
        }
        val remover = MenuItem("Remover registro")
        remover.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                if (AlertasPopup.confirmacaoModal("Aviso", "Deseja remover o registro?")) {
                    mObsListaProcessar.remove(tbViewProcessar.selectionModel.selectedItem)
                    tbViewProcessar.refresh()
                }
        }
        val removerAnteriores = MenuItem("Remover registros anteriores")
        removerAnteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                if (AlertasPopup.confirmacaoModal("Aviso", "Deseja remover os registros anteriores?")) {
                    controllerPai.setCursor(Cursor.WAIT)
                    val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                    if (index > 0) {
                        mObsListaProcessar.remove(0, index)
                        tbViewProcessar.refresh()
                    }
                    controllerPai.setCursor(null)
                }
        }

        val removerPosteriores = MenuItem("Remover próximos registros")
        removerPosteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                if (AlertasPopup.confirmacaoModal("Aviso", "Deseja remover os registros posteriores?")) {
                    controllerPai.setCursor(Cursor.WAIT)
                    val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem) + 1
                    if (index > 0 && mObsListaProcessar.size > index) {
                        mObsListaProcessar.remove(index, mObsListaProcessar.size)
                        tbViewProcessar.refresh()
                    }
                    controllerPai.setCursor(null)
                }
        }
        menu.items.add(salvar)
        menu.items.add(salvarAnteriores)
        menu.items.add(salvarPosteriores)
        menu.items.add(processar)
        menu.items.add(tags)
        menu.items.add(tagsAnteriores)
        menu.items.add(tagsAjustar)
        menu.items.add(tagsNormalizar)
        menu.items.add(remover)
        menu.items.add(removerAnteriores)
        menu.items.add(removerPosteriores)

        tbViewProcessar.contextMenu = menu
        tbViewProcessar.setOnKeyPressed { event ->
            if (event.code == KeyCode.DELETE && tbViewProcessar.selectionModel.selectedItem != null)
                if (AlertasPopup.confirmacaoModal("Aviso", "Deseja remover o registro?")) {
                    mObsListaProcessar.remove(tbViewProcessar.selectionModel.selectedItem)
                    tbViewProcessar.refresh()
                }
        }

        TextAreaTableCell.setOnKeyPress { pair ->
            val textArea = pair.key
            val key = pair.value
            if (key.isShiftDown && key.isAltDown) {
                when (key.code) {
                    KeyCode.ENTER -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains("\n") || !textArea.text.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val txt = textArea.text ?: ""
                        val linhas = mutableListOf<String>()
                        val separador = Utils.SEPARADOR_CAPITULO
                        for (linha in txt.split("\n"))
                            linhas.add(if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) linha.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim() + " - " + linha.substringAfterLast(separador) else linha)

                        textArea.replaceText(0, textArea.length, linhas.joinToString(separator = "\n"))
                    }
                    KeyCode.DELETE -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains("\n"))
                            return@setOnKeyPress true

                        val lastCaretPos = textArea.caretPosition

                        val txt = textArea.text ?: ""
                        val scroll = textArea.scrollTopProperty().value

                        var before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                        before = before.substringBeforeLast(line)

                        textArea.replaceText(0, textArea.length, (before + last).trim())
                        textArea.positionCaret(before.length)
                        textArea.scrollTop = scroll
                        key.consume()
                    }
                    KeyCode.LEFT -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains("\n") || !textArea.text.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val lastCaretPos = textArea.caretPosition

                        val txt = textArea.text ?: ""
                        val scroll = textArea.scrollTopProperty().value

                        var before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                        before = before.substringBeforeLast(line)

                        if (!line.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val separador = Utils.SEPARADOR_CAPITULO
                        val newLine = line.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim() + " - " + line.substringAfterLast(separador)
                        textArea.replaceText(0, textArea.length, before + newLine + last)
                        val caret = before.length + newLine.lastIndexOf(" - ")
                        textArea.positionCaret(caret)
                        textArea.scrollTop = scroll
                    }
                    KeyCode.UP,
                    KeyCode.DOWN -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains("\n") || !textArea.text.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val lastCaretPos = textArea.caretPosition

                        val txt = textArea.text ?: ""
                        val linhas = txt.split("\n").toMutableList()
                        val scroll = textArea.scrollTopProperty().value

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")

                        if (!line.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        var index = -1
                        for ((idx, item) in linhas.withIndex()) {
                            if (item == line) {
                                index = idx
                                break
                            }
                        }

                        if (key.code == KeyCode.UP) {
                            if (index > 0) {
                                if (index >= linhas.size)
                                    index = linhas.size - 2

                                var spaco = -1
                                for (i in (index - 1) downTo 2) {
                                    if (!linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = 1
                                if (spaco > 0)
                                    inicio = spaco + 1

                                for (i in inicio until index + 1) {
                                    linhas[i-1] = linhas[i-1] + if (linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + linhas[i].substringAfter(Utils.SEPARADOR_IMPORTACAO) else ""
                                    linhas[i] = linhas[i].substringBefore(Utils.SEPARADOR_IMPORTACAO)
                                }
                            }
                        } else if (key.code == KeyCode.DOWN) {
                            if (index < linhas.size - 1) {
                                var spaco = -1
                                for (i in index + 1 until linhas.size) {
                                    if (!linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = linhas.size - 2
                                if (spaco > 0 && spaco < linhas.size -1)
                                    inicio = spaco - 1

                                for (i in inicio downTo index) {
                                    linhas[i+1] = linhas[i+1] + if (linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + linhas[i].substringAfter(Utils.SEPARADOR_IMPORTACAO) else ""
                                    linhas[i] = linhas[i].substringBefore(Utils.SEPARADOR_IMPORTACAO)
                                }
                            }
                        }

                        for (i in linhas.size - 1 downTo 0) {
                            val linha = linhas[i]
                            if (linha.contains("-1${Utils.SEPARADOR_IMAGEM}", ignoreCase = true) && !linha.contains(Utils.SEPARADOR_IMPORTACAO, ignoreCase = true))
                                linhas.removeAt(i)
                        }

                        textArea.replaceText(0, textArea.length, linhas.joinToString(separator = "\n"))

                        val caret = if (key.code == KeyCode.UP) {
                            if (textArea.text.length > lastCaretPos)
                                textArea.text.substring(0, lastCaretPos).lastIndexOf(Utils.SEPARADOR_IMPORTACAO)
                            else
                                textArea.text.lastIndexOf(Utils.SEPARADOR_IMPORTACAO)
                        } else
                            textArea.text.indexOf(Utils.SEPARADOR_IMPORTACAO, lastCaretPos)

                        textArea.positionCaret(caret)
                        textArea.scrollTop = scroll
                    }
                    else -> {}
                }
            }

            return@setOnKeyPress true
        }

    }

    private fun linkaCelulas() {
        clProcessado.cellValueFactory = PropertyValueFactory("isProcessado")
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clSerie.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.series)
            else
                SimpleStringProperty("")
        }

        clTitulo.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.title)
            else
                SimpleStringProperty("")
        }

        clEditora.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.publisher)
            else
                SimpleStringProperty("")
        }

        clPublicacao.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null && item.comicInfo!!.year != null)
                SimpleStringProperty("${item.comicInfo!!.day}/${item.comicInfo!!.month}/${item.comicInfo!!.year}")
            else
                SimpleStringProperty("")
        }

        clResumo.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null && item.comicInfo!!.summary != null)
                SimpleStringProperty(item.comicInfo!!.summary)
            else
                SimpleStringProperty("")
        }

        clTags.cellValueFactory = PropertyValueFactory("tags")
        clProcessarOcr.cellValueFactory = PropertyValueFactory("processar")
        clProcessarAmazon.cellValueFactory = PropertyValueFactory("amazon")
        clSalvarComicInfo.cellValueFactory = PropertyValueFactory("salvar")

        editaColunas()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        cbLinguagem.items.addAll(Linguagem.JAPANESE, Linguagem.ENGLISH, Linguagem.PORTUGUESE)
        cbLinguagem.selectionModel.selectFirst()
        linkaCelulas()
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/AbaComicInfo.fxml")
    }

}