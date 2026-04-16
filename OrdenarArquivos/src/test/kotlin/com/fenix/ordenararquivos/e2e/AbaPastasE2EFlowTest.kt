package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Tag("E2E")
@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaPastasE2EFlowTest : BaseTest() {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mainController: TelaInicialController
    private lateinit var pastasController: AbaPastasController

    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            when (controllerClass) {
                TelaInicialController::class.java -> TelaInicialController().also { mainController = it }
                AbaPastasController::class.java -> AbaPastasController().also { pastasController = it }
                else -> controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        val root: AnchorPane = loader.load()

        injectMocks(pastasController)

        stage.scene = Scene(root, 1200.0, 800.0)
        stage.show()
    }

    private fun injectMocks(controller: AbaPastasController) {
        val mangaServiceField = AbaPastasController::class.java.getDeclaredField("mServiceManga")
        mangaServiceField.isAccessible = true
        mangaServiceField.set(controller, mockMangaService)

        val comicInfoServiceField = AbaPastasController::class.java.getDeclaredField("mServiceComicInfo")
        comicInfoServiceField.isAccessible = true
        comicInfoServiceField.set(controller, mockComicInfoService)
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Mockito.reset(mockMangaService, mockComicInfoService)
        
        // Mock default behavior for Manga service
        whenever(mockMangaService.listar()).thenReturn(listOf("One Piece", "Naruto"))

        // Navigate to Pastas tab
        robot.clickOn("Pastas")
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(1)
    fun testFullFlowAbaPastas(robot: FxRobot) {
        // 1. Preparar pastas físicas
        val sub1 = tempDir.resolve("[Scan A] One Piece v01 c01").toFile().apply { mkdirs() }
        val sub2 = tempDir.resolve("[Scan A] One Piece v01 c02").toFile().apply { mkdirs() }
        File(sub1, "img.jpg").writeText("data")
        File(sub2, "img.jpg").writeText("data")

        // 2. Selecionar Manga (Antes de carregar para que o nome seja extraído corretamente)
        val cbManga = robot.lookup("#cbManga").queryAs(JFXComboBox::class.java) as JFXComboBox<String>
        robot.interact {
            cbManga.selectionModel.select("One Piece")
        }

        // 3. Carregar pastas
        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        robot.interact {
            txtPasta.text = tempDir.toAbsolutePath().toString()
        }
        robot.clickOn("#btnCarregar")
        
        // Aguardar o carregamento da Task (máximo 1s)
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            !robot.lookup("#btnCarregar").queryAs(JFXButton::class.java).isDisable
        }
        WaitForAsyncUtils.waitForFxEvents()

        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        assertEquals(2, tbViewProcessar.items.size, "Deveriam ter sido carregadas 2 pastas")

        
        // 4. Aplicar (Renomear pastas reais)
        robot.clickOn("#btnAplicar")
        
        // Aguardar o processamento da Task de renomeio (máximo 1s)
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            !robot.lookup("#btnAplicar").queryAs(JFXButton::class.java).isDisable
        }
        WaitForAsyncUtils.waitForFxEvents()

        // 5. Verificar se as pastas foram renomeadas organicamente
        val files = tempDir.toFile().listFiles()
        assertTrue(files?.any { it.name.contains("One Piece") && it.name.contains("Capítulo 001") } == true, "Pasta do Cap 1 não encontrada ou renomeada incorretamente")
        assertTrue(files?.any { it.name.contains("One Piece") && it.name.contains("Capítulo 002") } == true, "Pasta do Cap 2 não encontrada ou renomeada incorretamente")
        
        // Verificar se a tabela foi limpa
        assertTrue(tbViewProcessar.items.isEmpty(), "A tabela deveria estar limpa após o processamento")
    }
}
