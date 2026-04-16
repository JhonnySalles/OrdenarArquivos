package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaComicInfoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
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
class AbaComicInfoE2EFlowTest : BaseTest() {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var stage: Stage
    private lateinit var mainController: TelaInicialController
    private lateinit var comicinfoController: AbaComicInfoController

    private val mockWinrar = mock<WinrarServices>()
    private val mockOcrServices = mock<OcrServices>()
    private var mockOcr: MockedStatic<Ocr>? = null

    @Start
    fun start(stage: Stage) {
        this.stage = stage
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root: AnchorPane = loader.load()
        mainController = loader.getController()

        val field = mainController.javaClass.getDeclaredField("comicinfoController")
        field.isAccessible = true
        comicinfoController = field.get(mainController) as AbaComicInfoController

        injectMocks(comicinfoController)

        stage.scene = Scene(root, 1200.0, 800.0)
        stage.show()
        stage.toFront()
    }

    private fun injectMocks(controller: AbaComicInfoController) {
        val winrarField = AbaComicInfoController::class.java.getDeclaredField("mRarService")
        winrarField.isAccessible = true
        winrarField.set(controller, mockWinrar)
        
        val ocrServicesField = AbaComicInfoController::class.java.getDeclaredField("mOcrService")
        ocrServicesField.isAccessible = true
        ocrServicesField.set(controller, mockOcrServices)
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Ocr.isTeste = true
        Mockito.reset(mockWinrar, mockOcrServices)
        mockOcr = Mockito.mockStatic(Ocr::class.java)
        
        // Selecionar aba Comic Info via objeto Tab para máxima precisão
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        robot.interact { 
            val tab = tabPane.tabs.find { it.text == "Comic Info" }
            tabPane.selectionModel.select(tab)
        }
        
        WaitForAsyncUtils.waitForFxEvents()
        TimeUnit.MILLISECONDS.sleep(1000) 
    }

    @AfterEach
    fun tearDown() {
        mockOcr?.close()
    }

    @Test
    @Order(1)
    fun testFullFlowAbaComicInfo(robot: FxRobot) {
        // 1. Preparar arquivos físicos dummy
        val rar1 = File(tempDir.toFile(), "manga_001.rar").apply { createNewFile() }
        val rar2 = File(tempDir.toFile(), "manga_002.rar").apply { createNewFile() }
        
        // Mock de ComicInfo interno nos arquivos
        val dummyComicInfoXml = File(tempDir.toFile(), "ComicInfo.xml").apply {
            writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ComicInfo>
                  <Series>Manga Title</Series>
                  <Title>Chapter 1</Title>
                  <Pages>
                    <Page Image="0" Bookmark="Capítulo 1" />
                    <Page Image="1" Bookmark="Capítulo 2" />
                  </Pages>
                </ComicInfo>
            """.trimIndent())
        }
        whenever(mockWinrar.extraiComicInfo(any())).thenReturn(dummyComicInfoXml)

        // 2. Carregar itens
        robot.interact { stage.requestFocus() }
        robot.clickOn("#txtPastaProcessar").write(tempDir.toAbsolutePath().toString())
        WaitForAsyncUtils.waitForFxEvents()
        TimeUnit.MILLISECONDS.sleep(200)
        // Garante que o botão está habilitado e visível nos limites da cena antes de clicar
        val btnCarregar = robot.lookup("#btnCarregar").queryAs(JFXButton::class.java)
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { 
            !btnCarregar.isDisable && btnCarregar.isVisible && btnCarregar.scene != null
        }
        
        // Tenta focar na cena antes de clicar
        robot.interact { btnCarregar.requestFocus() }
        robot.clickOn("#btnCarregar")
        
        // Aguardar o carregamento da Task (máximo 5s para diagnóstico)
        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Processar>
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) {
            tbViewProcessar.items.size == 2
        }
        WaitForAsyncUtils.waitForFxEvents()

        // 3. Processar OCR (Mockado)
        val dummySumario = File(tempDir.toFile(), "sumario.jpg").apply { createNewFile() }
        whenever(mockWinrar.extraiSumario(any(), any())).thenReturn(dummySumario)
        whenever(mockOcrServices.processOcr(any(), any(), any())).thenReturn("001|05|Manga Title OCR")

        robot.clickOn("#btnOcrProcessar")
        
        // Aguardar o OCR Task concluir (máximo 3s)
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) {
            tbViewProcessar.items.all { it.isProcessado }
        }
        WaitForAsyncUtils.waitForFxEvents()

        // 4. Normalizar e Salvar Todos
        robot.clickOn("#btnTagsNormaliza")
        WaitForAsyncUtils.waitForFxEvents()
        
        whenever(mockWinrar.insereComicInfo(any(), any())).thenReturn(true)
        
        robot.clickOn("#btnSalvarTodos")
        
        // Aguardar o Salvamento concluído (máximo 3s)
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) {
            tbViewProcessar.items.isEmpty()
        }
        WaitForAsyncUtils.waitForFxEvents()

        // 5. Verificações Finais
        verify(mockWinrar, times(2)).insereComicInfo(any(), any())
        assertTrue(tbViewProcessar.items.isEmpty(), "A tabela deveria estar limpa após salvar tudo")
    }
}
