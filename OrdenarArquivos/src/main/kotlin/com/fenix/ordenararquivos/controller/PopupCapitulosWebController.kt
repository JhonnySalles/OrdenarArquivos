package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.util.Utils
import com.fenix.ordenararquivos.webview.WebViewSessionManager
import org.slf4j.LoggerFactory
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialogLayout
import com.jfoenix.controls.JFXTextField
import javafx.animation.Interpolator
import javafx.animation.RotateTransition
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.util.Duration
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.web.PopupFeatures
import javafx.scene.web.WebEngine
import javafx.scene.web.WebErrorEvent
import javafx.scene.web.WebEvent
import javafx.scene.web.WebView
import javafx.util.Callback
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign2.MaterialDesignR
import java.io.File
import java.net.URL
import java.util.*

class PopupCapitulosWebController : Initializable {

    @FXML
    private lateinit var spWebView: StackPane

    @FXML
    private lateinit var txtEndereco: JFXTextField

    @FXML
    private lateinit var btnAtualizar: JFXButton

    @FXML
    private lateinit var hplComickIO: Hyperlink

    @FXML
    private lateinit var hplComickFan: Hyperlink

    @FXML
    private lateinit var hplTaiyo: Hyperlink

    @FXML
    private lateinit var hplMangaFire: Hyperlink

    @FXML
    private lateinit var hplMangaRead: Hyperlink

    @FXML
    private lateinit var hplMangaDex: Hyperlink

    @FXML
    private lateinit var hplMangak: Hyperlink

    @FXML
    private lateinit var hplMangaPark: Hyperlink

    @FXML
    private lateinit var hplMangaKatana: Hyperlink

    @FXML
    private lateinit var hplVyManga: Hyperlink

    @FXML
    private lateinit var hplMangaTown1: Hyperlink

    @FXML
    private lateinit var hplMangaTown2: Hyperlink

    @FXML
    private lateinit var hplMangaHere: Hyperlink

    @FXML
    private lateinit var hplKMangaKodansha: Hyperlink

    @FXML
    private lateinit var hplZazaManga: Hyperlink

    private var sincronizandoEndereco = false
    private var ultimaUrlCarregada: String? = null
    private lateinit var webView: WebView
    private lateinit var siteLinks: Array<Hyperlink>
    private lateinit var btnAtualizarIcon: FontIcon
    private var rotateCarregamento: RotateTransition? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        btnAtualizarIcon = FontIcon(MaterialDesignR.REFRESH).apply {
            iconSize = 17
            iconColor = Color.WHITE
        }
        btnAtualizar.graphic = btnAtualizarIcon
        rotateCarregamento = RotateTransition(Duration.seconds(0.8), btnAtualizarIcon).apply {
            byAngle = 360.0
            cycleCount = RotateTransition.INDEFINITE
            interpolator = Interpolator.LINEAR
        }

        instanciaAtiva = this
        webView = obterWebViewCompartilhado()
        anexarWebViewAoContainer()

        siteLinks = arrayOf(
            hplComickIO, hplComickFan, hplTaiyo, hplMangaDex, hplMangaFire, hplMangaRead,
            hplMangak, hplMangaPark, hplZazaManga, hplMangaKatana, hplVyManga, hplMangaTown1, hplMangaTown2,
            hplMangaHere, hplKMangaKodansha
        )
        for (link in siteLinks) {
            link.setOnAction {
                txtEndereco.text = link.text
                carregarEndereco()
            }
        }
    }

    private fun anexarWebViewAoContainer() {
        val parent = webView.parent
        if (parent is StackPane)
            parent.children.remove(webView)

        spWebView.children.add(0, webView)
        StackPane.setAlignment(webView, Pos.CENTER)
        VBox.setVgrow(webView, Priority.ALWAYS)
    }

    private fun iniciarIndicadorCarregamento() {
        if (::btnAtualizarIcon.isInitialized)
            rotateCarregamento?.play()
    }

    private fun pararIndicadorCarregamento() {
        rotateCarregamento?.stop()
        if (::btnAtualizarIcon.isInitialized)
            btnAtualizarIcon.rotate = 0.0
    }

    private fun onLoadWorkerStateChanged(oldState: Worker.State, newState: Worker.State) {
        when (newState) {
            Worker.State.RUNNING, Worker.State.SCHEDULED -> iniciarIndicadorCarregamento()
            Worker.State.SUCCEEDED, Worker.State.FAILED, Worker.State.CANCELLED -> {
                pararIndicadorCarregamento()
                if (newState == Worker.State.FAILED && oldState == Worker.State.RUNNING) {
                    val engine = webView.engine
                    val ex = engine.loadWorker.exception
                    mLOG.warn(
                        "Falha ao carregar WebView: url={}, erro={}",
                        engine.location,
                        ex?.message,
                        ex
                    )
                    val detalhe = ex?.message?.let { "\n\n$it" } ?: ""
                    AlertasModal.erro(
                        "Erro ao carregar",
                        "Não foi possível carregar a página. Verifique a conexão, bloqueio anti-bot do site ou tente atualizar (botão refresh).$detalhe"
                    )
                }
            }
            else -> {}
        }
    }

    private fun onLocationChanged(newLocation: String?) {
        if (!sincronizandoEndereco && !newLocation.isNullOrBlank() && newLocation != "about:blank") {
            txtEndereco.text = newLocation
        }
    }

    fun setEnderecoInicial(endereco: String) {
        txtEndereco.text = endereco
    }

    @FXML
    private fun onBtnAtualizar() {
        carregarEndereco(forcar = true)
    }

    fun carregarEndereco(forcar: Boolean = false) {
        val url = txtEndereco.text.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            return

        val worker = webView.engine.loadWorker
        if (deveIgnorarCarga(url, ultimaUrlCarregada, worker.state, forcar))
            return

        ultimaUrlCarregada = url
        sincronizandoEndereco = true
        webView.engine.load(url)
        sincronizandoEndereco = false
    }

    fun obterConteudoParaExtracao(): Pair<String, String> {
        val site = webView.engine.location
            .takeIf { it.startsWith("http") }
            ?: txtEndereco.text.trim()
        val script = montarScriptExtracaoHtml(seletoresPorDominio(site))
        val html = webView.engine.executeScript(script) as String
        return site to html
    }

    fun liberarPagina() {
        pararIndicadorCarregamento()
        if (instanciaAtiva === this)
            instanciaAtiva = null
        ultimaUrlCarregada = null
        webView.engine.load("about:blank")
    }

    companion object {
        private val mLOG = LoggerFactory.getLogger(PopupCapitulosWebController::class.java)

        private val STYLE_SHEET: String =
            PopupCapitulosWebController::class.java.getResource("/css/Dark_TelaInicial.css").toExternalForm()

        private var sharedWebView: WebView? = null
        private var listenersRegistrados = false
        private var instanciaAtiva: PopupCapitulosWebController? = null

        fun inicializarMotorWeb() {
            obterWebViewCompartilhado()
        }

        internal fun obterWebViewCompartilhado(): WebView {
            if (sharedWebView == null) {
                sharedWebView = WebView().apply {
                    minHeight = 200.0
                    isContextMenuEnabled = false
                }
                configurarWebEngine(sharedWebView!!.engine)
                registrarListenersGlobais(sharedWebView!!.engine)
                aquecerWebEngine()
            }
            return sharedWebView!!
        }

        private fun configurarWebEngine(engine: WebEngine) {
            val profileDir = WebViewSessionManager.inicializar()
            engine.userDataDirectory = profileDir
            engine.userAgent = Utils.WEBVIEW_USER_AGENT
            engine.isJavaScriptEnabled = true

            engine.onError = javafx.event.EventHandler { event: WebErrorEvent ->
                mLOG.warn("WebEngine erro ({}): {}", event.eventType, event.message, event.exception)
                if (event.eventType == WebErrorEvent.USER_DATA_DIRECTORY_ALREADY_IN_USE) {
                    val fallback = File(profileDir.parentFile, "profile_${System.currentTimeMillis()}")
                    fallback.mkdirs()
                    engine.userDataDirectory = fallback
                    mLOG.info("WebEngine usando profile alternativo: {}", fallback.absolutePath)
                }
            }

            engine.onAlert = javafx.event.EventHandler { event: WebEvent<String> ->
                mLOG.debug("WebView alert: {}", event.data)
            }

            engine.confirmHandler = Callback { message ->
                mLOG.debug("WebView confirm (auto-aceito): {}", message)
                true
            }

            engine.promptHandler = Callback { data ->
                mLOG.debug("WebView prompt (resposta vazia): {}", data.message)
                data.defaultValue ?: ""
            }

            engine.createPopupHandler = Callback { _: PopupFeatures ->
                mLOG.debug("WebView popup aberto no mesmo motor")
                engine
            }
        }

        private fun registrarListenersGlobais(engine: WebEngine) {
            if (listenersRegistrados) return
            listenersRegistrados = true
            engine.loadWorker.stateProperty().addListener { _, oldState, newState ->
                instanciaAtiva?.onLoadWorkerStateChanged(oldState, newState)
            }
            engine.locationProperty().addListener { _, _, newLocation ->
                instanciaAtiva?.onLocationChanged(newLocation)
            }
        }

        internal fun aquecerWebEngine() {
            Platform.runLater {
                sharedWebView?.engine?.takeIf { it.location.isNullOrBlank() || it.location == "about:blank" }
                    ?.load("about:blank")
            }
        }

        internal fun deveIgnorarCarga(
            url: String,
            ultimaUrl: String?,
            loadState: Worker.State,
            forcar: Boolean
        ): Boolean {
            if (forcar) return false
            if (loadState == Worker.State.RUNNING || loadState == Worker.State.SCHEDULED) return true
            return url == ultimaUrl && loadState == Worker.State.SUCCEEDED
        }

        internal fun seletoresPorDominio(site: String): List<String> {
            val s = site.lowercase()
            return when {
                s.contains("mangaplanet.com") -> listOf(
                    "div[id^=accordion_]",
                    "div.card.mt-4.select-options"
                )
                s.contains("comick.io") || s.contains("comickfan.com") -> listOf(
                    "table tbody",
                    "div.group"
                )
                s.contains("mangafire.to") -> listOf(
                    "div.flex.flex-col.gap-1",
                    "div[data-open=true]"
                )
                s.contains("taiyo.moe") -> listOf("li.item", "ul.list")
                s.contains("mangapark.net") -> listOf(
                    "li.wp-manga-chapter",
                    "#manga-chapters-holder"
                )
                s.contains("mangaread.org") -> listOf(
                    "select.chapter-select",
                    "div.chapter-list"
                )
                s.contains("mangak.io") -> listOf("a[data-chapter-row=true]")
                s.contains("mangakatana.com") -> listOf("div.chapters", "div.chapter-list")
                s.contains("mangadex.org") -> listOf(
                    ".chapter-list",
                    ".chapter-header",
                    "[data-chapter-id]"
                )
                else -> emptyList()
            }
        }

        internal fun montarScriptExtracaoHtml(selectors: List<String>): String {
            if (selectors.isEmpty())
                return "document.documentElement.outerHTML"

            val selectorsJs = selectors.joinToString(",") { "'${it.replace("'", "\\'")}'" }
            return """
                (function() {
                    var selectors = [$selectorsJs];
                    for (var i = 0; i < selectors.length; i++) {
                        var el = document.querySelector(selectors[i]);
                        if (el) return el.outerHTML;
                    }
                    return document.documentElement.outerHTML;
                })();
            """.trimIndent()
        }

        internal fun restaurarEstadoDialog(nodeBlur: Node) {
            nodeBlur.effect = null
            nodeBlur.isDisable = false
        }

        fun abreTelaWeb(
            stackPane: StackPane,
            nodeBlur: Node,
            enderecoInicial: String,
            onConfirm: (site: String, html: String) -> Unit
        ) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                val loader = FXMLLoader(PopupCapitulosWebController::class.java.getResource("/view/PopupCapitulosWeb.fxml"))
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupCapitulosWebController = loader.getController()
                val subDialog = JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)

                subDialog.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                    if (event.code == KeyCode.ESCAPE) {
                        cnt.liberarPagina()
                        subDialog.close()
                        event.consume()
                    }
                }

                cnt.setEnderecoInicial(enderecoInicial)

                val titulo = Label("Navegar e importar capítulos")
                titulo.font = Font.font(20.0)
                titulo.textFill = Color.WHITE
                val hbTitulo = HBox(titulo)
                hbTitulo.alignment = Pos.CENTER
                hbTitulo.maxWidth = Double.MAX_VALUE

                dialogLayout.setHeading(hbTitulo)
                dialogLayout.setBody(newAnchorPane)

                val btnVoltar = JFXButton("Voltar")
                btnVoltar.styleClass.add("background-White1")
                btnVoltar.setOnAction {
                    cnt.liberarPagina()
                    subDialog.close()
                }

                val btnConfirmar = JFXButton("Confirmar")
                btnConfirmar.styleClass.addAll("background-Green2", "texto-stilo-1")
                btnConfirmar.setOnAction {
                    val (site, html) = cnt.obterConteudoParaExtracao()
                    onConfirm(site, html)
                    cnt.liberarPagina()
                    subDialog.close()
                }

                dialogLayout.setActions(listOf(btnVoltar, btnConfirmar))
                dialogLayout.styleClass.add("dialog-black")
                subDialog.stylesheets.add(STYLE_SHEET)

                subDialog.setOnDialogClosed {
                    cnt.liberarPagina()
                    restaurarEstadoDialog(nodeBlur)
                }

                nodeBlur.effect = blur
                nodeBlur.isDisable = true

                subDialog.show()

                if (enderecoInicial.trim().startsWith("http"))
                    Platform.runLater { cnt.carregarEndereco() }
            } catch (e: Exception) {
                restaurarEstadoDialog(nodeBlur)
                AlertasModal.erro("Erro ao abrir navegador", e.message ?: e.toString())
            }
        }
    }
}
