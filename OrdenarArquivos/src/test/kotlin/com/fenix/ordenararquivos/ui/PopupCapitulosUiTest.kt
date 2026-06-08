package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupCapitulosController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.io.File
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupCapitulosUiTest : BaseTest() {

    private lateinit var popupController: PopupCapitulosController
    private lateinit var mockJsoup: MockedStatic<Jsoup>
    private lateinit var mockConnection: Connection
    private lateinit var mockDocument: Document
    private lateinit var mockTelaInicialController: TelaInicialController

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()
        whenever(mockTelaInicialController.rootStack).thenReturn(StackPane())
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())

        val loader = FXMLLoader(PopupCapitulosController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == PopupCapitulosController::class.java) {
                PopupCapitulosController().also { popupController = it }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        val root: StackPane = loader.load()

        stage.scene = Scene(root)
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        com.fenix.ordenararquivos.notification.AlertasModal.isTeste = true
        com.fenix.ordenararquivos.notification.AlertasModal.lastAlertText = null
 
        mockJsoup = Mockito.mockStatic(Jsoup::class.java, Answers.CALLS_REAL_METHODS)
        mockConnection = mock<Connection>()
        mockDocument = mock<Document>()

        mockJsoup.`when`<Connection> { Jsoup.connect(anyString()) }.thenReturn(mockConnection)
        whenever(mockConnection.userAgent(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.referrer(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.get()).thenReturn(mockDocument)
    }

    @AfterEach
    fun tearDown() {
        if (::mockJsoup.isInitialized) {
            mockJsoup.close()
        }
    }

    @Test
    fun testAbriPopupCapitulos(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        assertNotNull(robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java))
    }

    private fun executaScraping(robot: FxRobot, fixturePath: String, arquivos: List<String> = listOf("Manga Chapter 01.zip")) {
        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java) as TableView<Volume>

        // Garantir que temos ao menos um arquivo para o mapeamento automático funcionar
        robot.interact { popupController.setArquivos(arquivos) }

        val file = File(fixturePath)
        assertTrue(file.exists(), "Fixture $fixturePath não encontrada")

        robot.interact { txtEndereco.text = file.absolutePath }
        robot.interact { btnExecutar.fire() }
        
        WaitForAsyncUtils.waitForFxEvents()
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) {
            tbViewTabela.items.isNotEmpty()
        }

        assertTrue(tbViewTabela.items.size > 0, "Tabela deve conter itens após scraping de $fixturePath")
    }

    @Test
    fun testExecutarScrapingMangaPlanet(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/mangaplanet.html")
    }

    @Test
    fun testExecutarScrapingComick(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/comick.html")
    }

    @Test
    fun testExecutarScrapingMangaFire(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/mangafire.html")
    }

    @Test
    fun testExecutarScrapingTayo(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/taiyo.html")
    }

    @Test
    fun testExecutarScrapingMangaPark(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/mangapark.html")
    }

    @Test
    fun testExecutarScrapingMangaForest(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/mangaforest.html")
    }

    @Test
    fun testExecutarScrapingMangaRead(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/mangaread.html")
    }

    @Test
    fun testExecutarScrapingMangaDex(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/mangadex.html")
    }

    @Test
    fun testExecutarScrapingZBato(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        executaScraping(robot, "src/test/resources/fixtures/zbato.html")
    }

    @Test
    fun testMarcarTodos(robot: FxRobot) {
        testExecutarScrapingMangaPlanet(robot)
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java) as TableView<Volume>
        val cbMarcarTodos = robot.lookup("#cbMarcarTodos").queryAs(com.jfoenix.controls.JFXCheckBox::class.java)

        // Inicialmente, todos os itens vieram marcados (true), então cbMarcarTodos deve estar selecionado
        assertTrue(cbMarcarTodos.isSelected)

        // Desmarcar todos clicando
        robot.clickOn(cbMarcarTodos)
        WaitForAsyncUtils.waitForFxEvents()
        assertFalse(cbMarcarTodos.isSelected)
        tbViewTabela.items.forEach { assertFalse(it.marcado, "Item deveria estar desmarcado") }

        // Marcar todos clicando novamente
        robot.clickOn(cbMarcarTodos)
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(cbMarcarTodos.isSelected)
        tbViewTabela.items.forEach { assertTrue(it.marcado, "Item deveria estar marcado") }
    }

    @Test
    fun testMapeamentoArquivos(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java) as TableView<Volume>

        val arquivos = listOf("Manga Volume 01.zip", "Manga Volume 02.zip")
        executaScraping(robot, "src/test/resources/fixtures/mangaplanet.html", arquivos)
        
        val vol1 = tbViewTabela.items.find { it.volume == 1.0 }
        assertNotNull(vol1)
        assertEquals("Manga Volume 01.zip", vol1?.arquivo)
    }

    @Test
    fun testValidacaoEnderecoVazio(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)

        robot.interact { robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java).text = "" }
        robot.clickOn(btnExecutar)
        WaitForAsyncUtils.waitForFxEvents()

        mockJsoup.verify({ Jsoup.connect(anyString()) }, never())
    }
}
