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
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
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

@Tag("E2E")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaComicInfoE2EFlowTest : BaseTest() {

    @TempDir lateinit var tempDir: Path

    private lateinit var comicInfoController: AbaComicInfoController
    private val mockWinrar = mock<WinrarServices>()
    private val mockOcrServices = mock<OcrServices>()
    private val mockTelaInicial = mock<TelaInicialController>()
    private var mockOcr: MockedStatic<Ocr>? = null

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(AbaComicInfoController.fxmlLocate)
        loader.setControllerFactory { type: Class<*> ->
            if (type == AbaComicInfoController::class.java) {
                AbaComicInfoController().apply {
                    comicInfoController = this
                    controllerPai = mockTelaInicial
                    injectMocksInternal(this)
                }
            } else AbaComicInfoController()
        }
        val root: AnchorPane = loader.load()
        stage.scene = Scene(root, 1000.0, 700.0)
        stage.show()
    }

    private fun injectMocksInternal(controller: AbaComicInfoController) {
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
        Mockito.reset(mockWinrar, mockOcrServices, mockTelaInicial)
        
        // Mockar componentes de UI do TelaInicial para evitar NPE no binding de progresso
        whenever(mockTelaInicial.rootProgress).thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicial.rootMessage).thenReturn(javafx.scene.control.Label())
        
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

        // 2. Interagir com a UI
        val txtPasta = robot.lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        val btnCarregar = robot.lookup("#btnCarregar").queryAs(JFXButton::class.java)
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java) as JFXComboBox<Linguagem>

        robot.interact {
            cbLinguagem.value = Linguagem.JAPANESE
            txtPasta.text = tempDir.toString()
            btnCarregar.fire()
        }
        
        // 3. Validar carregamento
        val tbView = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Processar>
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS) { tbView.items.size == 2 }
        
        // 4. Testar OCR (Gatilho)
        val btnOcr = robot.lookup("#btnOcrProcessar").queryAs(JFXButton::class.java)
        robot.interact { btnOcr.fire() }
        
        // Validar que tentou extrair sumário (OCR chama extraiSumario)
        verify(mockWinrar, atLeastOnce()).extraiSumario(any(), any())
    }
}
