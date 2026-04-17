package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Pasta
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@Tag("E2E")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaPastasE2EFlowTest : BaseTest() {

    @TempDir lateinit var tempDir: Path

    private lateinit var pastasController: AbaPastasController
    private val mockTelaInicial = mock<TelaInicialController>()

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(AbaPastasController.fxmlLocate)
        loader.setControllerFactory { type: Class<*> ->
            if (type == AbaPastasController::class.java) {
                AbaPastasController().apply {
                    pastasController = this
                    controllerPai = mockTelaInicial
                }
            } else AbaPastasController()
        }
        val root: AnchorPane = loader.load()
        stage.scene = Scene(root, 1000.0, 700.0)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Mockito.reset(mockTelaInicial)

        // Mockar componentes de UI do TelaInicial para evitar NPE no binding de progresso
        // (Essencial para carregarItens)
        whenever(mockTelaInicial.rootProgress).thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicial.rootMessage).thenReturn(javafx.scene.control.Label())
    }

    @Test
    @Order(1)
    fun testFullFlowAbaPastas(robot: FxRobot) {
        // 1. Preparar pastas dummy
        val folder1 =
                File(tempDir.toFile(), "[Scan] Naruto - Volume 01 Capítulo 01").apply { mkdirs() }
        val folder2 =
                File(tempDir.toFile(), "[Scan] Naruto - Volume 01 Capítulo 02").apply { mkdirs() }

        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        val btnCarregar = robot.lookup("#btnCarregar").queryAs(JFXButton::class.java)
        val cbManga = robot.lookup("#cbManga").queryAs(com.jfoenix.controls.JFXComboBox::class.java)

        robot.interact {
            cbManga.editor.text = "Naruto"
            txtPasta.text = tempDir.toAbsolutePath().toString()
            btnCarregar.fire()
        }

        // 2. Validar carregamento na tabela
        val tbView =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS) { tbView.items.size >= 2 }

        assertTrue(tbView.items.any { it.nome.contains("Naruto", ignoreCase = true) })
    }
}
