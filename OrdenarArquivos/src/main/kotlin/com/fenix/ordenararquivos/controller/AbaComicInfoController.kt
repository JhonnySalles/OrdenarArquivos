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
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
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

    @FXML
    private fun onBtnCapitulos() {
        val callback: Callback<ObservableList<Volume>, Boolean> = Callback<ObservableList<Volume>, Boolean> { param ->
            val decimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
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
                                val numero = Utils.getNumber(it)
                                capitulos.find { c -> c.capitulo == numero }?.run {
                                    capitulos.remove(this)
                                    capitulo += " " + Utils.SEPARADOR_IMPORTACAO + " " + decimal.format(this.capitulo) + separador + if (linguagem == Linguagem.JAPANESE && this.japones.isNotEmpty()) this.japones else this.ingles
                                }
                            }
                        }
                        tags.add(capitulo)
                    }

                    if (capitulos.isNotEmpty())
                        for (capitulo in capitulos)
                            tags.add("-1${Utils.SEPARADOR_IMAGEM} Capítulo novo ${Utils.SEPARADOR_IMPORTACAO} ${decimal.format(capitulo.capitulo) + separador + if (linguagem == Linguagem.JAPANESE && capitulo.japones.isNotEmpty()) capitulo.japones else capitulo.ingles}")

                    item.tags = tags.joinToString("\n")
                }
            tbViewProcessar.refresh()
            null
        }
        PopupCapitulos.abreTelaCapitulos(controllerPai.rootStack, controllerPai.rootTab, callback, cbLinguagem.value, mObsListaProcessar, Utils.SEPARADOR_CAPITULO)
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
            for (item in mObsListaProcessar)
                gerarTagItem(item)
            tbViewProcessar.refresh()
        }
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
        btnCarregar.isDisable = true
        tbViewProcessar.isDisable = true
    }

    private fun habilita() {
        txtPastaProcessar.isDisable = false
        btnPesquisarPastaProcessar.isDisable = false
        btnCarregar.isDisable = false
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

            item.comicInfo?.pages?.forEach { it.bookmark = "" }

            var sumario = "*Chapter Titles Manual*\n"
            val tags = item.tags.split("\n")
            for (tag in tags) {
                val imagem = tag.substringBefore(Utils.SEPARADOR_IMAGEM)
                var capitulo = tag.substringAfter(Utils.SEPARADOR_IMAGEM).trim()

                if (capitulo.endsWith(Utils.SEPARADOR_IMPORTACAO))
                    capitulo = capitulo.substringBeforeLast(Utils.SEPARADOR_IMPORTACAO).trim()

                if (capitulo.isEmpty())
                    continue

                capitulo.lowercase().let {
                    if (it.contains("第") || it.contains("chapter") || it.contains("capítulo")) {
                        val numero = if (it.contains("第"))
                            it.substringBefore("-").replace("第", "Chapter ").replace("話", "").trim()
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

    private fun gerarTagItem(item: Processar) {
        val decimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
        val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
        val tags = item.comicInfo!!.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { p ->
            p.image.toString() + Utils.SEPARADOR_IMAGEM + p.bookmark!!.substringBeforeLast("-", p.bookmark!!).trim()
                .let { b -> if (b.lowercase().contains("第") || b.lowercase().contains("chapter") || b.lowercase().contains("capítulo")) {
                    val capitulo = Utils.getNumber(b) ?: 0.0
                    when (language) {
                        Linguagem.JAPANESE -> "第${decimal.format(capitulo)}話"
                        Linguagem.PORTUGUESE -> "Capítulo ${decimal.format(capitulo)}"
                        else -> "Chapter ${decimal.format(capitulo)}"
                    }
                } else b }
        }?.toList() ?: emptyList()
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

        clTags.cellFactory = TextAreaTableCell.forTableColumn()
        clTags.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            e.tableView.items[e.tablePosition.row].tags = e.newValue
        }

        val menu = ContextMenu()
        val salvar = MenuItem("Salvar ComicInfo")
        salvar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                salvarComicInfoItem(tbViewProcessar.selectionModel.selectedItem)
        }
        val processar = MenuItem("Processar OCR")
        processar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                processarOcrItem(tbViewProcessar.selectionModel.selectedItem)
        }
        val tags = MenuItem("Gerar Tags")
        tags.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem)
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
        menu.items.add(salvar)
        menu.items.add(processar)
        menu.items.add(tags)
        menu.items.add(remover)

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
                            linhas.add(if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) linha.substringBeforeLast(Utils.SEPARADOR_IMPORTACAO).trim() + " - " + linha.substringAfterLast(separador) else linha)

                        textArea.text = linhas.joinToString("\n")
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
                        val newLine = line.substringBeforeLast(Utils.SEPARADOR_IMPORTACAO).trim() + " - " + line.substringAfterLast(separador)
                        textArea.text = before + newLine + last
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
                        val lines = txt.split("\n").toMutableList()
                        val scroll = textArea.scrollTopProperty().value

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")

                        if (!line.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        var caret = before.indexOf(line)

                        var index = -1
                        for ((idx, item) in lines.withIndex()) {
                            if (item == line) {
                                index = idx
                                break
                            }
                        }

                        if (key.code == KeyCode.UP) {
                            if (index > 0) {
                                if (index >= lines.size)
                                    index = lines.size - 2

                                var spaco = -1
                                for (i in (index - 1) downTo 2) {
                                    if (!lines[i].contains(Utils.SEPARADOR_IMPORTACAO)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = 1
                                if (spaco > 0)
                                    inicio = spaco + 1

                                for (i in inicio until index + 1) {
                                    lines[i-1] = lines[i-1] + if (lines[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + lines[i].substringAfterLast(Utils.SEPARADOR_IMPORTACAO) else ""
                                    lines[i] = lines[i].substringBeforeLast(Utils.SEPARADOR_IMPORTACAO)
                                }
                            }
                        } else if (key.code == KeyCode.DOWN) {
                            if (index < lines.size - 1) {
                                var spaco = -1
                                for (i in index + 1 until lines.size) {
                                    if (!lines[i].contains(Utils.SEPARADOR_IMPORTACAO)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = lines.size - 2
                                if (spaco > 0 && spaco < lines.size -1)
                                    inicio = spaco - 1

                                for (i in inicio downTo index) {
                                    lines[i+1] = lines[i+1] + if (lines[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + lines[i].substringAfterLast(Utils.SEPARADOR_IMPORTACAO) else ""
                                    lines[i] = lines[i].substringBeforeLast(Utils.SEPARADOR_IMPORTACAO)
                                }
                            }
                        }

                        textArea.text = lines.joinToString(separator = "\n")

                        caret = if (key.code == KeyCode.UP)
                            textArea.text.substring(0, caret).lastIndexOf(Utils.SEPARADOR_IMPORTACAO)
                        else
                            textArea.text.indexOf(Utils.SEPARADOR_IMPORTACAO, caret)

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