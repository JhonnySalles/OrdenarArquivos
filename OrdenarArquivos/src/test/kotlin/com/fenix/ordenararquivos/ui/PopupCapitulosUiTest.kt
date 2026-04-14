package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupCapitulos
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
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
import org.mockito.MockedStatic
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupCapitulosUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var popupController: PopupCapitulos
    private lateinit var mockJsoup: MockedStatic<Jsoup>
    private lateinit var mockConnection: Connection
    private lateinit var mockDocument: Document

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            when (controllerClass) {
                TelaInicialController::class.java -> TelaInicialController().also { mainController = it }
                else -> controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        val root: AnchorPane = loader.load()
        
        // Mocking Jsoup before triggering initialization that might use it
        mockJsoup = Mockito.mockStatic(Jsoup::class.java)
        mockConnection = mock<Connection>()
        mockDocument = mock<Document>()
        
        whenever(Jsoup.connect(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.userAgent(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.referrer(anyString())).thenReturn(mockConnection)
        whenever(mockConnection.get()).thenReturn(mockDocument)

        stage.scene = Scene(root)
        stage.show()
    }

    @AfterEach
    fun tearDown() {
        if (::mockJsoup.isInitialized) {
            mockJsoup.close()
        }
    }

    @Test
    fun testAbriPopupCapitulos(robot: FxRobot) {
        // O popup de capítulos é aberto a partir da Aba Arquivo ou Aba Pastas.
        // Simulando abertura manual para testar o controlador isolado ou via gatilho
        
        robot.interact {
            val loader = FXMLLoader(PopupCapitulos::class.java.getResource("/view/PopupCapitulos.fxml"))
            val parent = loader.load<javafx.scene.Parent>()
            popupController = loader.getController()
            
            val dialog = com.jfoenix.controls.JFXDialog(mainController.rootStack, parent as javafx.scene.layout.Region, com.jfoenix.controls.JFXDialog.DialogTransition.CENTER)
            dialog.show()
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        assertNotNull(robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java))
    }

    @Test
    fun testExecutarScrapingMock(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        
        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)
        
        robot.interact {
            txtEndereco.text = "https://mangadex.org/title/example"
        }
        
        // Simular Documento HTML vindo do Jsoup para Mangadex
        // (Nota: em um teste real, precisaríamos de um HTML real mapeado)
        // Por enquanto validamos se o comando de execução dispara o mock
        robot.clickOn(btnExecutar)
        WaitForAsyncUtils.waitForFxEvents()
        
        mockJsoup.verify(MockedStatic.Verification { Jsoup.connect(anyString()) }, atLeastOnce())
    }

    @Test
    fun testValidacaoEnderecoVazio(robot: FxRobot) {
        testAbriPopupCapitulos(robot)
        val btnExecutar = robot.lookup("#btnExecutar").queryAs(JFXButton::class.java)
        
        robot.interact {
            robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java).text = ""
        }
        
        robot.clickOn(btnExecutar)
        WaitForAsyncUtils.waitForFxEvents()
        
        // Não deve ter chamado Jsoup
        mockJsoup.verify(MockedStatic.Verification { Jsoup.connect(anyString()) }, never())
    }
}
