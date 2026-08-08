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
    private lateinit var cbGeminiKey: JFXComboBox<String>

    @FXML
    private lateinit var boxOpenRouter: VBox

    @FXML
    private lateinit var cbOpenRouterModel: JFXComboBox<String>

    @FXML
    private lateinit var boxOcrSpace: VBox

    @FXML
    private lateinit var boxOptiic: VBox

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

        cbGeminiModel.isEditable = true
        cbGeminiKey.items = FXCollections.observableArrayList("Key 1", "Key 2")

        cbOpenRouterModel.isEditable = true

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
        cbGeminiModel.value = currentModel
        cbGeminiKey.value = Configuracao.geminiKeySelecionada
        carregarModelosGemini()

        val currentOpenRouterModel = Configuracao.openrouterModel
        cbOpenRouterModel.value = currentOpenRouterModel
        carregarModelosOpenRouter()

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
        boxOpenRouter.opacity = if (engine == OcrEngine.OPENROUTER) 1.0 else 0.55
        boxOcrSpace.opacity = if (engine == OcrEngine.OCR_SPACE) 1.0 else 0.55
        boxOptiic.opacity = if (engine == OcrEngine.OPTIIC) 1.0 else 0.55
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

            val selectedModel = cbGeminiModel.editor.text.takeIf { !it.isNullOrBlank() } ?: cbGeminiModel.value ?: "gemini-2.0-flash"
            Configuracao.geminiModel = selectedModel
            Configuracao.geminiKeySelecionada = cbGeminiKey.value ?: "Key 1"

            val selectedOpenRouterModel = cbOpenRouterModel.editor.text.takeIf { !it.isNullOrBlank() } ?: cbOpenRouterModel.value ?: ""
            Configuracao.openrouterModel = selectedOpenRouterModel

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

    private fun carregarModelosGemini() {
        val client = com.squareup.okhttp.OkHttpClient()
        val keysToTry = listOf(Configuracao.geminiKey1, Configuracao.geminiKey2).filter { it.isNotEmpty() }
        
        if (keysToTry.isEmpty()) {
            return
        }

        java.util.concurrent.CompletableFuture.runAsync {
            var responseData: String? = null
            for (key in keysToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
                    val request = com.squareup.okhttp.Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful && response.body() != null) {
                        responseData = response.body().string()
                    }
                    response.body()?.close()
                } catch (e: Exception) {
                    mLOG.error("Erro ao listar modelos do Gemini com a chave: $e")
                }
                if (responseData != null) break
            }

            if (responseData != null) {
                try {
                    val json = org.json.JSONObject(responseData!!)
                    val modelsArray = json.getJSONArray("models")
                    val modelList = mutableListOf<String>()
                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.getJSONObject(i)
                        val name = m.getString("name")
                        val description = m.optString("description", "")
                        val methods = m.getJSONArray("supportedGenerationMethods")
                        val supportsGenerate = (0 until methods.length()).any { j ->
                            methods.getString(j) == "generateContent"
                        }
                        val isMultimodal = (name.contains("gemini") && !name.contains("gemini-1.0-pro")) ||
                                description.contains("multimodal", ignoreCase = true)
                        if (supportsGenerate && isMultimodal) {
                            val shortName = if (name.startsWith("models/")) name.substring("models/".length) else name
                            modelList.add(shortName)
                        }
                    }
                    modelList.sort()
                    javafx.application.Platform.runLater {
                        val editorText = cbGeminiModel.editor.text
                        val currentVal = if (editorText != null && editorText.isNotBlank()) editorText else cbGeminiModel.value
                        cbGeminiModel.items = javafx.collections.FXCollections.observableArrayList(modelList)
                        if (currentVal != null) {
                            cbGeminiModel.value = currentVal
                        }
                    }
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar resposta de modelos do Gemini: $e")
                }
            }
        }
    }

    private fun carregarModelosOpenRouter() {
        val client = com.squareup.okhttp.OkHttpClient()
        val apiKey = Configuracao.openrouterApiKey

        java.util.concurrent.CompletableFuture.runAsync {
            var responseData: String? = null
            try {
                val url = "https://openrouter.ai/api/v1/models"
                val requestBuilder = com.squareup.okhttp.Request.Builder()
                    .url(url)
                    .get()
                if (apiKey.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
                val response = client.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful && response.body() != null) {
                    responseData = response.body().string()
                }
                response.body()?.close()
            } catch (e: Exception) {
                mLOG.error("Erro ao listar modelos do OpenRouter: $e")
            }

            if (responseData != null) {
                try {
                    val json = org.json.JSONObject(responseData)
                    val dataArray = json.getJSONArray("data")
                    val modelList = mutableListOf<String>()
                    for (i in 0 until dataArray.length()) {
                        val m = dataArray.getJSONObject(i)
                        val id = m.getString("id")

                        // Verificar pricing
                        val pricing = m.optJSONObject("pricing")
                        val isFree = if (pricing != null) {
                            val promptCost = pricing.optString("prompt", "0").toDoubleOrNull() ?: 0.0
                            val completionCost = pricing.optString("completion", "0").toDoubleOrNull() ?: 0.0
                            promptCost == 0.0 && completionCost == 0.0
                        } else false

                        // Verificar modalidade de visão (input_modalities contém "image" ou modality contém "image")
                        val architecture = m.optJSONObject("architecture")
                        val supportsVision = if (architecture != null) {
                            val modality = architecture.optString("modality", "")
                            val inputModalities = architecture.optJSONArray("input_modalities")
                            val hasImageInInput = if (inputModalities != null) {
                                (0 until inputModalities.length()).any { idx ->
                                    inputModalities.getString(idx) == "image"
                                }
                            } else false
                            modality.contains("image", ignoreCase = true) || hasImageInInput
                        } else false

                        if (isFree && supportsVision) {
                            modelList.add(id)
                        }
                    }
                    modelList.sort()
                    javafx.application.Platform.runLater {
                        val editorText = cbOpenRouterModel.editor.text
                        val currentVal = if (editorText != null && editorText.isNotBlank()) editorText else cbOpenRouterModel.value
                        cbOpenRouterModel.items = javafx.collections.FXCollections.observableArrayList(modelList)
                        if (currentVal != null && currentVal.isNotBlank() && modelList.contains(currentVal)) {
                            cbOpenRouterModel.value = currentVal
                        } else if (modelList.isNotEmpty()) {
                            cbOpenRouterModel.value = modelList.first()
                        }
                    }
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar resposta de modelos do OpenRouter: $e")
                }
            }
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
