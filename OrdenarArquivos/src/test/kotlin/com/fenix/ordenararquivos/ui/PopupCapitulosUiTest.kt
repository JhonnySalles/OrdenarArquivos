package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupCapitulos
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXTabPane
import java.io.File
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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupCapitulosUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var popupController: PopupCapitulos
    private lateinit var mockJsoup: MockedStatic<Jsoup>
    private lateinit var mockConnection: Connection
    private lateinit var mockDocument: Document

    private lateinit var mockTelaInicialController: TelaInicialController

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()
        whenever(mockTelaInicialController.rootStack).thenReturn(StackPane())
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())

        val loader = FXMLLoader(PopupCapitulos.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == PopupCapitulos::class.java) {
                PopupCapitulos().also { popupController = it }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        val root: AnchorPane = loader.load()
        // PopupCapitulos does not have controllerPai

        stage.scene = Scene(root)
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        // Mocking Jsoup
        mockJsoup = Mockito.mockStatic(Jsoup::class.java)
        mockConnection = mock<Connection>()
        mockDocument = mock<Document>()

        whenever(Jsoup.connect(anyString())).thenReturn(mockConnection)
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
        // O popup de capítulos agora é carregado isoladamente no @Start.
        WaitForAsyncUtils.waitForFxEvents()
        assertNotNull(robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java))
    }

    @Test
    fun testExecutarScrapingMangaPlanet(robot: FxRobot) {
        testAbriPopupCapitulos(robot)

        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)

        // Carregar fixture real
        val htmlFile = File("src/test/resources/fixtures/mangaplanet.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        whenever(mockConnection.get()).thenReturn(doc)

        robot.interact { txtEndereco.text = "https://mangaplanet.com/comic/example" }

        robot.clickOn(btnExecutar)
        WaitForAsyncUtils.waitForFxEvents()

        // Verificar se a tabela foi populada (a fixture mangaplanet.html tem capítulos)
        assertTrue(tbViewTabela.items.size > 0, "A tabela de capítulos deveria estar populada")

        // Opcional: verificar se o primeiro volume tem capítulos
        val primeiroVol =
                tbViewTabela.items[0] as com.fenix.ordenararquivos.model.entities.capitulos.Volume
        assertFalse(
                primeiroVol.capitulos.isEmpty(),
                "O primeiro volume deveria ter capítulos extraídos"
        )
    }

    @Test
    fun testExecutarScrapingComick(robot: FxRobot) {
        testAbriPopupCapitulos(robot)

        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)

        // Carregar fixture real
        val htmlFile = File("src/test/resources/fixtures/comick.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        whenever(mockConnection.get()).thenReturn(doc)

        robot.interact { txtEndereco.text = "https://comick.app/comic/example" }

        robot.interact { btnExecutar.fire() }
        
        // Aguardar o modelo de dados ser populado
        WaitForAsyncUtils.waitForFxEvents()
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            tbViewTabela.items.isNotEmpty()
        }

        assertTrue(
                tbViewTabela.items.size > 0,
                "A tabela de capítulos deveria estar populada (Comick)"
        )
    }

    @Test
    fun testValidacaoEnderecoVazio(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)

        robot.interact { robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java).text = "" }

        robot.clickOn(btnExecutar)
        WaitForAsyncUtils.waitForFxEvents()

        // Não deve ter chamado Jsoup
        mockJsoup.verify(MockedStatic.Verification { Jsoup.connect(anyString()) }, never())
    }
}
