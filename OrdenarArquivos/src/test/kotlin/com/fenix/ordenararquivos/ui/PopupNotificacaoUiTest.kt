package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupNotificacaoController
import com.fenix.ordenararquivos.database.DataBase
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import java.io.File
import java.sql.DriverManager

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class PopupNotificacaoUiTest : BaseTest() {

    private lateinit var controller: PopupNotificacaoController
    private lateinit var root: AnchorPane

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_notificacao?mode=memory&cache=shared")
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
        val loader = FXMLLoader(PopupNotificacaoController.fxmlLocate)
        root = loader.load<AnchorPane>()
        controller = loader.getController()

        val scene = Scene(root, 650.0, 100.0)
        
        stage.scene = scene
        stage.show()
    }

    @Test
    fun testShortTextSizing(robot: FxRobot) {
        val text = "Short message"
        robot.interact {
            controller.setTexto(text)
        }
        
        val lblTexto = robot.lookup("#lblTexto").queryAs(Label::class.java)
        assertEquals(text, lblTexto.text)
        assertEquals(350.0, root.prefWidth)
        assertEquals(45.0, root.prefHeight)
        assertEquals(60.0, controller.wheight)
    }

    @Test
    fun testMediumTextSizing(robot: FxRobot) {
        val text = "A".repeat(150)
        robot.interact {
            controller.setTexto(text)
        }
        
        assertEquals(500.0, root.prefWidth)
        assertEquals(80.0, root.prefHeight)
        assertEquals(80.0, controller.wheight)
    }

    @Test
    fun testLongTextSizing(robot: FxRobot) {
        val text = "B".repeat(300)
        robot.interact {
            controller.setTexto(text)
        }
        
        assertEquals(650.0, root.prefWidth)
        assertEquals(100.0, root.prefHeight)
        assertEquals(100.0, controller.wheight)
    }

    @Test
    fun testSetTitulo(robot: FxRobot) {
        val titulo = "Notificação Importante"
        robot.interact {
            controller.setTitulo(titulo)
        }
        
        val lblTitulo = robot.lookup("#lblTitulo").queryAs(Label::class.java)
        assertEquals(titulo, lblTitulo.text)
    }

    @Test
    fun testSetImagem(robot: FxRobot) {
        val newImgView = ImageView()
        val mockImage = Image(PopupNotificacaoController::class.java.getResourceAsStream("/images/icoAbrir_48.png"))
        newImgView.image = mockImage
        
        robot.interact {
            controller.setImagem(newImgView)
        }
        
        assertEquals(mockImage, controller.imagem)
    }
}
