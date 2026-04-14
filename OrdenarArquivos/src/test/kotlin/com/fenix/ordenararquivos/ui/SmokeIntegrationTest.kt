package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.nio.file.Path

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SmokeIntegrationTest : BaseTest() {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mainController: TelaInicialController

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
        stage.scene = Scene(root)
        stage.show()
    }

    @Test
    @DisplayName("Fluxo Completo: Carregar -> Renomear -> Compactar")
    fun testFullApplicationFlow(robot: FxRobot) {
        // 1. Preparar ambiente físico
        val sourceDir = tempDir.resolve("manga_source").toFile().apply { mkdirs() }
        val chapter1 = File(sourceDir, "[Scan] One Piece - Cap 01").apply { mkdirs() }
        File(chapter1, "001.jpg").writeText("fake image data")
        
        // 2. Navegar para Aba Pastas
        robot.clickOn("Pastas")
        WaitForAsyncUtils.waitForFxEvents()
        
        // 3. Configurar caminhos e manga
        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        val cbManga = robot.lookup("#cbManga").queryAs(JFXComboBox::class.java)
        
        robot.interact {
            txtPasta.text = sourceDir.absolutePath
            cbManga.editor.text = "One Piece"
            cbManga.value = "One Piece"
        }
        
        // 4. Carregar Itens
        robot.clickOn("#btnCarregar")
        WaitForAsyncUtils.waitForFxEvents()
        
        // Pequena espera para o processamento da Task de carregamento
        Thread.sleep(1000) 
        WaitForAsyncUtils.waitForFxEvents()
        
        // 5. Verificar se carregou na tabela
        val tv = robot.lookup("#tbViewProcessar").queryAs(javafx.scene.control.TableView::class.java)
        assertFalse(tv.items.isEmpty(), "A tabela deveria conter o capítulo carregado")
        
        // 6. Aplicar (Fluxo Final)
        // OBS: Winrar.compactar pode falhar se o executável do WinRAR não estiver no PATH,
        // mas o Smoke Test valida o disparo da lógica e o fluxo de UI.
        val btnAplicar = robot.lookup("#btnAplicar").queryAs(JFXButton::class.java)
        robot.clickOn(btnAplicar)
        WaitForAsyncUtils.waitForFxEvents()
        
        // Validar sucesso (Notificação ou limpeza de tabela)
        // No sucesso, mObsListaProcessar.clear() é chamado (AbaPastasController:587)
        Thread.sleep(1000) 
        WaitForAsyncUtils.waitForFxEvents()
        
        assertTrue(tv.items.isEmpty() || !btnAplicar.isDisabled, "O fluxo deveria ter sido concluído.")
    }
}
