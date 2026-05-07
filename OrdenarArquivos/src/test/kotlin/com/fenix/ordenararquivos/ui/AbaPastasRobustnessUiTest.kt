package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.notification.ConfirmaModal
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Tab
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
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
import java.sql.DriverManager
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaPastasRobustnessUiTest : BaseTest() {

    private lateinit var pastasController: AbaPastasController
    private lateinit var mockMangaService: MangaServices
    private lateinit var mockComicInfoService: ComicInfoServices
    private lateinit var mockRarService: WinrarServices

    @TempDir
    lateinit var tempDir: Path
    private lateinit var mockConfirmaModal: MockedStatic<ConfirmaModal>

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:pastas_robust_testdb?mode=memory\u0026cache=shared")
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

    private lateinit var mockTelaInicialController: TelaInicialController
    private lateinit var rootStack: StackPane
    private lateinit var rootNode: Parent

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()
        mockMangaService = mock()
        mockComicInfoService = mock()
        mockRarService = mock()

        val loader = FXMLLoader(AbaPastasController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaPastasController::class.java) {
                AbaPastasController().apply {
                    listOf("mServiceManga", "mServiceComicInfo", "mRarService").forEach { fieldName ->
                        try {
                            val field = AbaPastasController::class.java.getDeclaredField(fieldName)
                            field.isAccessible = true
                            when (fieldName) {
                                "mServiceManga" -> field.set(this, mockMangaService)
                                "mServiceComicInfo" -> field.set(this, mockComicInfoService)
                                "mRarService" -> field.set(this, mockRarService)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    pastasController = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        rootNode = loader.load<Parent>()
        rootStack = StackPane(rootNode)

        val scene = Scene(rootStack, 1024.0, 768.0)
        applyJFoenixFix(scene)
        stage.setScene(scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        AlertasModal.isTeste = true
        AlertasModal.lastAlertText = null
        AlertasModal.lastAlertTitle = null

        ConfirmaModal.rootStackPane = rootStack
        ConfirmaModal.nodeBlur = rootNode
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = rootNode as AnchorPane

        mockConfirmaModal = Mockito.mockStatic(ConfirmaModal::class.java)

        Mockito.reset(mockMangaService, mockComicInfoService, mockTelaInicialController, mockRarService)

        pastasController.controllerPai = mockTelaInicialController
        whenever(mockTelaInicialController.rootProgress).thenReturn(ProgressBar())
        whenever(mockTelaInicialController.rootMessage).thenReturn(Label())
        whenever(mockTelaInicialController.rootStack).thenReturn(rootStack)
        val tabPane = JFXTabPane().apply { tabs.addAll(Tab("Pastas"), Tab("Comic Info")) }
        whenever(mockTelaInicialController.rootTab).thenReturn(tabPane)
    }

    @AfterEach
    fun tearDown() {
        if (::mockConfirmaModal.isInitialized) {
            mockConfirmaModal.close()
        }
    }

    @Test
    fun `test carregar itens com diretorio excluido entre verificacao e execucao`(robot: FxRobot) {
        val dirSumindo = tempDir.resolve("sumindo").toFile()
        dirSumindo.mkdirs()

        // Usamos mockConstruction para interceptar a criação do File dentro do controlador
        Mockito.mockConstruction(File::class.java) { mock, context ->
            if (context.arguments().firstOrNull() == dirSumindo.absolutePath) {
                whenever(mock.exists()).thenReturn(true)
                whenever(mock.isDirectory).thenReturn(true)
                whenever(mock.listFiles()).thenReturn(null)
                whenever(mock.absolutePath).thenReturn(dirSumindo.absolutePath)
            }
        }.use {
            val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
            robot.interact { txtPasta.text = dirSumindo.absolutePath }

            robot.clickOn("#btnCarregar")

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) {
                AlertasModal.lastAlertTitle == "Erro ao carregar"
            }

            assertTrue(AlertasModal.lastAlertText!!.contains("Não foi possível listar os arquivos"))
        }
    }

    @Test
    fun `test erro ao compactar na AbaPastas deve ser tratado`(robot: FxRobot) {
        val pastaTest = Pasta(tempDir.resolve("Folder").toFile().apply { mkdirs() }, "Folder", volume = 1f, isSelecionado = true)
        
        robot.interact {
            pastasController.mObsListaProcessar.add(pastaTest)
            robot.lookup("#txtPasta").queryAs(JFXTextField::class.java).text = tempDir.toAbsolutePath().toString()
            val cb = robot.lookup("#cbManga").queryAs(JFXComboBox::class.java) as JFXComboBox<String>
            cb.editor.text = "Manga Teste"
            cb.value = "Manga Teste"
            
            val btn = robot.lookup("#btnCompactar").queryAs(JFXButton::class.java)
            btn.accessibleText = "COMPACTAR"
        }

        whenever(mockRarService.compactar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("Falha de I-O no WinRAR"))

        robot.clickOn("#btnCompactar")

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) {
            AlertasModal.lastAlertTitle == "Erro ao compactar"
        }

        assertTrue(AlertasModal.lastAlertText!!.contains("Falha de I-O no WinRAR"))
    }

    @Test
    fun `test erro ao renomear pastas deve ser tratado`(robot: FxRobot) {
        val pastaTest = Pasta(tempDir.resolve("FolderRename").toFile().apply { mkdirs() }, "FolderRename", scan = "Scan", nome = "Manga", volume = 1f, capitulo = 1f)
        
        robot.interact {
            pastasController.mObsListaProcessar.add(pastaTest)
            robot.lookup("#txtPasta").queryAs(JFXTextField::class.java).text = tempDir.toAbsolutePath().toString()
            
            val btn = robot.lookup("#btnRenomear").queryAs(JFXButton::class.java)
            btn.accessibleText = "RENOMEAR"
        }

        // Simula erro de sistema de arquivos durante o renomeio (ex: FileSystemException)
        // Como o controlador usa Files.move internamente, poderíamos usar Mockito para Files.move mas é estático e complexo.
        // Vou forçar uma exceção via mock de comportamento ou apenas garantir que o try-catch funciona.
        
        // Na verdade, o renomear() chama Files.move(path, target, StandardCopyOption.REPLACE_EXISTING)
        // Se eu deletar a pasta original LOGO antes de clicar, ele deve falhar.
        
        robot.interact {
            pastaTest.pasta.delete()
        }

        robot.clickOn("#btnRenomear")

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) {
            AlertasModal.lastAlertTitle == "Erro ao renomear pastas"
        }

        assertNotNull(AlertasModal.lastAlertText)
    }
}
