package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.model.entities.api.ApiComicInfoResumo
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.service.ReaderApiServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialogLayout
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.effect.BoxBlur
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.util.Callback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.util.*

class PopupBuscaApiController : Initializable {

    private val mLOG: Logger = LoggerFactory.getLogger(PopupBuscaApiController::class.java)

    @FXML
    lateinit var txtPesquisa: JFXTextField

    @FXML
    lateinit var btnPesquisar: JFXButton

    @FXML
    lateinit var lblStatus: Label

    @FXML
    lateinit var pbCarregando: ProgressBar

    @FXML
    lateinit var tbViewResultados: TableView<ApiComicInfoResumo>

    @FXML
    lateinit var clVolume: TableColumn<ApiComicInfoResumo, String>

    @FXML
    lateinit var clNumero: TableColumn<ApiComicInfoResumo, String>

    @FXML
    lateinit var clSerie: TableColumn<ApiComicInfoResumo, String>

    @FXML
    lateinit var clTitulo: TableColumn<ApiComicInfoResumo, String>

    @FXML
    lateinit var clIdioma: TableColumn<ApiComicInfoResumo, String>

    @FXML
    lateinit var clEditora: TableColumn<ApiComicInfoResumo, String>

    private val listaResultados: ObservableList<ApiComicInfoResumo> = FXCollections.observableArrayList()
    private var callbackSucesso: Callback<ComicInfo, Boolean>? = null

    val itemSelecionado: ApiComicInfoResumo?
        get() = tbViewResultados.selectionModel.selectedItem

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        configurarTabela()

        txtPesquisa.setOnKeyPressed { ke ->
            if (ke.code == KeyCode.ENTER) {
                onBtnPesquisar()
            }
        }
    }

    private fun configurarTabela() {
        clVolume.setCellValueFactory { cell ->
            SimpleStringProperty(cell.value.volume?.let { if (it % 1.0f == 0.0f) it.toInt().toString() else it.toString() } ?: "-")
        }
        clNumero.setCellValueFactory { cell ->
            SimpleStringProperty(if (cell.value.number % 1.0f == 0.0f) cell.value.number.toInt().toString() else cell.value.number.toString())
        }
        clSerie.setCellValueFactory { cell -> SimpleStringProperty(cell.value.series) }
        clTitulo.setCellValueFactory { cell -> SimpleStringProperty(cell.value.title) }
        clIdioma.setCellValueFactory { cell -> SimpleStringProperty(cell.value.languageISO ?: "-") }
        clEditora.setCellValueFactory { cell -> SimpleStringProperty(cell.value.publisher ?: "-") }

        tbViewResultados.items = listaResultados

        tbViewResultados.setRowFactory {
            val row = TableRow<ApiComicInfoResumo>()
            row.setOnMouseClicked { event ->
                if (event.button == MouseButton.PRIMARY && event.clickCount == 2 && !row.isEmpty) {
                    confirmarSelecao()
                }
            }
            row
        }
    }

    fun setPesquisaInicial(termo: String?) {
        if (!termo.isNullOrBlank()) {
            txtPesquisa.text = termo
            pesquisar(termo)
        }
    }

    @FXML
    fun onBtnPesquisar() {
        val termo = txtPesquisa.text.trim()
        if (termo.isBlank()) {
            lblStatus.text = "Digite um termo para pesquisar."
            return
        }
        pesquisar(termo)
    }

    private fun pesquisar(termo: String) {
        btnPesquisar.isDisable = true
        pbCarregando.isVisible = true
        pbCarregando.progress = -1.0
        lblStatus.text = "Buscando na API..."

        val task = object : Task<List<ApiComicInfoResumo>>() {
            override fun call(): List<ApiComicInfoResumo> {
                // Se a API ainda não estiver conectada, tenta conectar primeiro
                if (ReaderApiServices.status != ReaderApiServices.ApiStatus.CONECTADO) {
                    ReaderApiServices.descobrirEConectar()
                }
                return ReaderApiServices.pesquisar(termo, 100)
            }

            override fun succeeded() {
                btnPesquisar.isDisable = false
                pbCarregando.isVisible = false
                listaResultados.setAll(value)

                if (value.isEmpty()) {
                    lblStatus.text = "Nenhum resultado encontrado para '$termo'."
                } else {
                    lblStatus.text = "${value.size} resultados encontrados (ordenados por volume mais recente)."
                    tbViewResultados.selectionModel.selectFirst()
                }
            }

            override fun failed() {
                btnPesquisar.isDisable = false
                pbCarregando.isVisible = false
                val erroMsg = exception?.message ?: "Erro desconhecido"
                mLOG.error("Falha ao pesquisar na API: $erroMsg", exception)
                lblStatus.text = "Erro: $erroMsg"
                Notificacoes.notificacao(Notificacao.ERRO, "ReaderServer API", "Falha na busca: $erroMsg")
            }
        }

        Thread(task).start()
    }

    fun confirmarSelecao() {
        val selecionado = itemSelecionado
        if (selecionado == null) {
            lblStatus.text = "Selecione um item da lista."
            return
        }

        btnPesquisar.isDisable = true
        pbCarregando.isVisible = true
        pbCarregando.progress = -1.0
        lblStatus.text = "Baixando ComicInfo do servidor..."

        val task = object : Task<ComicInfo>() {
            override fun call(): ComicInfo {
                return ReaderApiServices.baixarComicInfo(selecionado.id)
            }

            override fun succeeded() {
                btnPesquisar.isDisable = false
                pbCarregando.isVisible = false
                callbackSucesso?.call(value)
                fecharDialog()
            }

            override fun failed() {
                btnPesquisar.isDisable = false
                pbCarregando.isVisible = false
                val erroMsg = exception?.message ?: "Erro desconhecido"
                mLOG.error("Falha ao baixar ComicInfo da API: $erroMsg", exception)
                lblStatus.text = "Erro ao baixar ComicInfo: $erroMsg"
                Notificacoes.notificacao(Notificacao.ERRO, "ReaderServer API", "Erro ao carregar dados: $erroMsg")
            }
        }

        Thread(task).start()
    }

    private fun fecharDialog() {
        dialog?.close()
    }

    companion object {
        private val mLOG: Logger = LoggerFactory.getLogger(PopupBuscaApiController::class.java)
        private val STYLE_SHEET: String = PopupBuscaApiController::class.java.getResource("/css/Dark_TelaInicial.css").toExternalForm()
        private var dialog: JFXDialog? = null

        @JvmStatic
        fun abreTelaBuscaApi(
            rootStackPane: StackPane,
            nodeBlur: Node,
            callback: Callback<ComicInfo, Boolean>,
            termoInicial: String? = null
        ) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                dialog = JFXDialog(rootStackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)
                dialog?.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                    if (event.code == KeyCode.ESCAPE) {
                        dialog?.close()
                        event.consume()
                    }
                }

                val loader = FXMLLoader(fxmlLocate)
                val root: Parent = loader.load()
                val controller: PopupBuscaApiController = loader.getController()
                controller.callbackSucesso = callback

                val titulo = Label("Pesquisar na ReaderServer API")
                titulo.font = Font.font(18.0)
                titulo.textFill = Color.WHITE
                val hbTitulo = HBox(titulo)
                hbTitulo.alignment = Pos.CENTER_LEFT
                hbTitulo.maxWidth = Double.MAX_VALUE
                dialogLayout.setHeading(hbTitulo)

                val botoes = mutableListOf<JFXButton>()
                val btnVoltar = JFXButton("Voltar")
                btnVoltar.setOnAction { dialog?.close() }
                btnVoltar.styleClass.add("background-White1")
                botoes.add(btnVoltar)

                val btnConfirmar = JFXButton("Confirmar")
                btnConfirmar.setOnAction { controller.confirmarSelecao() }
                btnConfirmar.styleClass.add("background-Green2")
                btnConfirmar.styleClass.add("texto-stilo-1")
                botoes.add(btnConfirmar)

                dialogLayout.setBody(root)
                dialogLayout.setActions(botoes)
                dialogLayout.styleClass.add("dialog-black")
                dialog?.stylesheets?.add(STYLE_SHEET)
                dialog?.padding = Insets(0.0)
                dialog?.setOnDialogClosed {
                    nodeBlur.effect = null
                    nodeBlur.isDisable = false
                }

                nodeBlur.effect = blur
                nodeBlur.isDisable = true
                dialog?.show()

                Platform.runLater {
                    controller.setPesquisaInicial(termoInicial)
                }
            } catch (e: IOException) {
                mLOG.error("Erro ao abrir PopupBuscaApi", e)
            }
        }

        val fxmlLocate: URL get() = PopupBuscaApiController::class.java.getResource("/view/PopupBuscaApi.fxml") as URL
    }
}
