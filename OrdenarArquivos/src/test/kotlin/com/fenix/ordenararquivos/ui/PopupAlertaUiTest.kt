package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupAlertaController
import com.fenix.ordenararquivos.database.DataBase
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextArea
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.sql.DriverManager

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class PopupAlertaUiTest : BaseTest() {

    private lateinit var controller: PopupAlertaController

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_alerta?mode=memory&cache=shared")
            DataBase.instancia
        }

        @AfterAll
        @JvmStatic
        fun globalTearDown() {
            staticKeepAlive?.close()
            staticKeepAlive = null
            DataBase.isTeste = false
        }
    }

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(PopupAlertaController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        controller = loader.getController()

        val scene = Scene(root, 500.0, 180.0)
        
        // Fix JFoenix skins
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix_alerta", ".css")
            cssFile.writeText("""
                .jfx-button { -fx-skin: "com.sun.javafx.scene.control.skin.ButtonSkin"; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin"; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stage.scene = scene
        stage.show()
    }

    @Test
    fun testSetTexto(robot: FxRobot) {
        robot.interact {
            controller.setTexto("Teste Titulo", "Teste Texto de Alerta")
        }
        
        val lblTitulo = robot.lookup("#lblTitulo").queryAs(Label::class.java)
        val txtTexto = robot.lookup("#txtTexto").queryAs(JFXTextArea::class.java)
        
        assertEquals("Teste Titulo", lblTitulo.text)
        assertEquals("Teste Texto de Alerta", txtTexto.text)
    }

    @Test
    fun testSetVisivel(robot: FxRobot) {
        val lblTitulo = robot.lookup("#lblTitulo").queryAs(Label::class.java)
        val imgIcone = robot.lookup("#imgIcone").queryAs(ImageView::class.java)
        val txtTexto = robot.lookup("#txtTexto").queryAs(JFXTextArea::class.java)

        // Hide both
        robot.interact {
            controller.setVisivel(titulo = false, imagem = false)
        }
        assertFalse(lblTitulo.isVisible)
        assertFalse(imgIcone.isVisible)
        assertEquals(0.0, txtTexto.padding.top)
        
        // Show both
        robot.interact {
            controller.setVisivel(titulo = true, imagem = true)
        }
        assertTrue(lblTitulo.isVisible)
        assertTrue(imgIcone.isVisible)
        assertEquals(20.0, txtTexto.padding.top) // Top padding per code
    }

    @Test
    fun testButtonOkAction(robot: FxRobot) {
        var clicked = false
        robot.interact {
            controller.setEventosBotoes { clicked = true }
        }
        
        robot.clickOn("#btnOk")
        assertTrue(clicked)
    }
}
