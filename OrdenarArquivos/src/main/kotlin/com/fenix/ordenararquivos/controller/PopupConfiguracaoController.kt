package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.model.enums.OcrEngine
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.process.ocr.NativePaths
import com.fenix.ordenararquivos.process.ocr.PaddleOcrConfigApplier
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
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.awt.Desktop
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
    private lateinit var cbOcrEngine: JFXComboBox<OcrEngine>

    @FXML
    private lateinit var boxGemini: VBox

    @FXML
    private lateinit var cbGeminiModel: JFXComboBox<String>

    @FXML
    private lateinit var boxOllama: VBox

    @FXML
    private lateinit var txtOllamaUrl: JFXTextField

    @FXML
    private lateinit var txtOllamaModel: JFXTextField

    @FXML
    private lateinit var boxPaddle: VBox

    @FXML
    private lateinit var lblPaddleStatus: Label

    @FXML
    private lateinit var btnAbrirPastaPaddle: JFXButton

    @FXML
    private lateinit var chkPaddleCls: CheckBox

    @FXML
    private lateinit var chkPaddleUseAngleCls: CheckBox

    @FXML
    private lateinit var spPaddleLimitSideLen: Spinner<Int>

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
        cbOcrEngine.items = FXCollections.observableArrayList(*OcrEngine.values())
        cbOcrEngine.setConverter(object : javafx.util.StringConverter<OcrEngine>() {
            override fun toString(engine: OcrEngine?) = engine?.displayName ?: ""
            override fun fromString(string: String?) =
                OcrEngine.values().find { it.displayName == string } ?: OcrEngine.TESSERACT
        })
        cbOcrEngine.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateOcrBlocksState()
        }

        cbGeminiModel.items = FXCollections.observableArrayList(geminiModelsMap.keys.toList())

        spRegistrosMal.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 50)

        val paddleFactory = object : SpinnerValueFactory.IntegerSpinnerValueFactory(960, 4320, 2880, 32) {
            override fun decrement(steps: Int) {
                val newValue = (value - steps * 32).coerceAtLeast(960)
                value = (newValue / 32) * 32
            }

            override fun increment(steps: Int) {
                val newValue = (value + steps * 32).coerceAtMost(4320)
                value = (newValue / 32) * 32
            }
        }
        spPaddleLimitSideLen.valueFactory = paddleFactory

        val colorFocus = Color.web("#0cff00")
        val colorUnfocus = Color.web("#ababab")

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
        spRegistrosMal.valueFactory.value = Configuracao.registrosConsultaMal

        cbOcrEngine.selectionModel.select(Configuracao.ocrEngine)

        val currentModel = Configuracao.geminiModel
        val displayName = geminiModelsMap.entries.find { it.value == currentModel }?.key ?: geminiModelsMap.keys.first()
        cbGeminiModel.selectionModel.select(displayName)

        txtOllamaUrl.text = Configuracao.ollamaUrl
        txtOllamaModel.text = Configuracao.ollamaModel

        loadPaddleConfig()
        updateOcrBlocksState()
    }

    private fun loadPaddleConfig() {
        val paddleSettings = PaddleOcrConfigApplier.readCurrentValues()
        chkPaddleCls.isSelected = paddleSettings.cls
        chkPaddleUseAngleCls.isSelected = paddleSettings.useAngleCls
        spPaddleLimitSideLen.valueFactory.value = paddleSettings.limitSideLen

        lblPaddleStatus.text = if (PaddleOcrConfigApplier.isInstalled()) {
            "Instalado em natives/paddleocr/"
        } else {
            "Não encontrado — instale em natives/paddleocr/"
        }
    }

    private fun updateOcrBlocksState() {
        val engine = cbOcrEngine.selectionModel.selectedItem ?: OcrEngine.TESSERACT
        boxGemini.opacity = if (engine == OcrEngine.GEMINI) 1.0 else 0.55
        boxOllama.opacity = 0.55
        boxPaddle.opacity = if (engine == OcrEngine.PADDLE) 1.0 else 0.55
    }

    @FXML
    private fun onBtnPesquisarTagger() {
        val pasta = Utils.selecionaPasta(txtCaminhoTagger.text)
        if (pasta != null) {
            txtCaminhoTagger.text = pasta.absolutePath
        }
    }

    @FXML
    private fun onBtnAbrirPastaPaddle() {
        try {
            val dir = NativePaths.paddleDir
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir)
            } else {
                Notificacoes.notificacao(Notificacao.AVISO, "PaddleOCR", "Não foi possível abrir a pasta automaticamente: ${dir.absolutePath}")
            }
        } catch (e: Exception) {
            Notificacoes.notificacao(Notificacao.ERRO, "PaddleOCR", "Erro ao abrir pasta: ${e.message}")
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
            Configuracao.registrosConsultaMal = spRegistrosMal.value

            Configuracao.ocrEngine = cbOcrEngine.selectionModel.selectedItem ?: OcrEngine.TESSERACT

            val selectedDisplay = cbGeminiModel.selectionModel.selectedItem
            Configuracao.geminiModel = geminiModelsMap[selectedDisplay] ?: "gemini-2.0-flash"

            Configuracao.ollamaUrl = txtOllamaUrl.text.trim().ifEmpty { "http://localhost:11434" }
            Configuracao.ollamaModel = txtOllamaModel.text.trim().ifEmpty { "moondream" }

            Configuracao.paddleCls = chkPaddleCls.isSelected
            Configuracao.paddleUseAngleCls = chkPaddleUseAngleCls.isSelected
            Configuracao.paddleLimitSideLen = spPaddleLimitSideLen.value

            Configuracao.saveProperties()
            PaddleOcrConfigApplier.applyFromConfiguracao()
            Configuracao.reload()
            Ocr.refreshConfiguration()

            Notificacoes.notificacao(Notificacao.SUCESSO, "Configurações", "Configurações salvas com sucesso!")
            onClose?.invoke()
        } catch (e: Exception) {
            Notificacoes.notificacao(Notificacao.ERRO, "Configurações", "Erro ao salvar configurações: ${e.message}")
        }
    }

    companion object {
        private val mLOG = LoggerFactory.getLogger(PopupConfiguracaoController::class.java)

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
