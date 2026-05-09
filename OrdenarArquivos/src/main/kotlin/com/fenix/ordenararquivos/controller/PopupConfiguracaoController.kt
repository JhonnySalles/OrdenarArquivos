package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXTextField
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*

class PopupConfiguracaoController : Initializable {

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var txtCaminhoTagger: JFXTextField

    @FXML
    private lateinit var btnPesquisarTagger: JFXButton

    @FXML
    private lateinit var cbGeminiModel: JFXComboBox<String>

    @FXML
    private lateinit var txtUpdateLink: JFXTextField

    @FXML
    private lateinit var lblRegistrosMal: Label

    @FXML
    private lateinit var spRegistrosMal: Spinner<Int>

    @FXML
    private lateinit var lineFocus: Region

    @FXML
    private lateinit var btnCancelar: JFXButton

    @FXML
    private lateinit var btnConfirmar: JFXButton

    private lateinit var controller: TelaInicialController
    var controllerPai: TelaInicialController
        get() = controller
        set(controller) {
            this.controller = controller
        }

    private var onClose: (() -> Unit)? = null

    private val geminiModelsMap = mapOf(
        "Gemini 2.5 Flash" to "gemini-2.5-flash-lite",
        "Gemini 2.0 Flash" to "gemini-2.0-flash",
        "Gemini 1.5 Flash" to "gemini-1.5-flash",
        "Gemini 1.5 Pro" to "gemini-1.5-pro"
    )

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupFields()
        loadConfig()
    }

    private fun setupFields() {
        // Setup Gemini ComboBox
        cbGeminiModel.items = FXCollections.observableArrayList(geminiModelsMap.keys.toList())

        // Setup Spinner for MAL records
        val valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 50)
        spRegistrosMal.valueFactory = valueFactory

        // Colors
        val colorFocus = Color.web("#0cff00")
        val colorUnfocus = Color.web("#ababab")

        // Animation and Label color sync
        val focusListener = { _: Any?, _: Boolean, isFocused: Boolean ->
            val finalFocus = isFocused || spRegistrosMal.isFocused || spRegistrosMal.editor.isFocused
            lblRegistrosMal.textFill = if (finalFocus) colorFocus else colorUnfocus
            animateFocusLine(finalFocus)
        }

        spRegistrosMal.focusedProperty().addListener(focusListener)
        spRegistrosMal.editor.focusedProperty().addListener(focusListener)
    }

    private fun animateFocusLine(focused: Boolean) {
        val timeline = Timeline()
        val keyFrame = KeyFrame(
            Duration.millis(300.0),
            KeyValue(lineFocus.scaleXProperty(), if (focused) 1.0 else 0.0, Interpolator.EASE_BOTH)
        )
        timeline.keyFrames.add(keyFrame)
        timeline.play()
    }

    private fun loadConfig() {
        txtCaminhoTagger.text = Configuracao.caminhoCommicTagger
        txtUpdateLink.text = Configuracao.updateLink
        
        // Find display name for internal gemini model
        val currentModel = Configuracao.geminiModel
        val displayName = geminiModelsMap.entries.find { it.value == currentModel }?.key ?: geminiModelsMap.keys.first()
        cbGeminiModel.selectionModel.select(displayName)

        spRegistrosMal.valueFactory.value = Configuracao.registrosConsultaMal
    }

    @FXML
    private fun onBtnPesquisarTagger() {
        val pasta = Utils.selecionaPasta(txtCaminhoTagger.text)
        if (pasta != null) {
            txtCaminhoTagger.text = pasta.absolutePath
        }
    }

    @FXML
    private fun onBtnCancelar() {
        onClose?.invoke()
    }

    @FXML
    private fun onBtnConfirmar() {
        try {
            Configuracao.caminhoCommicTagger = txtCaminhoTagger.text.trim()
            Configuracao.updateLink = txtUpdateLink.text.trim()
            
            val selectedDisplay = cbGeminiModel.selectionModel.selectedItem
            Configuracao.geminiModel = geminiModelsMap[selectedDisplay] ?: "gemini-2.0-flash"
            
            Configuracao.registrosConsultaMal = spRegistrosMal.value

            Configuracao.saveProperties()
            
            Notificacoes.notificacao(Notificacao.SUCESSO, "Configurações", "Configurações salvas com sucesso!")
            onClose?.invoke()
        } catch (e: Exception) {
            Notificacoes.notificacao(Notificacao.ERRO, "Configurações", "Erro ao salvar configurações: ${e.message}")
        }
    }

    companion object {
        private val mLOG = LoggerFactory.getLogger(PopupConfiguracaoController::class.java)
        private lateinit var dialog: JFXDialog

        @JvmStatic
        fun abreTelaConfiguracao(rootStackPane: StackPane, nodeBlur: Node) {
            try {
                val dialog = JFXDialog()
                dialog.dialogContainer = rootStackPane
                dialog.transitionType = JFXDialog.DialogTransition.CENTER

                val loader = FXMLLoader()
                loader.location = PopupConfiguracaoController::class.java.getResource("/view/PopupConfiguracao.fxml")
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupConfiguracaoController = loader.getController()

                cnt.onClose = { dialog.close() }

                val blur = BoxBlur(3.0, 3.0, 3)
                dialog.setOnDialogClosed {
                    nodeBlur.effect = null
                    nodeBlur.isDisable = false
                }

                nodeBlur.effect = blur
                nodeBlur.isDisable = true

                dialog.content = newAnchorPane as Region
                dialog.padding = Insets(0.0)
                dialog.show()
            } catch (e: Exception) {
                mLOG.error("Erro ao abrir popup de configuração: ${e.message}", e)
            }
        }
    }
}
