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
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
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
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaComicInfoE2EFlowTest : BaseTest() {

    @TempDir lateinit var tempDir: Path

    private lateinit var stage: Stage
    private lateinit var mainController: TelaInicialController
    private lateinit var comicInfoController: AbaComicInfoController
    private lateinit var tabContent: Node

    private val mockWinrar = mock<WinrarServices>()
    private val mockOcrServices = mock<OcrServices>()
    private var mockOcr: MockedStatic<Ocr>? = null

    @Start
    fun start(stage: Stage) {
        this.stage = stage
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        // Extrai o controller via reflexão
        val field = mainController.javaClass.getDeclaredField("comicinfoController")
        field.isAccessible = true
        comicInfoController = field.get(mainController) as AbaComicInfoController

        // Injeta mocks no controller via reflexão
        injectMocksInternal(comicInfoController)

        val scene = Scene(root, 1024.0, 768.0)
        applyJFoenixFix(scene)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
        stage.toFront()

        // Seleciona a aba Comic Info
        val mainTabPane = mainController.rootTab
        val tabField = mainController.javaClass.getDeclaredField("tbTabComicInfo")
        tabField.isAccessible = true
        val tab = tabField.get(mainController) as Tab
        Platform.runLater { mainTabPane.selectionModel.select(tab) }
        WaitForAsyncUtils.waitForFxEvents()
        
        tabContent = tab.content
        
        // Inicializar o sistema de notificações
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = root
    }

    private fun injectMocksInternal(controller: AbaComicInfoController) {
        try {
            val winrarField = AbaComicInfoController::class.java.getDeclaredField("mRarService")
            winrarField.isAccessible = true
            winrarField.set(controller, mockWinrar)

            val ocrServicesField = AbaComicInfoController::class.java.getDeclaredField("mOcrService")
            ocrServicesField.isAccessible = true
            ocrServicesField.set(controller, mockOcrServices)
        } catch (e: Exception) {}
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Ocr.isTeste = true
        Mockito.reset(mockWinrar, mockOcrServices)
        mockOcr = Mockito.mockStatic(Ocr::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockOcr?.close()
    }

    @Test
    @Order(1)
    fun testFullFlowAbaComicInfo(robot: FxRobot) {
        // 1. Preparar mocks e arquivos
        val rar1 = File(tempDir.toFile(), "manga_001.rar").apply { createNewFile() }
        val rar2 = File(tempDir.toFile(), "manga_002.rar").apply { createNewFile() }
        
        val dummyXml = File(tempDir.toFile(), "ComicInfo.xml").apply {
            writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><ComicInfo><Series>One Piece</Series></ComicInfo>")
        }
        whenever(mockWinrar.extraiComicInfo(any())).thenReturn(dummyXml)

        // 2. Interagir com a UI (escopado)
        val txtPasta = robot.from(tabContent).lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        val btnCarregar = robot.from(tabContent).lookup("#btnCarregar").queryAs(JFXButton::class.java)
        val cbLinguagem = robot.from(tabContent).lookup("#cbLinguagem").queryAs(JFXComboBox::class.java) as JFXComboBox<Linguagem>

        robot.interact {
            cbLinguagem.value = Linguagem.JAPANESE
            txtPasta.text = tempDir.toString()
            btnCarregar.fire()
        }
        
        // 3. Validar carregamento
        val tbView = robot.from(tabContent).lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Processar>
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbView.items.size == 2 }
        
        // 4. Testar OCR
        val btnOcr = robot.from(tabContent).lookup("#btnOcrProcessar").queryAs(JFXButton::class.java)
        robot.interact { btnOcr.fire() }
        
        verify(mockWinrar, atLeastOnce()).extraiSumario(any(), any())
    }
}
