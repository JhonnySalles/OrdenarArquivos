package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.animation.Animacao
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.service.GoogleDriveDownloadService
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTabPane
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Tab
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import kotlin.system.exitProcess


class TelaInicialController : Initializable {

    private val mLOG = LoggerFactory.getLogger(TelaInicialController::class.java)

    //<--------------------------  PRINCIPAL   -------------------------->

    @FXML
    private lateinit var apGlobal: AnchorPane

    @FXML
    private lateinit var spGlobal: StackPane

    @FXML
    private lateinit var tpGlobal: JFXTabPane

    @FXML
    private lateinit var tbTabArquivo: Tab

    @FXML
    private lateinit var tbTabPasta: Tab

    @FXML
    private lateinit var tbTabComicInfo: Tab

    @FXML
    private lateinit var tbTabManga: Tab

    @FXML
    private lateinit var lblProgresso: Label

    @FXML
    private lateinit var pbProgresso: ProgressBar

    @FXML
    private lateinit var btnAtualizar: JFXButton

    @FXML
    private lateinit var btnCompartilhamento: JFXButton

    @FXML
    private lateinit var imgCompartilhamento: ImageView

    @FXML
    lateinit var apDragOverlay: AnchorPane

    @FXML
    lateinit var spDragDropZone: StackPane

    @FXML
    lateinit var lblDragDrop: Label

    @FXML
    private lateinit var arquivoController: AbaArquivoController

    @FXML
    private lateinit var comicinfoController: AbaComicInfoController

    @FXML
    private lateinit var pastasController: AbaPastasController

    @FXML
    private lateinit var abaMangaController: AbaMangaController

    lateinit var mSincronizacao: SincronizacaoServices
    private lateinit var mAnimacao: Animacao

    private val imgAnimaCompartilha = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamento_48.png"))
    private val imgAnimaCompartilhaEspera = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoEspera_48.png"))
    private val imgAnimaCompartilhaErro = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoErro_48.png"))
    private val imgAnimaCompartilhaEnvio = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoEnvio_48.png"))

    @FXML
    private fun onSelectChanged(event: Event) {
        if (::tbTabArquivo.isInitialized && ::tbTabPasta.isInitialized && ::tbTabManga.isInitialized) {
            AbaArquivoController.isAbaSelecionada = tbTabArquivo.isSelected
            AbaPastasController.isAbaSelecionada = tbTabPasta.isSelected
            AbaMangaController.isAbaSelecionada = tbTabManga.isSelected

            if (tbTabManga.isSelected) {
                abaMangaController.checkCarregarDados()
            }
        } else if (::tbTabArquivo.isInitialized)
            AbaArquivoController.isAbaSelecionada = true
    }

    val rootStack : StackPane by lazy { spGlobal }
    val rootAnchor : AnchorPane by lazy { apGlobal }
    val rootTab : JFXTabPane by lazy { tpGlobal }
    val rootMessage : Label by lazy { lblProgresso }
    val rootProgress : ProgressBar by lazy { pbProgresso }

    fun clearProgress() {
        lblProgresso.text = ""
        pbProgresso.progress = 0.0
    }

    fun setCursor(cursor : Cursor?) = apGlobal.cursorProperty().set(cursor)

    fun configurarAtalhos(scene: Scene) {
        arquivoController.configurarAtalhos(scene)
        pastasController.configurarAtalhos(scene)
    }

    @FXML
    private fun onBtnAtualizar() {
        if (!AlertasPopup.confirmacaoModal("Atualização", "Deseja verificar e aplicar atualizações do aplicativo?"))
            return

        btnAtualizar.isDisable = true
        setCursor(Cursor.WAIT)

        val updateService = GoogleDriveDownloadService()
        val task = object : Task<Void>() {
            private var updateResult: GoogleDriveDownloadService.UpdateResult? = null

            override fun call(): Void? {
                updateResult = updateService.performUpdate { message, progress ->
                    updateMessage(message)
                    if (progress >= 0) updateProgress(progress, 1.0)
                }
                return null
            }

            override fun succeeded() {
                pbProgresso.progressProperty().unbind()
                lblProgresso.textProperty().unbind()
                clearProgress()
                setCursor(null)
                btnAtualizar.isDisable = false

                val result = updateResult
                if (result != null && result.updated) {
                    Notificacoes.notificacao(Notificacao.SUCESSO, "Atualização", result.message)
                    // Schedule restart
                    Platform.runLater {
                        if (AlertasPopup.confirmacaoModal("Reiniciar", "Atualização aplicada com sucesso! Deseja reiniciar o aplicativo agora?")) {
                            updateService.restartApplication()
                            DataBase.closeConnection()
                            exitProcess(0)
                        }
                    }
                } else {
                    Notificacoes.notificacao(Notificacao.ALERTA, "Atualização", result?.message ?: "Nenhuma atualização encontrada.")
                }
            }

            override fun failed() {
                pbProgresso.progressProperty().unbind()
                lblProgresso.textProperty().unbind()
                clearProgress()
                setCursor(null)
                btnAtualizar.isDisable = false

                val errorMsg = exception?.message ?: "Erro desconhecido"
                mLOG.error("Erro na atualização: $errorMsg", exception)
                Notificacoes.notificacao(Notificacao.ERRO, "Atualização", "Erro ao atualizar: $errorMsg")
            }
        }

        pbProgresso.progressProperty().bind(task.progressProperty())
        lblProgresso.textProperty().bind(task.messageProperty())
        Thread(task).start()
    }

    @FXML
    private fun onBtnCompartilhamento() {
        compartilhamento()
    }

    private fun compartilhamento() {
        val compartilha: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                mSincronizacao.consultar()
                return mSincronizacao.sincroniza()
            }

            override fun succeeded() {
                if (!value)
                    setLog("Não foi possível sincronizar os dados com a cloud", true)
                else
                    setLog("Sincronização de dados com a cloud concluída com sucesso.")
            }
        }

        val t = Thread(compartilha)
        t.start()
    }

    fun animacaoSincronizacao(isProcessando: Boolean, isErro: Boolean) {
        Platform.runLater {
            if (isProcessando)
                mAnimacao.tmSincronizacao.play()
            else {
                mAnimacao.tmSincronizacao.stop()
                if (isErro)
                    imgCompartilhamento.image = imgAnimaCompartilhaErro
                else
                    imgCompartilhamento.image = imgAnimaCompartilhaEnvio
            }
        }
    }

    fun setLog(texto: String, isError: Boolean = false) {
        Platform.runLater {
            lblProgresso.text = texto
            if (isError) {
                // Aqui poderíamos adicionar uma lógica visual para erro no lblProgresso se necessário
            }
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        /* Setando as variáveis para o alerta padrão. */
        AlertasPopup.rootStackPane = spGlobal
        AlertasPopup.nodeBlur = tpGlobal
        Notificacoes.rootAnchorPane = apGlobal

        mAnimacao = Animacao()
        mAnimacao.animaSincronizacao(imgCompartilhamento, imgAnimaCompartilha, imgAnimaCompartilhaEspera)
        mSincronizacao = SincronizacaoServices(this)

        arquivoController.controllerPai = this
        comicinfoController.controllerPai = this
        pastasController.controllerPai = this
        abaMangaController.controllerPai = this

        arquivoController.limpaCampos()
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/TelaInicial.fxml")
        val iconLocate: String get() = "/images/icoProcessar_512.png"
    }
}
