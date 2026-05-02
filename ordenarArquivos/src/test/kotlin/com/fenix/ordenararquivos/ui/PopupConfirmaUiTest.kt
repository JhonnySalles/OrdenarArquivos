package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupConfirmaController
import com.jfoenix.controls.JFXTextArea
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupConfirmaUiTest : BaseTest() {

    private lateinit var controller: PopupConfirmaController

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(PopupConfirmaController.fxmlLocate)
        val root: AnchorPane = loader.load()
        controller = loader.getController()

        stage.scene = Scene(root)
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(1)
    fun testInitialState(robot: FxRobot) {
        val lblTitulo = robot.lookup("#lblTitulo").queryAs(Label::class.java)
        val txtTexto = robot.lookup("#txtTexto").queryAs(JFXTextArea::class.java)
        val btnCancelar = robot.lookup("#btnCancelar").queryAs(Button::class.java)
        val btnConfirmar = robot.lookup("#btnConfirmar").queryAs(Button::class.java)

        assertNotNull(lblTitulo)
        assertNotNull(txtTexto)
        assertNotNull(btnCancelar)
        assertNotNull(btnConfirmar)
    }

    @Test
    @Order(2)
    fun testSetTexto(robot: FxRobot) {
        robot.interact {
            controller.setTexto("Confirmação de Teste", "Deseja realmente prosseguir?")
        }
        
        val lblTitulo = robot.lookup("#lblTitulo").queryAs(Label::class.java)
        val txtTexto = robot.lookup("#txtTexto").queryAs(JFXTextArea::class.java)

        assertEquals("Confirmação de Teste", lblTitulo.text)
        assertEquals("Deseja realmente prosseguir?", txtTexto.text)
    }

    @Test
    @Order(3)
    fun testEventosConfirmacao(robot: FxRobot) {
        var cancelado = false
        var confirmado = false

        robot.interact {
            controller.setEventosConfirmacao(
                { cancelado = true },
                { confirmado = true }
            )
        }

        robot.clickOn("#btnCancelar")
        assertTrue(cancelado, "Evento de cancelar deveria ter sido disparado")
        assertFalse(confirmado, "Evento de confirmar não deveria ter sido disparado")

        cancelado = false // Reset
        robot.clickOn("#btnConfirmar")
        assertTrue(confirmado, "Evento de confirmar deveria ter sido disparado")
        assertFalse(cancelado, "Evento de cancelar não deveria ter sido disparado")
    }
 
    @Test
    @Order(4)
    fun testIcons(robot: FxRobot) {
        val imgIcone = robot.lookup("#imgIcone").queryAs(javafx.scene.image.ImageView::class.java)
        
        robot.interact { controller.aviso() }
        assertEquals(PopupConfirmaController.IMG_AVISO, imgIcone.image)

        robot.interact { controller.alerta() }
        assertEquals(PopupConfirmaController.IMG_ALERTA, imgIcone.image)

        robot.interact { controller.erro() }
        assertEquals(PopupConfirmaController.IMG_ERRO, imgIcone.image)

        robot.interact { controller.confirmacao() }
        assertEquals(PopupConfirmaController.IMG_CONFIRMA, imgIcone.image)
    }
}
