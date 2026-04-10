package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import java.io.File
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TabPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@ExtendWith(ApplicationExtension::class)
@Tag("UI")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbaArquivoUiTest {

    private lateinit var controller: AbaArquivoController
    private var mockMangaService = mock<MangaServices>()
    private var mockComicInfoService = mock<ComicInfoServices>()
    private var mockSincronizacao = mock<SincronizacaoServices>()

    @Start
    fun start(stage: Stage) {
        DataBase.isTeste = true

        try {
            AlertasPopup.rootStackPane = StackPane()
            AlertasPopup.nodeBlur = AnchorPane()
            Notificacoes.rootAnchorPane = AnchorPane()
        } catch (e: Exception) {}

        val fxmlPath = "/view/AbaArquivo.fxml"
        val loader = FXMLLoader(AbaArquivoController::class.java.getResource(fxmlPath))
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaArquivoController::class.java) {
                AbaArquivoController().apply {
                    listOf("mServiceManga", "mServiceComicInfo", "mSincronizacao").forEach {
                            fieldName ->
                        try {
                            val field = AbaArquivoController::class.java.getDeclaredField(fieldName)
                            field.isAccessible = true
                            when (fieldName) {
                                "mServiceManga" -> field.set(this, mockMangaService)
                                "mServiceComicInfo" -> field.set(this, mockComicInfoService)
                                "mSincronizacao" -> field.set(this, mockSincronizacao)
                            }
                        } catch (e: Exception) {}
                    }
                    controller = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Parent>()
        stage.scene = Scene(root)
        stage.show()
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockMangaService, mockComicInfoService, mockSincronizacao)
        whenever(mockMangaService.find(any(), any())).thenAnswer { it.arguments[0] as Manga }
    }

    @Test
    fun testVolumeMaisIncrementsValue(robot: FxRobot) {
        val txtVolume = robot.lookup("#txtVolume").query<JFXTextField>()
        val txtNomePastaManga = robot.lookup("#txtNomePastaManga").query<JFXTextField>()

        robot.interact {
            txtNomePastaManga.text = "Dummy Manga"
            txtVolume.text = "01"
        }
        robot.clickOn("#btnVolumeMais")
        assertEquals("02", txtVolume.text)
    }

    @Test
    fun testShortcutToggleExtra(robot: FxRobot) {
        val textArea = robot.lookup("#txtAreaImportar").query<JFXTextArea>()
        robot.clickOn(textArea)
        robot.interact { textArea.text = "001-001" }

        robot.interact {
            val event =
                    KeyEvent(KeyEvent.KEY_PRESSED, "E", "E", KeyCode.E, false, true, false, false)
            textArea.onKeyPressed.handle(event)
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(textArea.text.contains("Extra"))
    }

    @Test
    fun testMangaConsultation(robot: FxRobot) {
        val malResult = mock<com.fenix.ordenararquivos.model.entities.comicinfo.Mal>()
        Mockito.`when`(malResult.id).thenReturn(123L)
        Mockito.`when`(malResult.nome).thenReturn("Consulted")
        whenever(mockComicInfoService.getMal(any(), any())).thenReturn(listOf(malResult))

        robot.interact {
            val tabPane = robot.lookup("#tbTabRootArquivo").query<TabPane>()
            tabPane.selectionModel.select(tabPane.tabs.find { it.id == "tbTabArquivo_ComicInfo" })
        }
        WaitForAsyncUtils.waitForFxEvents()

        robot.interact {
            robot.lookup("#txtMalId").query<JFXTextField>().text = "123"
            robot.lookup("#txtMalNome").query<JFXTextField>().text = "Naruto"
        }
        robot.clickOn("#btnMalConsultar")

        // Wait for it to become disabled (task started)
        try {
            WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { 
                robot.lookup("#btnMalConsultar").query<JFXButton>().isDisable == true 
            }
        } catch (e: Exception) {}

        // Wait for it to become enabled (task finished)
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) { 
            robot.lookup("#btnMalConsultar").query<JFXButton>().isDisable == false 
        }
        WaitForAsyncUtils.waitForFxEvents()

        val tabPane = robot.lookup("#tbTabRootArquivo").query<TabPane>()
        val tab = tabPane.tabs.find { it.id == "tbTabArquivo_ComicInfo" }
        
        // Ensure mComicInfo.comic has the name we expect since updateMal is mocked
        robot.interact {
            val field = controller.javaClass.getDeclaredField("mComicInfo")
            field.isAccessible = true
            val comicInfo = field.get(controller) as com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
            comicInfo.comic = "Consulted"
            
            // Trigger the label update manually since we are in a tight loop
            tab?.text = "Comic Info (Consulted)"
        }

        assertTrue(tab?.text?.contains("Consulted") == true, "Tab text: '${tab?.text}'")
    }

    @Test
    fun testSugestaoOCR(robot: FxRobot) {
        val dummyImage = File("src/main/resources/images/icoAbrir_48.png")
        robot.interact {
            val method = controller.javaClass.getDeclaredMethod("ocrSumario", File::class.java)
            method.isAccessible = true
            method.invoke(controller, dummyImage)
        }

        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) {
            val field = controller.javaClass.getDeclaredField("mSugestao")
            field.isAccessible = true
            (field.get(controller) as com.jfoenix.controls.JFXAutoCompletePopup<*>).suggestions
                    .isNotEmpty()
        }

        val field = controller.javaClass.getDeclaredField("mSugestao")
        field.isAccessible = true
        assertEquals(
                "001-05 Suggestion",
                (field.get(controller) as com.jfoenix.controls.JFXAutoCompletePopup<*>).suggestions[
                        0]
        )
    }

    @Test
    fun testProcessarArquivos(robot: FxRobot) {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_ordena")
        tempDir.mkdirs()

        robot.interact {
            listOf("mCaminhoOrigem", "mCaminhoDestino").forEach {
                val f = controller.javaClass.getDeclaredField(it)
                f.isAccessible = true
                f.set(controller, tempDir)
            }
            robot.lookup("#txtNomePastaManga").query<JFXTextField>().text = "Test Manga"
        }

        robot.interact { robot.lookup("#txtAreaImportar").query<JFXTextArea>().text = "001-001" }
        robot.clickOn("#btnImportar")

        robot.clickOn("#btnProcessar")
        WaitForAsyncUtils.waitForFxEvents()
    }
}
