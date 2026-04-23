package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupAmazon
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupAmazonUiTest : BaseTest() {

    private lateinit var popupController: PopupAmazon
    private lateinit var mockTelaInicialController: TelaInicialController
    private var mockedJsoup: MockedStatic<Jsoup>? = null

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

        stage.scene = Scene(root)
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        robot.interact {
            popupController.objeto = ComicInfo()
            popupController.setLinguagem(Linguagem.ENGLISH)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        // Reset AlertasPopup
        com.fenix.ordenararquivos.notification.AlertasPopup.isTeste = true
        com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText = null
    }

    @AfterEach
    fun tearDown(robot: FxRobot) {
        robot.interact {
            mockedJsoup?.close()
            mockedJsoup = null
        }
    }

    private fun setupMockJsoup(robot: FxRobot, htmlFileName: String): Document {
        val htmlFile = File("src/test/resources/fixtures/$htmlFileName")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        
        val mockConnection = mock<Connection>()
        
        // CRITICAL: Static mocks in Mockito are thread-local. 
        // We must create and configure the mock on the JavaFX Application Thread 
        // so that the controller's listener (running on the same thread) can see it.
        robot.interact {
            mockedJsoup = Mockito.mockStatic(Jsoup::class.java)
            mockedJsoup!!.`when`<Connection> { Jsoup.connect(anyString()) }.thenReturn(mockConnection)
        }
        
        whenever(mockConnection.userAgent(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.referrer(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.get()).thenReturn(doc)
        
        return doc
    }

    @Test
    fun testAmazonScrapingEn(robot: FxRobot) {
        setupMockJsoup(robot, "amazon_en.html")

        val txtSite = robot.lookup("#txtSiteAmazon").queryAs(JFXTextField::class.java)
        val txtTitulo = robot.lookup("#txtTitulo").queryAs(JFXTextField::class.java)
        val txtEditora = robot.lookup("#txtEditora").queryAs(JFXTextField::class.java)
        val dpPublicacao = robot.lookup("#dpPublicacao").queryAs(com.jfoenix.controls.JFXDatePicker::class.java)

        robot.clickOn(txtSite)
        robot.interact { txtSite.text = "https://www.amazon.com/example/dp/B000000000" }
        robot.clickOn(txtTitulo)

        // Aguardar scraping
        WaitForAsyncUtils.waitForFxEvents()
        
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { 
            txtTitulo.text == "Manga Title EN" || com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText != null
        }

        assertNull(com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText, "Should not have error: ${com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText}")
        assertEquals("Manga Title EN", txtTitulo.text)
        assertEquals("Publisher EN", txtEditora.text)
        assertEquals("2024-01-01", dpPublicacao.value.toString())
    }

    @Test
    fun testAmazonScrapingJp(robot: FxRobot) {
        setupMockJsoup(robot, "amazon_jp.html")

        val txtSite = robot.lookup("#txtSiteAmazon").queryAs(JFXTextField::class.java)
        val txtTitulo = robot.lookup("#txtTitulo").queryAs(JFXTextField::class.java)

        robot.clickOn(txtSite)
        robot.interact { txtSite.text = "https://www.amazon.co.jp/example/dp/B000000000" }
        robot.clickOn(txtTitulo)

        // Aguardar scraping
        WaitForAsyncUtils.waitForFxEvents()

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { 
            txtTitulo.text == "Manga Title JP" || com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText != null
        }

        assertNull(com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText, "Should not have error: ${com.fenix.ordenararquivos.notification.AlertasPopup.lastAlertText}")
        assertEquals("Manga Title JP", txtTitulo.text)
    }

    @Test
    fun testLanguageSelectionChange(robot: FxRobot) {
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(com.jfoenix.controls.JFXComboBox::class.java)
        
        robot.interact {
            popupController.setLinguagem(Linguagem.JAPANESE)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals(Linguagem.JAPANESE, cbLinguagem.value)
    }
    
    private fun assertNull(actual: Any?, message: String) {
        if (actual != null) {
            throw org.opentest4j.AssertionFailedError(message)
        }
    }
}
