package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.CheckBoxTableCellCustom
import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.notification.ConfirmaModal
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
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
import java.io.File
import java.io.FileOutputStream
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
    private lateinit var btnProcessar: JFXButton

    @FXML
    private lateinit var ckbTodosProcessado: JFXCheckBox

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

    internal var mRarService = WinrarServices()
    internal var mOcrService = OcrServices()

    private lateinit var controller: TelaInicialController
    var controllerPai: TelaInicialController
        get() = controller
        set(controller) {
            this.controller = controller
        }

    private var mObsListaProcessar: ObservableList<Processar> = FXCollections.observableArrayList()
    private val mDecimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
    private val mPASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")

    @FXML
    private fun onBtnCapitulos() {
        val selected = tbViewProcessar.selectionModel.selectedItems
        val listToProcess = if (selected.size > 1) selected.toList() else mObsListaProcessar

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
        PopupCapitulosController.abreTelaCapitulos(controllerPai.rootStack, controllerPai.rootTab, callback, cbLinguagem.value, listToProcess)
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
                desabilita(btnOcrProcessar)
                processaOCR()
            } else
                mCANCELAR = true
        }
    }

    @FXML
    private fun onBtnProcessar() {
        if (btnProcessar.accessibleTextProperty().value.equals("PROCESSA", ignoreCase = true)) {
            if (txtPastaProcessar.text.isEmpty()) {
                AlertasModal.alerta("Alerta", "Necessário informar uma pasta para processar.")
                txtPastaProcessar.requestFocus()
                return
            }

            btnProcessar.accessibleTextProperty().set("CANCELA")
            btnProcessar.text = "Cancelar"
            controllerPai.setCursor(Cursor.WAIT)
            mObsListaProcessar.clear()
            desabilita(btnProcessar)

            val marcaCapitulo = when (cbLinguagem.value) {
                Linguagem.PORTUGUESE -> "Capítulo"
                Linguagem.ENGLISH -> "Chapter"
                Linguagem.JAPANESE -> "第%s話"
                else -> ""
            }

            val processa: Task<Boolean> = object : Task<Boolean>() {
                override fun call(): Boolean {
                    updateMessage("Processando ComicInfo...")
                    com.fenix.ordenararquivos.process.ComicInfo.processa(cbLinguagem.value, txtPastaProcessar.text, marcaCapitulo) { param ->
                        updateProgress(param[0], param[1])
                        updateMessage("Processando item ${param[0]} de ${param[1]}")
                        null
                    }
                    return true
                }

                override fun succeeded() {
                    updateMessage("Processamento finalizado com sucesso.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita()
                    carregarItens()
                    Notificacoes.notificacao(Notificacao.SUCESSO, "Processamento ComicInfo", "ComicInfo processado com sucesso.")
                }

                override fun failed() {
                    super.failed()
                    mLOG.error("Erro na Task de processamento de ComicInfo", exception)
                    updateMessage("Erro ao processar o ComicInfo.")
                    AlertasModal.erro("Erro ao processar o ComicInfo", exception?.message ?: "Erro desconhecido")
                    habilita()
                }
            }

            controllerPai.rootProgress.progressProperty().bind(processa.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processa.messageProperty())
            val t = Thread(processa)
            t.isDaemon = true
            t.start()

        } else {
            com.fenix.ordenararquivos.process.ComicInfo.cancelar()
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
                    linhas.add(
                        if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) linha.substringBefore(Utils.SEPARADOR_IMPORTACAO)
                            .trim() + " - " + linha.substringAfterLast(separador) else linha
                    )

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

    @FXML
    private fun onBtnTodosProcessado() {
        mObsListaProcessar.forEach { it.isProcessado = ckbTodosProcessado.isSelected }
        tbViewProcessar.refresh()
    }

    private fun atualizaCheckTodosProcessado() {
        ckbTodosProcessado.isSelected = mObsListaProcessar.isNotEmpty() && mObsListaProcessar.all { it.isProcessado }
    }

    private fun desabilita(botaoAtivo: JFXButton? = null) {
        txtPastaProcessar.isDisable = true
        btnPesquisarPastaProcessar.isDisable = true
        cbLinguagem.isDisable = true
        btnCarregar.isDisable = true
        btnTagsProcessar.isDisable = true
        btnTagsNormaliza.isDisable = true
        btnTagsAplicar.isDisable = true
        btnSalvarTodos.isDisable = true
        btnCapitulos.isDisable = true
        tbViewProcessar.isDisable = true

        btnOcrProcessar.isDisable = botaoAtivo != btnOcrProcessar
        btnProcessar.isDisable = botaoAtivo != btnProcessar
    }

    private fun habilita() {
        txtPastaProcessar.isDisable = false
        btnPesquisarPastaProcessar.isDisable = false
        cbLinguagem.isDisable = false
        btnCarregar.isDisable = false
        btnTagsProcessar.isDisable = false
        btnTagsNormaliza.isDisable = false
        btnTagsAplicar.isDisable = false
        btnSalvarTodos.isDisable = false
        btnCapitulos.isDisable = false
        tbViewProcessar.isDisable = false
        btnOcrProcessar.isDisable = false
        btnProcessar.isDisable = false

        btnOcrProcessar.accessibleTextProperty().set("PROCESSA")
        btnOcrProcessar.text = "OCR proximos 10"

        btnProcessar.accessibleTextProperty().set("PROCESSA")
        btnProcessar.text = "Processar ComicInfo"
        if (::controller.isInitialized) controllerPai.setCursor(null)
    }

    private var mCANCELAR = false
    private fun processaOCR() {
        val separador = Utils.SEPARADOR_CAPITULO
        val listaProcessar = mObsListaProcessar.toList()
        val processaOCR: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                try {
                    mCANCELAR = false
                    val max = 10L
                    var i = 0
                    updateMessage("Processando o OCR...")

                    for (item in listaProcessar) {
                        if (item.isProcessado)
                            continue
                        i++
                        if (mCANCELAR || i > max)
                            break

                        updateProgress(i.toLong(), max)
                        updateMessage("Processando item " + i + " de " + max + ". Processando OCR - " + item.arquivo)

                        val sumario = mRarService.extraiSumario(item.file!!, mPASTA_TEMPORARIA)
                        if (sumario == null) {
                            i--
                            continue
                        }

                        val capitulos = mOcrService.processOcr(sumario, Utils.SEPARADOR_PAGINA, separador).split("\n")
                        val newTag = mutableSetOf<String>()
                        val tags = item.comicInfo?.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toList()
                            ?: emptyList()

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
                            for (idx in index + 1 until capitulos.size)
                                newTag.add("0${Utils.SEPARADOR_IMAGEM} Capítulo novo ${Utils.SEPARADOR_IMPORTACAO} ${capitulos[idx]}")
                        }

                        val finalTags = if (newTag.isNotEmpty()) newTag.joinToString(separator = "\n") else item.tags
                        Platform.runLater {
                            item.tags = finalTags
                            item.isProcessado = true
                        }
                    }

                    if (!mCANCELAR) {
                        updateProgress(max, max)
                        updateMessage("Processamento de OCR finalizado.")
                    }
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar o OCR.", e)
                    Platform.runLater { AlertasModal.erro("Erro ao processar o OCR", e.stackTrace.toString()) }
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
                AlertasModal.erro("Erro ao processar o OCR", super.getMessage() ?: "Erro desconhecido")
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
                            if (!Utils.isRar(arquivo.name))
                                continue

                            i++
                            updateProgress(i.toLong(), max.toLong())
                            updateMessage("Carregando item $i de $max.")

                            val info: File? = mRarService.extraiComicInfo(arquivo)
                            val comic: ComicInfo = if (info != null && info.exists()) {
                                try {
                                    val unmarshaller = jaxb.createUnmarshaller()
                                    unmarshaller.unmarshal(info) as ComicInfo
                                } catch (e: Exception) {
                                    mLOG.error(e.message, e)
                                    ComicInfo()
                                }
                            } else {
                                ComicInfo()
                            }

                            val bookMarks =
                                comic.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toSet() ?: emptySet()
                            val processar = JFXButton("Processar").apply { id = "btnProcessar_${i}" }
                            val amazon = JFXButton("Amazon").apply { id = "btnAmazon_${i}" }
                            val salvar = JFXButton("Salvar").apply { id = "btnSalvar_${i}" }
                            val tags = bookMarks.joinToString(separator = "\n")
                            val item = Processar(arquivo.name, tags, arquivo, comic, processar, amazon, salvar)

                            processar.styleClass.add("background-White1")
                            processar.setOnAction { processarOcrItem(item) }
                            amazon.styleClass.add("background-White1")
                            amazon.setOnAction { openSiteAmazon(item) }
                            salvar.styleClass.add("background-White1")
                            salvar.setOnAction { salvarComicInfoItem(item) }

                            lista.add(item)
                        }

                        Platform.runLater {
                            mObsListaProcessar = FXCollections.observableArrayList(lista)
                            tbViewProcessar.items = mObsListaProcessar
                            atualizaCheckTodosProcessado()

                            val lastLang = lista.lastOrNull()?.comicInfo?.languageISO
                            if (!lastLang.isNullOrEmpty()) {
                                val lingua = Linguagem.getEnum(lastLang)
                                if (lingua != null) cbLinguagem.selectionModel.select(lingua)
                            }
                        }
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
            AlertasModal.alerta("Alerta", "Necessário informar uma pasta para processar.")
            txtPastaProcessar.requestFocus()
        }
    }

    private fun salvarComicInfoItem(item: Processar) {
        try {
            val info = File(item.file!!.parent, "ComicInfo.xml")
            if (info.exists())
                info.delete()

            item.comicInfo?.pages?.forEach { it.bookmark = null }

            val tags = item.tags.split("\n")
            var temTitulo = false
            val sbSumario = StringBuilder()

            for (tag in tags) {
                val imagem = tag.substringBefore(Utils.SEPARADOR_IMAGEM)
                var capitulo = tag.substringAfter(Utils.SEPARADOR_IMAGEM).trim()

                if (capitulo.endsWith(Utils.SEPARADOR_IMPORTACAO))
                    capitulo = capitulo.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()

                if (capitulo.isEmpty())
                    continue

                capitulo.lowercase().let {
                    if (it.contains("第") || it.contains("chapter") || it.contains("capítulo")) {
                        val hasDelimiter = capitulo.contains("-") || capitulo.contains("—")
                        val delimiter = if (capitulo.contains("—")) "—" else "-"

                        val chapterPart = if (hasDelimiter) capitulo.substringBefore(delimiter).trim() else capitulo.trim()
                        val numero = if (chapterPart.lowercase().contains("第"))
                            Utils.fromNumberJapanese(chapterPart.lowercase().replace("第", "Chapter ").replace("話", "").trim())
                        else
                            chapterPart.lowercase().replace("capítulo", "Chapter").replace("chapter", "Chapter").trim()

                        val tituloCapitulo = if (hasDelimiter) capitulo.substringAfter(delimiter).trim() else ""

                        if (tituloCapitulo.isNotEmpty() && !tituloCapitulo.equals(chapterPart, ignoreCase = true)) {
                            temTitulo = true
                            sbSumario.append(Utils.normaliza(numero)).append(": ").append(tituloCapitulo).append("\n")
                        }
                    }
                }

                val page = item.comicInfo?.pages?.firstOrNull { it.image.toString() == imagem } ?: continue
                page.bookmark = capitulo
            }

            if (temTitulo) {
                val sumarioFinal = "*Chapter Titles Manual*\n" + sbSumario.toString()
                item.comicInfo?.run {
                    summary = if (summary.isNullOrEmpty())
                        sumarioFinal
                    else {
                        if (summary!!.lowercase().contains("*chapter titles manual*"))
                            summary!!.substring(0, summary!!.lowercase().indexOf("*chapter titles manual*")).trim() + "\n\n" + sumarioFinal
                        else
                            summary!! + "\n\n" + sumarioFinal
                    }
                }
            }

            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(info)
            marshaller.marshal(item.comicInfo, out)
            out.close()
            mRarService.insereComicInfo(item.file!!, info)
            mObsListaProcessar.remove(item)
        } catch (e: Exception) {
            mLOG.error(e.message, e)
            AlertasModal.erro("Erro", "Erro ao salvar o ComicInfo no arquivo ComicInfo.xml. " + e.message)
        }
    }

    private fun salvarItens(startIndex: Int = 0, endIndex: Int = 0) {
        controllerPai.setCursor(Cursor.WAIT)
        desabilita(btnSalvarTodos)
        val list = mObsListaProcessar.toList()
        val processar: Task<Void> = object : Task<Void>() {
            override fun call(): Void? {
                try {
                    updateMessage("Salvando ComicInfo...")
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

    private fun recarregarComicInfoItem(item: Processar) {
        val jaxb = JAXBContext.newInstance(ComicInfo::class.java)
        val info: File? = mRarService.extraiComicInfo(item.file!!)
        val comic: ComicInfo = if (info != null && info.exists()) {
            try {
                val unmarshaller = jaxb.createUnmarshaller()
                unmarshaller.unmarshal(info) as ComicInfo
            } catch (e: Exception) {
                mLOG.error(e.message, e)
                ComicInfo()
            }
        } else {
            ComicInfo()
        }
        item.comicInfo = comic
        val bookMarks = comic.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toSet() ?: emptySet()
        item.tags = bookMarks.joinToString(separator = "\n")
        item.arquivo = item.file!!.name
        tbViewProcessar.refresh()
    }

    private fun processarOcrItem(item: Processar) {
        val sumario = mRarService.extraiSumario(item.file!!, mPASTA_TEMPORARIA) ?: return
        val capitulos = mOcrService.processOcr(sumario, Utils.SEPARADOR_PAGINA, Utils.SEPARADOR_CAPITULO).split("\n")
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

    private fun gerarTagItem(item: Processar, language: Linguagem, isAjustar: Boolean = false) {
        val series = item.comicInfo?.series ?: ""
        val title = item.comicInfo?.title ?: ""
        val header = if (series.isNotEmpty()) "$series - $title" else title

        val tags = item.comicInfo?.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { p ->
            val b = p.bookmark!!.split("-", "—")[0].trim()
            val mark = b.lowercase()
            val prefix = if (mark.contains("第") || mark.contains("capítulo") || mark.contains("capitulo") || mark.contains("chapter")) {
                val capitulo = Utils.getNumber(if (mark.contains("第")) Utils.fromNumberJapanese(b) else b) ?: 0.0
                when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(capitulo))}話"
                    Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(capitulo)}"
                    else -> "Chapter ${mDecimal.format(capitulo)}"
                } + if (isAjustar) " — " + p.bookmark!!.substringAfterLast("-").substringAfterLast("—").trim() else ""
            } else b

            p.image.toString() + Utils.SEPARADOR_IMAGEM + prefix
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
            } else if (ln.contains("第") || ln.contains("capítulo") || ln.contains("capitulo") || ln.contains("chapter")) {
                val imagem = linha.substringBefore(Utils.SEPARADOR_IMAGEM)
                var capitulo = linha.substringAfter(Utils.SEPARADOR_IMAGEM).split("-", "—")[0].trim()
                val numero = Utils.getNumber(if (capitulo.contains("第")) Utils.fromNumberJapanese(capitulo) else capitulo) ?: 0.0
                capitulo = when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(numero))}話"
                    Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(numero)}"
                    else -> "Chapter ${mDecimal.format(numero)}"
                }
                val titulo = if (linha.contains(Utils.SEPARADOR_IMPORTACAO))
                    " ${Utils.SEPARADOR_IMPORTACAO} " + linha.substringAfter(Utils.SEPARADOR_IMPORTACAO).substringBefore(Utils.SEPARADOR_CAPITULO)
                        .trim() + Utils.SEPARADOR_CAPITULO + Utils.normaliza(linha.substringAfterLast(Utils.SEPARADOR_CAPITULO).trim())
                else if (linha.contains("-") || linha.contains("—"))
                    " — " + Utils.normaliza(if (linha.contains("—")) linha.substringAfter("—").trim() else linha.substringAfter("-").trim())
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
        PopupAmazonController.abreTelaAmazon(controllerPai.rootStack, controllerPai.rootTab, callback, item.comicInfo, cbLinguagem.value)
    }

    private fun editaColunas() {
        clTitulo.cellFactory = TextAreaTableCell.forTableColumn()
        clSerie.cellFactory = TextAreaTableCell.forTableColumn()
        clResumo.cellFactory = TextAreaTableCell.forTableColumn()
        clResumo.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.comicInfo != null) {
                item.comicInfo!!.summary = e.newValue
            }
        }

        clProcessado.setCellValueFactory { param ->
            val item = param.value

            val booleanProp = SimpleBooleanProperty(item.isProcessado)
            booleanProp.addListener { _, _, newValue ->
                item.isProcessado = newValue
                atualizaCheckTodosProcessado()
                tbViewProcessar.refresh()
            }
            return@setCellValueFactory booleanProp
        }
        clProcessado.setCellFactory {
            val cell: CheckBoxTableCellCustom<Processar, Boolean> = CheckBoxTableCellCustom()
            cell.alignment = Pos.CENTER
            cell
        }

        clTags.cellFactory = TextAreaTableCell.forTableColumn(
            Tooltip(
                "Com o shift e alt pressionados poderá ser executado algumas funções no texto apresentado, são eles:\n" +
                        "Shift + Alt + Enter: Aplicação da tag gerada aos capítulos.\nShift + Alt + Delete: Apaga a linha selecionada.\nShift + Alt + Left: Aplicar as tags da linha selecionada.\n" +
                        "Shift + Alt + Right: Aplicar capítulo importado da linha selecionada.\n" +
                        "Shift + Alt + Acima/Baixo: Move a tag da linha selecionada para cima ou baixo, movimentando outras tags subjacentes."
            )
        )
        clTags.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            e.tableView.items[e.tablePosition.row].tags = e.newValue
        }

        val menu = ContextMenu()

        // GRUPO TAGS
        val tagsAnteriores = MenuItem("Gerar Tags até o Item Atual")
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

        val tags = MenuItem("Gerar Tags (Ctrl + T)")
        tags.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem, language)
                tbViewProcessar.refresh()
            }
        }

        val tagsAjustar = MenuItem("Ajustar Tags (Ctrl + A)")
        tagsAjustar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem, language, isAjustar = true)
                tbViewProcessar.refresh()
            }
        }

        val tagsNormalizar = MenuItem("Normalizar Tags (Ctrl + N)")
        tagsNormalizar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                normalizarTagItem(tbViewProcessar.selectionModel.selectedItem, language)
                tbViewProcessar.refresh()
            }
        }

        val tagsDoArquivo = MenuItem("Gerar Tags do Arquivo")
        tagsDoArquivo.setOnAction {
            val item = tbViewProcessar.selectionModel.selectedItem
            if (item != null)
                gerarTagsDoArquivo(item)
        }

        val atualizarPaginaTag = MenuItem("Atualizar Página da Tag")
        atualizarPaginaTag.setOnAction {
            val item = tbViewProcessar.selectionModel.selectedItem
            if (item != null)
                atualizarPaginaTag(item)
        }

        // GRUPO OCR
        val processar = MenuItem("Processar OCR (Ctrl + O)")
        processar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                processarOcrItem(tbViewProcessar.selectionModel.selectedItem)
        }

        // GRUPO SALVAR
        val salvarAnteriores = MenuItem("Salvar ComicInfo do Inicio até o Atual")
        salvarAnteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                salvarItens(startIndex = 0, endIndex = index + 1)
            }
        }

        val salvar = MenuItem("Salvar ComicInfo (Ctrl + S)")
        salvar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                salvarComicInfoItem(tbViewProcessar.selectionModel.selectedItem)
        }

        val salvarPosteriores = MenuItem("Salvar ComicInfo do Atual até o Final")
        salvarPosteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                salvarItens(startIndex = index, endIndex = mObsListaProcessar.size)
            }
        }

        // GRUPO GERAL
        val recarregar = MenuItem("Recarregar ComicInfo do Item Atual")
        recarregar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                recarregarComicInfoItem(tbViewProcessar.selectionModel.selectedItem)
        }

        val remover = MenuItem("Remover registro (Del)")
        remover.setOnAction {
            val selected = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selected.isNotEmpty())
                if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                    mObsListaProcessar.removeAll(selected)
                    tbViewProcessar.refresh()
                }
        }

        menu.items.addAll(
            tagsAnteriores,
            tags,
            tagsAjustar,
            tagsNormalizar,
            atualizarPaginaTag,
            tagsDoArquivo,
            SeparatorMenuItem(),
            processar,
            SeparatorMenuItem(),
            salvarAnteriores,
            salvar,
            salvarPosteriores,
            SeparatorMenuItem(),
            recarregar,
            remover
        )

        tbViewProcessar.contextMenu = menu
        tbViewProcessar.setOnKeyPressed { event ->
            if (event.code == KeyCode.DELETE && tbViewProcessar.selectionModel.selectedItems.isNotEmpty())
                if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                    mObsListaProcessar.removeAll(tbViewProcessar.selectionModel.selectedItems.toList())
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
                            linhas.add(
                                if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) linha.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim() + " — " + linha.substringAfterLast(
                                    separador
                                ) else linha
                            )

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
                        val newLine = line.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim() + " — " + line.substringAfterLast(separador)
                        textArea.replaceText(0, textArea.length, before + newLine + last)
                        val caret = before.length + newLine.lastIndexOf(" — ")
                        textArea.positionCaret(caret)
                        textArea.scrollTop = scroll
                    }

                    KeyCode.RIGHT -> {
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
                        val importedPart = line.substringAfterLast(separador).trim()
                        val prefix = line.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()

                        val newLine = if (importedPart.lowercase().contains("capítulo") || importedPart.contains("—") || importedPart.contains(" - ")) {
                            line.substringBefore(Utils.SEPARADOR_IMAGEM) + Utils.SEPARADOR_IMAGEM + importedPart.replace(" - ", " — ")
                        } else {
                            val cleanPrefix = prefix.split("-", "—")[0].trim()
                            line.substringBefore(Utils.SEPARADOR_IMAGEM) + Utils.SEPARADOR_IMAGEM + cleanPrefix + " — " + importedPart
                        }

                        textArea.replaceText(0, textArea.length, before + newLine + last)
                        textArea.positionCaret(before.length + newLine.length)
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
                                    linhas[i - 1] =
                                        linhas[i - 1] + if (linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + linhas[i].substringAfter(Utils.SEPARADOR_IMPORTACAO) else ""
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
                                if (spaco > 0 && spaco < linhas.size - 1)
                                    inicio = spaco - 1

                                for (i in inicio downTo index) {
                                    linhas[i + 1] =
                                        linhas[i + 1] + if (linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + linhas[i].substringAfter(Utils.SEPARADOR_IMPORTACAO) else ""
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

    private fun configurarAtalhosGrid() {
        tbViewProcessar.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED) { e ->
            if (e.target is javafx.scene.control.TextInputControl) return@addEventFilter

            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            val selecionado = tbViewProcessar.selectionModel.selectedItem
            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE

            if (e.code.isLetterKey && !e.isControlDown && !e.isAltDown) {
                val letter = e.code.name.lowercase()
                val startIdx = if (selecionado != null) mObsListaProcessar.indexOf(selecionado) + 1 else 0

                var foundIdx = -1
                for (i in startIdx until mObsListaProcessar.size) {
                    if (mObsListaProcessar[i].arquivo.lowercase().startsWith(letter)) {
                        foundIdx = i
                        break
                    }
                }
                if (foundIdx == -1) {
                    for (i in 0 until startIdx) {
                        if (mObsListaProcessar[i].arquivo.lowercase().startsWith(letter)) {
                            foundIdx = i
                            break
                        }
                    }
                }
                if (foundIdx != -1) {
                    tbViewProcessar.selectionModel.clearAndSelect(foundIdx)
                    tbViewProcessar.scrollTo(foundIdx)
                }
                return@addEventFilter
            }

            if (selecionado == null && e.code != KeyCode.DELETE) return@addEventFilter

            when (e.code) {
                KeyCode.S -> {
                    if (e.isControlDown) {
                        salvarComicInfoItem(selecionado)
                        e.consume()
                    }
                }

                KeyCode.T -> {
                    if (e.isControlDown) {
                        gerarTagItem(selecionado, language)
                        tbViewProcessar.refresh()
                        e.consume()
                    }
                }

                KeyCode.O -> {
                    if (e.isControlDown) {
                        processarOcrItem(selecionado)
                        e.consume()
                    }
                }

                KeyCode.A -> {
                    if (e.isControlDown) {
                        gerarTagItem(selecionado, language, isAjustar = true)
                        tbViewProcessar.refresh()
                        e.consume()
                    }
                }

                KeyCode.N -> {
                    if (e.isControlDown) {
                        normalizarTagItem(selecionado, language)
                        tbViewProcessar.refresh()
                        e.consume()
                    }
                }

                KeyCode.DELETE -> {
                    if (selecionados.isNotEmpty()) {
                        if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                            mObsListaProcessar.removeAll(selecionados)
                            tbViewProcessar.refresh()
                        }
                        e.consume()
                    }
                }

                else -> {}
            }
        }
    }

    private fun gerarTagsDoArquivo(item: Processar) {
        val file = item.file ?: return
        val conteudo = mRarService.listarConteudo(file).sortedNaturally()
        if (conteudo.isEmpty()) return

        val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
        val newTags = mutableListOf<String>()
        val mapCapitulos = mutableMapOf<String, String>()

        // 1. Extrair títulos existentes das tags atuais
        item.tags.split("\n").forEach { linha ->
            if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) {
                val cap = linha.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim().substringBefore(Utils.SEPARADOR_CAPITULO).trim()
                val titulo = linha.substringAfterLast(Utils.SEPARADOR_CAPITULO).trim()
                if (cap.isNotEmpty() && titulo.isNotEmpty()) mapCapitulos[cap] = titulo
            } else if (linha.contains("-") || linha.contains("—")) {
                val cap = linha.substringAfter(Utils.SEPARADOR_IMAGEM).trim().split("-", "—")[0].trim()
                val titulo = if (linha.contains("—")) linha.substringAfter("—").trim() else linha.substringAfter("-").trim()
                if (cap.isNotEmpty() && titulo.isNotEmpty()) {
                    val num = Utils.getNumber(if (cap.contains("第")) Utils.fromNumberJapanese(cap) else cap)
                    if (num != null) mapCapitulos[mDecimal.format(num)] = titulo
                }
            }
        }

        // 2. Extrair títulos do resumo
        item.comicInfo?.summary?.let { summary ->
            if (summary.lowercase().contains("*chapter titles manual*") || summary.lowercase().contains("*chapter titles*")) {
                val lines = summary.split("\n")
                lines.forEach { line ->
                    if (line.contains(":")) {
                        val cap = line.substringBefore(":").replace(Regex("(?i)chapter|capítulo|第|話"), "").trim()
                        val titulo = line.substringAfter(":").trim()
                        val num = Utils.getNumber(if (cap.contains("第")) Utils.fromNumberJapanese(cap) else cap)
                        if (num != null && titulo.isNotEmpty()) mapCapitulos[mDecimal.format(num)] = titulo
                    }
                }
            }
        }

        val chaptersFound = mutableSetOf<String>()
        conteudo.forEachIndexed { index, path ->
            val fileName = path.lowercase()
            val folder = path.substringBeforeLast("/", "").substringBeforeLast("\\", "").lowercase()

            var tag: String? = null
            if (folder.contains("capa") || fileName.contains("frente") || fileName.contains("tras") || fileName.contains("tudo") || fileName.contains("sumario") || fileName.contains(
                    "sumário"
                )
            ) {
                tag = when {
                    fileName.contains("frente") -> "Cover"
                    fileName.contains("tras") -> "Back"
                    fileName.contains("tudo") -> "All cover"
                    fileName.contains("sumario") || fileName.contains("sumário") -> "Sumary"
                    else -> null
                }
            }

            if (tag == null) {
                // Tenta identificar capítulos por pastas
                val regexCap = Regex("(?i)(capítulo|chapter|第)\\s*([\\d.]+)")
                val match = regexCap.find(path)
                if (match != null) {
                    val tipo = match.groupValues[1]
                    val numeroStr = match.groupValues[2]
                    val numero = Utils.getNumber(numeroStr)
                    if (numero != null) {
                        val formatado = mDecimal.format(numero)
                        val folderPath = path.substringBeforeLast("/", path).substringBeforeLast("\\", path)
                        if (!chaptersFound.contains(folderPath)) {
                            chaptersFound.add(folderPath)
                            val prefix = when (language) {
                                Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(formatado)}話"
                                Linguagem.ENGLISH -> "Chapter $formatado"
                                else -> "Capítulo $formatado"
                            }
                            val titulo = mapCapitulos[formatado]
                            tag = if (titulo != null) {
                                "$prefix ${Utils.SEPARADOR_IMPORTACAO} $formatado${Utils.SEPARADOR_CAPITULO}$titulo"
                            } else {
                                prefix
                            }
                        }
                    }
                }
            }

            if (tag != null) {
                newTags.add("$index${Utils.SEPARADOR_IMAGEM}$tag")
            }
        }

        if (newTags.isNotEmpty()) {
            item.tags = newTags.joinToString("\n")
            tbViewProcessar.refresh()
        }
    }

    private fun atualizarPaginaTag(item: Processar) {
        val file = item.file ?: return
        val conteudo = mRarService.listarConteudo(file).sortedNaturally()
        if (conteudo.isEmpty()) return

        val tags = item.tags.split("\n")
        val updatedTags = mutableListOf<String>()

        tags.forEach { tagLine ->
            if (tagLine.isBlank()) return@forEach

            val parts = tagLine.split(Utils.SEPARADOR_IMAGEM, limit = 2)
            if (parts.size < 2) {
                updatedTags.add(tagLine)
                return@forEach
            }

            val tagText = parts[1]
            val tagTextLower = tagText.lowercase()

            var foundIndex = -1

            // 1. Identificar o que procurar no RAR
            if (tagTextLower.contains("cover") || tagTextLower.contains("frente")) {
                foundIndex = conteudo.indexOfFirst { it.lowercase().contains("frente") }
            } else if (tagTextLower.contains("back") || tagTextLower.contains("tras")) {
                foundIndex = conteudo.indexOfFirst { it.lowercase().contains("tras") }
            } else if (tagTextLower.contains("all cover") || tagTextLower.contains("tudo")) {
                foundIndex = conteudo.indexOfFirst { it.lowercase().contains("tudo") }
            } else if (tagTextLower.contains("sumary") || tagTextLower.contains("sumario") || tagTextLower.contains("sumário")) {
                foundIndex = conteudo.indexOfFirst { it.lowercase().contains("sumario") || it.lowercase().contains("sumário") }
            } else {
                // Procurar por capítulo
                val capInfo = if (tagText.contains(Utils.SEPARADOR_IMPORTACAO)) {
                    tagText.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim().substringBefore(Utils.SEPARADOR_CAPITULO).trim()
                } else {
                    val match = Regex("(?i)(capítulo|chapter|第)\\s*([\\d.]+)").find(tagText)
                    if (match != null) {
                        val num = Utils.getNumber(match.groupValues[2])
                        if (num != null) mDecimal.format(num) else null
                    } else null
                }

                if (capInfo != null) {
                    val num = Utils.getNumber(capInfo)
                    if (num != null) {
                        val formatado = mDecimal.format(num)
                        // Procura a primeira página de uma pasta que contenha o número do capítulo
                        foundIndex = conteudo.indexOfFirst { path ->
                            val folder = path.substringBeforeLast("/", path).substringBeforeLast("\\", path).lowercase()
                            folder.contains(formatado) || folder.contains(num.toInt().toString())
                        }
                    }
                }
            }

            if (foundIndex != -1) {
                updatedTags.add("$foundIndex${Utils.SEPARADOR_IMAGEM}$tagText")
            } else {
                updatedTags.add(tagLine)
            }
        }

        item.tags = updatedTags.joinToString("\n")
        tbViewProcessar.refresh()
    }

    private fun List<String>.sortedNaturally(): List<String> {
        val regex = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)".toRegex()
        return this.sortedWith { s1, s2 ->
            val chunks1 = s1.split(regex)
            val chunks2 = s2.split(regex)
            var result = 0
            for (i in 0 until minOf(chunks1.size, chunks2.size)) {
                val c1 = chunks1[i]
                val c2 = chunks2[i]
                if (c1 != c2) {
                    val n1 = c1.toLongOrNull()
                    val n2 = c2.toLongOrNull()
                    result = if (n1 != null && n2 != null) n1.compareTo(n2) else c1.compareTo(c2, ignoreCase = true)
                    if (result != 0) break
                }
            }
            if (result == 0) chunks1.size.compareTo(chunks2.size) else result
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

        tbViewProcessar.selectionModel.selectionMode = SelectionMode.MULTIPLE

        linkaCelulas()
        configurarAtalhosGrid()
        habilita()
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/AbaComicInfo.fxml")
    }

}