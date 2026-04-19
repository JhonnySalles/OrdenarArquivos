package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupAmazon
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXTabPane
import java.io.File
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
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

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupAmazonUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var popupController: PopupAmazon
    private lateinit var mockJsoup: MockedStatic<Jsoup>
    private lateinit var mockConnection: Connection
    private lateinit var mockDocument: Document

    private lateinit var mockTelaInicialController: TelaInicialController

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()
        whenever(mockTelaInicialController.rootStack).thenReturn(StackPane())
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())

        val loader = FXMLLoader(PopupAmazon.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == PopupAmazon::class.java) {
                PopupAmazon().also { popupController = it }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        val root: AnchorPane = loader.load()
        // PopupAmazon does not have controllerPai

        mockJsoup = Mockito.mockStatic(Jsoup::class.java)
        mockConnection = mock<Connection>()
        mockDocument = mock<Document>()

        whenever(Jsoup.connect(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.userAgent(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.referrer(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.get()).thenReturn(mockDocument)

        stage.scene = Scene(root)
        stage.scene.stylesheets.add(TelaInicialController::class.java.getResource("/css/jfoenix-components-fix.css")?.toExternalForm())
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @AfterEach
    fun tearDown() {
        if (::mockJsoup.isInitialized) {
            mockJsoup.close()
        }
    }

    private fun openPopupAmazon(robot: FxRobot) {
        robot.interact {
            popupController.objeto = ComicInfo()
            popupController.setLinguagem(Linguagem.ENGLISH)
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testAmazonScrapingEn(robot: FxRobot) {
        openPopupAmazon(robot)

        val txtSite = robot.lookup("#txtSiteAmazon").queryAs(JFXTextField::class.java)
        val txtTitulo = robot.lookup("#txtTitulo").queryAs(JFXTextField::class.java)
        val txtEditora = robot.lookup("#txtEditora").queryAs(JFXTextField::class.java)
        val dpPublicacao =
                robot.lookup("#dpPublicacao").queryAs(javafx.scene.control.DatePicker::class.java)

        // Carregar fixture real
        val htmlFile = File("src/test/resources/fixtures/amazon_en.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        whenever(mockConnection.get()).thenReturn(doc)

        robot.interact {
            txtSite.text = "https://www.amazon.com/example/dp/B000000000"
            // Trigger focus lost
            txtSite.parent.requestFocus()
        }

        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Manga Title EN", txtTitulo.text)
        assertEquals("Publisher EN", txtEditora.text)
        assertEquals("2024-01-01", dpPublicacao.value.toString())
    }

    @Test
    fun testAmazonScrapingJp(robot: FxRobot) {
        // Abrir popup com linguagem Japonesa
        robot.interact {
            popupController.objeto = ComicInfo()
            popupController.setLinguagem(Linguagem.JAPANESE)
        }
        WaitForAsyncUtils.waitForFxEvents()

        val txtSite = robot.lookup("#txtSiteAmazon").queryAs(JFXTextField::class.java)
        val txtTitulo = robot.lookup("#txtTitulo").queryAs(JFXTextField::class.java)
        val dpPublicacao =
                robot.lookup("#dpPublicacao").queryAs(javafx.scene.control.DatePicker::class.java)

        // Carregar fixture Japonesa
        val htmlFile = File("src/test/resources/fixtures/amazon_jp.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        whenever(mockConnection.get()).thenReturn(doc)

        robot.interact {
            txtSite.text = "https://www.amazon.co.jp/example/dp/B000000000"
            txtSite.parent.requestFocus()
        }

        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Manga Title JP", txtTitulo.text)
        assertEquals("2024-01-01", dpPublicacao.value.toString())
    }

    @Test
    fun testLanguageSelectionChange(robot: FxRobot) {
        openPopupAmazon(robot)
        val cbLinguagem =
                robot.lookup("#cbLinguagem")
                        .queryAs(com.jfoenix.controls.JFXComboBox::class.java) as
                        com.jfoenix.controls.JFXComboBox<Linguagem>

        robot.interact { cbLinguagem.selectionModel.select(Linguagem.JAPANESE) }

        assertEquals(Linguagem.JAPANESE, cbLinguagem.selectionModel.selectedItem)
    }
}
