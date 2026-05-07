package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Capa
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.TipoCapa
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaArquivoRobustnessUiTest : BaseTest() {

    private lateinit var controller: AbaArquivoController
    private var mockMangaService = mock<MangaServices>()
    private var mockComicInfoService = mock<ComicInfoServices>()
    private var mockSincronizacao = mock<SincronizacaoServices>()
    private var mockWinrar = mock<WinrarServices>()
    private var mockTelaInicialController = mock<TelaInicialController>()
    private lateinit var rootStack: StackPane

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:robustness_testdb?mode=memory&cache=shared")
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
        Ocr.isTeste = true
        AlertasModal.isTeste = true
        AlertasModal.lastAlertTitle = null
        AlertasModal.lastAlertText = null
        Notificacoes.rootAnchorPane = AnchorPane()

        val loader = FXMLLoader(AbaArquivoController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaArquivoController::class.java) {
                AbaArquivoController().apply {
                    // Injeção de mocks via reflexão para campos privados/protected
                    this.javaClass.declaredFields.forEach { field ->
                        field.isAccessible = true
                        when (field.name) {
                            "mServiceManga" -> field.set(this, mockMangaService)
                            "mServiceComicInfo" -> field.set(this, mockComicInfoService)
                            "mSincronizacao" -> field.set(this, mockSincronizacao)
                            "mRarService" -> field.set(this, mockWinrar)
                        }
                    }
                    controller = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Parent>()
        rootStack = StackPane(root)
        stage.scene = Scene(rootStack)
        stage.show()
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockMangaService, mockComicInfoService, mockSincronizacao, mockWinrar, mockTelaInicialController)
        whenever(mockTelaInicialController.rootProgress).thenReturn(ProgressBar())
        whenever(mockTelaInicialController.rootMessage).thenReturn(javafx.scene.control.Label())
        whenever(mockTelaInicialController.apDragOverlay).thenReturn(AnchorPane())
        whenever(mockTelaInicialController.mSincronizacao).thenReturn(mockSincronizacao)
        
        controller.controllerPai = mockTelaInicialController
        
        AlertasModal.lastAlertTitle = null
        AlertasModal.lastAlertText = null
    }

    @Test
    fun `test erro durante compactacao deve ser tratado graciosamente` (robot: FxRobot) {
        // Setup de pastas válidas para passar na validação inicial
        val tempDir = Files.createTempDirectory("robust_test_dir").toFile()
        val originDir = File(tempDir, "origin").apply { mkdirs() }
        val destDir = File(tempDir, "dest").apply { mkdirs() }
        File(originDir, "page01.jpg").writeText("dummy")

        robot.interact {
            robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java).text = originDir.absolutePath
            robot.lookup("#txtPastaDestino").queryAs(JFXTextField::class.java).text = destDir.absolutePath
            robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text = "Robustness Manga"
            robot.lookup("#txtVolume").queryAs(JFXTextField::class.java).text = "01"
            robot.lookup("#txtNomeArquivo").queryAs(JFXTextField::class.java).text = "Manga 01.cbr"
            robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java).selectionModel.select(0)
        }
        
        WaitForAsyncUtils.waitForFxEvents()

        // Simula erro no serviço de compactação
        whenever(mockWinrar.compactar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(RuntimeException("Falha catastrófica no WinRAR"))

        robot.interact {
            // Injeção robusta: garante que a TableView e as coleções internas estão sincronizadas
            val originField = AbaArquivoController::class.java.getDeclaredField("mCaminhoOrigem")
            originField.isAccessible = true
            originField.set(controller, originDir)

            val destField = AbaArquivoController::class.java.getDeclaredField("mCaminhoDestino")
            destField.isAccessible = true
            destField.set(controller, destDir)
            
            val caminhosList = arrayListOf(com.fenix.ordenararquivos.model.entities.Caminhos("1", "1", "Pasta 1", ""))
            
            val mListaCaminhosField = AbaArquivoController::class.java.getDeclaredField("mListaCaminhos")
            mListaCaminhosField.isAccessible = true
            mListaCaminhosField.set(controller, caminhosList)

            val mObsListaCaminhosField = AbaArquivoController::class.java.getDeclaredField("mObsListaCaminhos")
            mObsListaCaminhosField.isAccessible = true
            val obsList = mObsListaCaminhosField.get(controller) as ObservableList<com.fenix.ordenararquivos.model.entities.Caminhos>
            obsList.setAll(caminhosList)

            val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
            table.items = obsList

            val btn = robot.lookup("#btnProcessar").queryAs(JFXButton::class.java)
            btn.accessibleTextProperty().set("PROCESSA")
            
            robot.clickOn(btn)
        }
        
        Thread.sleep(3000) 
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Erro ao processar", AlertasModal.lastAlertTitle)
        assertTrue(AlertasModal.lastAlertText?.contains("Falha catastrófica no WinRAR") == true, "Deveria conter a mensagem de falha do WinRAR")
        
        tempDir.deleteRecursively()
    }

    @Test
    fun `test erro ao salvar manga no banco de dados durante processamento`(robot: FxRobot) {
        val tempDir = Files.createTempDirectory("robust_db_test").toFile()
        val originDir = File(tempDir, "origin").apply { mkdirs() }
        val destDir = File(tempDir, "dest").apply { mkdirs() }
        File(originDir, "image.png").writeText("data")

        robot.interact {
            robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java).text = originDir.absolutePath
            robot.lookup("#txtPastaDestino").queryAs(JFXTextField::class.java).text = destDir.absolutePath
            robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text = "DB Fail Manga"
            robot.lookup("#txtVolume").queryAs(JFXTextField::class.java).text = "01"
            robot.lookup("#txtNomeArquivo").queryAs(JFXTextField::class.java).text = "Manga 01.cbr"
            robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java).selectionModel.select(0)
        }
        
        WaitForAsyncUtils.waitForFxEvents()

        whenever(mockMangaService.save(any(), any(), any())).thenThrow(RuntimeException("Erro de SQL"))

        robot.interact {
            val originField = AbaArquivoController::class.java.getDeclaredField("mCaminhoOrigem")
            originField.isAccessible = true
            originField.set(controller, originDir)

            val destField = AbaArquivoController::class.java.getDeclaredField("mCaminhoDestino")
            destField.isAccessible = true
            destField.set(controller, destDir)
            
            val caminhosList = arrayListOf(com.fenix.ordenararquivos.model.entities.Caminhos("1", "1", "Pasta 1", ""))
            val mListaCaminhosField = AbaArquivoController::class.java.getDeclaredField("mListaCaminhos")
            mListaCaminhosField.isAccessible = true
            mListaCaminhosField.set(controller, caminhosList)

            val mObsListaCaminhosField = AbaArquivoController::class.java.getDeclaredField("mObsListaCaminhos")
            mObsListaCaminhosField.isAccessible = true
            val obsList = mObsListaCaminhosField.get(controller) as ObservableList<com.fenix.ordenararquivos.model.entities.Caminhos>
            obsList.setAll(caminhosList)

            val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
            table.items = obsList

            val btn = robot.lookup("#btnProcessar").queryAs(JFXButton::class.java)
            btn.accessibleTextProperty().set("PROCESSA")
            
            robot.clickOn(btn)
        }
        
        Thread.sleep(3000)
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Erro ao salvar manga", AlertasModal.lastAlertTitle)
        assertEquals("Erro de SQL", AlertasModal.lastAlertText)
        
        tempDir.deleteRecursively()
    }
}
