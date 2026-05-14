package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.notification.ConfirmaModal
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.effect.BoxBlur
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.util.converter.IntegerStringConverter
import java.net.URL
import java.util.*

class AbaMangaController : Initializable {

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var txtFiltro: JFXTextField

    @FXML
    private lateinit var tbViewManga: TableView<Manga>

    @FXML
    private lateinit var clId: TableColumn<Manga, Long>

    @FXML
    private lateinit var clNome: TableColumn<Manga, String>

    @FXML
    private lateinit var clVolume: TableColumn<Manga, String>

    @FXML
    private lateinit var clCapitulo: TableColumn<Manga, String>

    @FXML
    private lateinit var clArquivo: TableColumn<Manga, String>

    @FXML
    private lateinit var clCapitulos: TableColumn<Manga, String>

    @FXML
    private lateinit var clQuantidade: TableColumn<Manga, Int>

    @FXML
    private lateinit var clComic: TableColumn<Manga, String>

    @FXML
    private lateinit var clAcoes: TableColumn<Manga, Void>

    private val mServiceManga = MangaServices()
    private val mServiceComicInfo = ComicInfoServices()
    private val mMangas: ObservableList<Manga> = FXCollections.observableArrayList()

    private var mOffset = 0
    private val mLimit = 1000
    private var mCarregando = false
    private var mTemMais = true
    private var mPrimeiroCarregamento = true

    lateinit var controllerPai: TelaInicialController

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        initTable()

        // Listener para Scroll Infinito
        tbViewManga.lookup(".virtual-flow")?.let { flow ->
            val scrollBar = (flow as ScrollPane).childrenUnmodifiable.filterIsInstance<ScrollBar>().firstOrNull { it.orientation == javafx.geometry.Orientation.VERTICAL }
            scrollBar?.valueProperty()?.addListener { _, _, newValue ->
                if (newValue.toDouble() >= 0.9 && !mCarregando && mTemMais)
                    carregarDados(incremental = true)
            }
        }
    }

    private fun initTable() {
        clId.cellValueFactory = PropertyValueFactory("id")

        clNome.cellValueFactory = PropertyValueFactory("nome")
        clNome.cellFactory = TextFieldTableCell.forTableColumn()
        clNome.setOnEditCommit { it.rowValue.nome = it.newValue }

        clVolume.cellValueFactory = PropertyValueFactory("volume")
        clVolume.cellFactory = TextFieldTableCell.forTableColumn()
        clVolume.setOnEditCommit { it.rowValue.volume = it.newValue }

        clCapitulo.cellValueFactory = PropertyValueFactory("capitulo")
        clCapitulo.cellFactory = TextFieldTableCell.forTableColumn()
        clCapitulo.setOnEditCommit { it.rowValue.capitulo = it.newValue }

        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clArquivo.cellFactory = TextFieldTableCell.forTableColumn()
        clArquivo.setOnEditCommit { it.rowValue.arquivo = it.newValue }

        clCapitulos.cellValueFactory = PropertyValueFactory("capitulos")
        clCapitulos.cellFactory = TextFieldTableCell.forTableColumn()
        clCapitulos.setOnEditCommit { it.rowValue.capitulos = it.newValue }

        clQuantidade.cellValueFactory = PropertyValueFactory("quantidade")
        clQuantidade.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        clQuantidade.setOnEditCommit { it.rowValue.quantidade = it.newValue }

        clComic.cellValueFactory = PropertyValueFactory("comic")
        clComic.cellFactory = TextFieldTableCell.forTableColumn()
        clComic.setOnEditCommit { it.rowValue.comic = it.newValue }

        initAcoes()

        tbViewManga.setRowFactory {
            val row = TableRow<Manga>()
            row.setOnMouseClicked { event ->
                if (event.clickCount == 2 && !row.isEmpty)
                    abrirPopupComicInfo(listOf(row.item))
            }

            val menu = ContextMenu()
            val itemEditar = MenuItem("Editar ComicInfo / Pesquisar MAL").apply {
                setOnAction {
                    val selected = tbViewManga.selectionModel.selectedItems
                    if (selected.isNotEmpty())
                        abrirPopupComicInfo(selected.toList())
                }
            }
            menu.items.add(itemEditar)

            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.`when`(row.emptyProperty())
                    .then(null as ContextMenu?)
                    .otherwise(menu)
            )

            row
        }

        tbViewManga.items = mMangas
    }

    private fun initAcoes() {
        clAcoes.setCellFactory {
            object : TableCell<Manga, Void>() {
                private val btnConfirmar =
                    JFXButton("Salvar").apply {
                        styleClass.add("background-Green2")
                        setOnAction {
                            val manga = tableView.items[index]
                            salvarManga(manga)
                        }
                    }

                private val btnExcluir =
                    JFXButton("Cancelar").apply {
                        styleClass.add("background-Red2")
                        setOnAction {
                            val manga = tableView.items[index]
                            excluirManga(manga)
                        }
                    }

                private val container = HBox(5.0, btnExcluir, btnConfirmar).apply { alignment = javafx.geometry.Pos.CENTER }

                override fun updateItem(item: Void?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty) null else container
                }
            }
        }
    }

    private fun carregarDados(incremental: Boolean = false) {
        if (mCarregando)
            return

        mCarregando = true
        if (!incremental) {
            mOffset = 0
            mTemMais = true
            mMangas.clear()
        }

        val useProgress = !controllerPai.rootProgress.progressProperty().isBound

        val task = object : Task<List<Manga>>() {
            override fun call(): List<Manga> {
                if (useProgress) {
                    updateProgress(-1.0, 1.0)
                    updateMessage("Carregando mangas...")
                }
                return mServiceManga.findAll(txtFiltro.text, mLimit, mOffset)
            }

            override fun succeeded() {
                val novos = value
                if (novos.isEmpty())
                    mTemMais = false
                else {
                    mMangas.addAll(novos)
                    mOffset += mLimit
                }
                mCarregando = false
                if (useProgress) {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                }
            }

            override fun failed() {
                mCarregando = false
                if (useProgress) {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                }
                Platform.runLater { AlertasModal.erro("Erro ao carregar dados", exception.message ?: "Erro desconhecido") }
            }

            override fun cancelled() {
                mCarregando = false
                if (useProgress) {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                }
            }
        }
        
        if (useProgress) {
            controllerPai.rootProgress.progressProperty().bind(task.progressProperty())
            controllerPai.rootMessage.textProperty().bind(task.messageProperty())
        }
        Thread(task).start()
    }

    @FXML
    private fun onKeyFiltro() {
        // Simples debounce poderia ser adicionado aqui, mas por ora carregamento direto
        mPrimeiroCarregamento = false
        carregarDados()
    }

    fun checkCarregarDados() {
        if (mPrimeiroCarregamento) {
            mPrimeiroCarregamento = false
            carregarDados()
        }
    }

    private fun salvarManga(manga: Manga) {
        try {
            mServiceManga.save(manga)
            // Atualizar ComicInfo se o campo comic mudou
            if (manga.comic.isNotEmpty()) {
                val ci = mServiceComicInfo.find(manga.nome, "pt") ?: mServiceComicInfo.find(manga.nome, "ja") ?: ComicInfo()
                ci.comic = manga.comic
                ci.series = manga.nome
                mServiceComicInfo.save(ci)
            }
            Platform.runLater {
                controllerPai.rootMessage.text = "Manga ${manga.nome} salvo com sucesso."
            }
        } catch (e: Exception) {
            Platform.runLater { AlertasModal.erro("Erro ao processar", e.message ?: "Erro desconhecido") }
        }
    }

    private fun excluirManga(manga: Manga) {
        if (ConfirmaModal.confirmacao(
                controllerPai.rootStack,
                controllerPai.rootTab,
                "Excluir Manga",
                "Deseja realmente excluir o manga: ${manga.nome}?\nEsta ação excluirá também os caminhos e o comicinfo associado."
            )
        ) {
            try {
                mServiceManga.deleteManga(manga)
                mMangas.remove(manga)
                controllerPai.rootMessage.text = "Manga excluído com sucesso."
            } catch (e: Exception) {
                Platform.runLater { AlertasModal.erro("Erro ao excluir manga", e.message ?: "Erro desconhecido") }
            }
        }
    }

    internal fun abrirPopupComicInfo(mangas: List<Manga>) {
        if (mangas.isEmpty()) return

        try {
            val mangaPrimeiro = mangas.first()
            val loader = FXMLLoader(javaClass.getResource("/view/PopupComicInfo.fxml"))
            val root = loader.load<AnchorPane>()
            val controller = loader.getController<PopupComicInfoController>()

            // Busca o ComicInfo associado ao primeiro manga selecionado
            val comicInfo = mServiceComicInfo.find(mangaPrimeiro.nome, "pt") ?: mServiceComicInfo.find(mangaPrimeiro.nome, "ja") ?: ComicInfo().apply {
                series = mangaPrimeiro.nome
                comic = mangaPrimeiro.comic
            }

            controller.setComicInfo(comicInfo)

            val blur = BoxBlur(3.0, 3.0, 3)
            val dialogLayout = com.jfoenix.controls.JFXDialogLayout()
            dialogLayout.setBody(root)
            val dialog = JFXDialog(controllerPai.rootStack, dialogLayout, JFXDialog.DialogTransition.CENTER)
            dialog.isOverlayClose = false

            controller.onClose = { dialog.close() }
            controller.onSave = { ci ->
                // Se houver mais de um registro, atualiza todos os selecionados
                if (mangas.size > 1) {
                    mangas.forEach { m ->
                        if (m != mangaPrimeiro) {
                            val ciManga = mServiceComicInfo.find(m.nome, "pt") ?: mServiceComicInfo.find(m.nome, "ja") ?: ComicInfo().apply {
                                series = m.nome
                                comic = m.comic
                            }
                            ciManga.merge(ci)
                            mServiceComicInfo.save(ciManga)
                        }
                    }
                }
                // Após salvar, atualiza os nomes na grid caso tenham mudado
                mangas.forEach { m ->
                    m.nome = ci.series ?: ci.title ?: m.nome
                    m.comic = ci.comic
                }
                tbViewManga.refresh()
            }

            dialog.setOnDialogClosed {
                controllerPai.rootTab.effect = null
                controllerPai.rootTab.isDisable = false
            }

            controllerPai.rootTab.effect = blur
            controllerPai.rootTab.isDisable = true
            dialog.show()

        } catch (e: Exception) {
            Platform.runLater { AlertasModal.erro("Erro ao processar", e.message ?: "Erro desconhecido") }
        }
    }

    companion object {
        val fxmlLocate: java.net.URL? get() = TelaInicialController::class.java.getResource("/view/AbaManga.fxml")
        var isAbaSelecionada = false
    }
}
