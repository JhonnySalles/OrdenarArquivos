package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.jfoenix.controls.JFXTabPane
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Tab
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*


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
    private lateinit var lblProgresso: Label

    @FXML
    private lateinit var pbProgresso: ProgressBar

    @FXML
    private lateinit var arquivoController: AbaArquivoController

    @FXML
    private lateinit var comicinfoController: AbaComicInfoController

    @FXML
    private lateinit var pastasController: AbaPastasController

    @FXML
    private fun onSelectChanged(event: Event) {
        if (::tbTabArquivo.isInitialized && ::tbTabPasta.isInitialized) {
            AbaArquivoController.isAbaSelecionada = tbTabArquivo.isSelected
            AbaPastasController.isAbaSelecionada = tbTabPasta.isSelected
        } else
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

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        /* Setando as variáveis para o alerta padrão. */
        AlertasPopup.rootStackPane = spGlobal
        AlertasPopup.nodeBlur = tpGlobal
        Notificacoes.rootAnchorPane = apGlobal

        arquivoController.controllerPai = this
        comicinfoController.controllerPai = this
        pastasController.controllerPai = this

        arquivoController.limpaCampos()
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/TelaInicial.fxml")
        val iconLocate: String get() = "/images/icoProcessar_512.png"
    }
}
