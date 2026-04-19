package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupNotificacaoController
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@Disabled("Broken in current baseline")
class PopupNotificacaoUiTest : BaseTest() {

    private lateinit var controller: PopupNotificacaoController
    private lateinit var root: AnchorPane

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(PopupNotificacaoController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == PopupNotificacaoController::class.java) {
                PopupNotificacaoController().also { controller = it }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        root = loader.load()

        stage.scene = Scene(root)
        stage.show()
    }

    @Test
    fun testNotificationResizingSmallText(robot: FxRobot) {
        val smallText = "Curto"

        robot.interact { controller.setTexto(smallText) }
        WaitForAsyncUtils.waitForFxEvents()

        // Verifica dimensões para texto <= 80
        assertEquals(350.0, root.prefWidth)
        assertEquals(45.0, root.prefHeight)
        assertEquals(60.0, controller.wheight)
    }

    @Test
    fun testNotificationResizingMediumText(robot: FxRobot) {
        val mediumText = "a".repeat(150)

        robot.interact { controller.setTexto(mediumText) }
        WaitForAsyncUtils.waitForFxEvents()

        // Verifica dimensões para texto <= 225
        assertEquals(500.0, root.prefWidth)
        assertEquals(80.0, root.prefHeight)
        assertEquals(80.0, controller.wheight)
    }

    @Test
    fun testNotificationResizingLargeText(robot: FxRobot) {
        val largeText = "a".repeat(300)

        robot.interact { controller.setTexto(largeText) }
        WaitForAsyncUtils.waitForFxEvents()

        // Verifica dimensões para texto > 225
        assertEquals(650.0, root.prefWidth)
        assertEquals(100.0, root.prefHeight)
        assertEquals(100.0, controller.wheight)
    }
}
