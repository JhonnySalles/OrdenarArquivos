package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupAmazon
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXDatePicker
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.sql.DriverManager
import java.time.LocalDate
import kotlin.test.assertNotNull

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class PopupAmazonUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    
    companion object {
        private var staticKeepAlive: java.sql.Connection? = null
        private lateinit var mockedAlertas: MockedStatic<AlertasPopup>
        private lateinit var mockedJsoup: MockedStatic<Jsoup>

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_amazon?mode=memory&cache=shared")
            DataBase.instancia
            
            mockedAlertas = Mockito.mockStatic(AlertasPopup::class.java)
            mockedJsoup = Mockito.mockStatic(Jsoup::class.java)
        }

        @AfterAll
        @JvmStatic
        fun globalTearDown() {
            mockedAlertas.close()
            mockedJsoup.close()
            staticKeepAlive?.close()
            staticKeepAlive = null
            DataBase.isTeste = false
        }
    }

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        val scene = Scene(root, 1024.0, 768.0)
        
        // Fix JFoenix skins
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix_amazon", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin"; }
                .jfx-date-picker { -fx-skin: "com.sun.javafx.scene.control.skin.DatePickerSkin"; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stage.scene = scene
        stage.show()
        
        robotOpenPopup()
    }

    private fun robotOpenPopup() {
        WaitForAsyncUtils.waitForFxEvents()
        val callback = javafx.util.Callback<ComicInfo, Boolean> { true }
        val comicInfo = ComicInfo().apply { series = "Initial Series" }
        
        javafx.application.Platform.runLater {
            PopupAmazon.abreTelaAmazon(
                mainController.rootStack,
                mainController.rootTab,
                callback,
                comicInfo,
                Linguagem.ENGLISH
            )
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testInitialState(robot: FxRobot) {
        val txtSerie = robot.lookup("#txtSerie").queryAs(JFXTextField::class.java)
        assertEquals("Initial Series", txtSerie.text)
        
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        assertEquals(Linguagem.ENGLISH, cbLinguagem.value)
    }

    @Test
    fun testExtractionAmazonUS(robot: FxRobot) {
        val doc = Document("https://www.amazon.com/dp/B000000000")
        doc.appendChild(Element("span").attr("id", "productTitle").text("Spiderman Vol 1"))
        
        val pubDate = Element("div").attr("id", "rpi-attribute-book_details-publication_date")
        pubDate.appendChild(Element("span").addClass("attribute-value").text("January 1, 2023"))
        doc.appendChild(pubDate)
        
        val publisher = Element("div").attr("id", "rpi-attribute-book_details-publisher")
        publisher.appendChild(Element("span").addClass("attribute-value").text("Marvel"))
        doc.appendChild(publisher)

        val mockConnection = Mockito.mock(Connection::class.java)
        mockedJsoup.`when`<Connection> { Jsoup.connect(anyString()) }.thenReturn(mockConnection)
        Mockito.`when`(mockConnection.userAgent(anyString())).thenReturn(mockConnection)
        Mockito.`when`(mockConnection.referrer(anyString())).thenReturn(mockConnection)
        Mockito.`when`(mockConnection.get()).thenReturn(doc)

        val txtSiteAmazon = robot.lookup("#txtSiteAmazon").queryAs(JFXTextField::class.java)
        robot.interact {
            txtSiteAmazon.text = "https://www.amazon.com/dp/B000000000"
        }
        
        // Trigger focus out to start consulta()
        robot.clickOn("#txtSerie") 
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Spiderman Vol 1", robot.lookup("#txtTitulo").queryAs(JFXTextField::class.java).text)
        assertEquals("Marvel", robot.lookup("#txtEditora").queryAs(JFXTextField::class.java).text)
        assertEquals(LocalDate.of(2023, 1, 1), robot.lookup("#dpPublicacao").queryAs(JFXDatePicker::class.java).value)
    }

    @Test
    fun testExtractionAmazonJP(robot: FxRobot) {
        val doc = Document("https://www.amazon.co.jp/dp/B000000000")
        doc.appendChild(Element("span").attr("id", "productTitle").text("One Piece Vol 100"))
        
        val pubDate = Element("div").attr("id", "rpi-attribute-book_details-publication_date")
        pubDate.appendChild(Element("span").addClass("attribute-value").text("2021/09/03"))
        doc.appendChild(pubDate)
        
        val details = Element("div").attr("id", "detailBullets_feature_div")
        val ul = Element("ul")
        ul.appendChild(Element("li").text("出版社 : 集英社 (2021/9/3)"))
        details.appendChild(ul)
        doc.appendChild(details)

        val mockConnection = Mockito.mock(Connection::class.java)
        mockedJsoup.`when`<Connection> { Jsoup.connect(anyString()) }.thenReturn(mockConnection)
        Mockito.`when`(mockConnection.userAgent(anyString())).thenReturn(mockConnection)
        Mockito.`when`(mockConnection.referrer(anyString())).thenReturn(mockConnection)
        Mockito.`when`(mockConnection.get()).thenReturn(doc)

        val txtSiteAmazon = robot.lookup("#txtSiteAmazon").queryAs(JFXTextField::class.java)
        robot.interact {
            txtSiteAmazon.text = "https://www.amazon.co.jp/dp/B000000000"
        }
        
        robot.clickOn("#txtSerie") 
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("One Piece Vol 100", robot.lookup("#txtTitulo").queryAs(JFXTextField::class.java).text)
        assertEquals("集英社", robot.lookup("#txtEditora").queryAs(JFXTextField::class.java).text)
        assertEquals(LocalDate.of(2021, 9, 3), robot.lookup("#dpPublicacao").queryAs(JFXDatePicker::class.java).value)
    }

    @Test
    fun testAplicarButton(robot: FxRobot) {
        val txtPublicacaoSite = robot.lookup("#txtPublicacaoSite").queryAs(JFXTextField::class.java)
        val dpPublicacao = robot.lookup("#dpPublicacao").queryAs(JFXDatePicker::class.java)
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)

        robot.interact {
            cbLinguagem.value = Linguagem.JAPANESE
            txtPublicacaoSite.text = "2024/05/20"
        }
        robot.clickOn("#btnAplicar")
        assertEquals(LocalDate.of(2024, 5, 20), dpPublicacao.value)
    }
    
    @Test
    fun testConfirmButton(robot: FxRobot) {
        val btnConfirmar = robot.lookup("Confirmar").queryAs(JFXButton::class.java)
        assertNotNull(btnConfirmar)
        robot.clickOn(btnConfirmar)
    }
}
