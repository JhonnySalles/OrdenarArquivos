package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaComicInfoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXComboBox
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.control.Tab
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.sql.DriverManager
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaComicInfoUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var comicinfoController: AbaComicInfoController
    private val tempDir = File("temp_ui_comicinfo_test").absoluteFile
    private lateinit var mockWinrar: WinrarServices
    private lateinit var mockOcr: MockedStatic<Ocr>

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_comicinfo?mode=memory&cache=shared")
        }

        @AfterAll
        @JvmStatic
        fun globalTearDown() {
            staticKeepAlive?.close()
        }
    }

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        val field = mainController.javaClass.getDeclaredField("comicinfoController")
        field.isAccessible = true
        comicinfoController = field.get(mainController) as AbaComicInfoController

        mockWinrar = mock<WinrarServices>()
        
        val scene = Scene(root, 1024.0, 768.0)
        
        // Workaround for JFoenix skins
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin" !important; }
                .jfx-password-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin" !important; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin" !important; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin" !important; }
                .jfx-button { -fx-skin: "javafx.scene.control.skin.ButtonSkin" !important; }
                .jfx-tab-pane { -fx-skin: "javafx.scene.control.skin.TabPaneSkin" !important; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {}
        
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
        
        // Select Comic Info tab
        val tabPane = root.lookup("#tpGlobal") as JFXTabPane
        val tabField = mainController.javaClass.getDeclaredField("tbTabComicInfo")
        tabField.isAccessible = true
        val tab = tabField.get(mainController) as Tab
        Platform.runLater {
            tabPane.selectionModel.select(tab)
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @BeforeEach
    fun setUp() {
        if (::mockOcr.isInitialized) {
            try { mockOcr.close() } catch (e: Exception) {}
        }
        mockOcr = Mockito.mockStatic(Ocr::class.java)
        
        val winrarField = comicinfoController.javaClass.getDeclaredField("mRarService")
        winrarField.isAccessible = true
        winrarField.set(comicinfoController, mockWinrar)
        
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()
        Mockito.reset(mockWinrar)
    }

    @AfterEach
    fun tearDown() {
        if (::mockOcr.isInitialized) {
            try { mockOcr.close() } catch (e: Exception) {}
        }
    }

    private fun helperCarregarItens(robot: FxRobot) {
        val dummyFile = File(tempDir, "test_manga_001.rar")
        dummyFile.createNewFile()
        
        val dummyComicInfoXml = File(tempDir, "ComicInfo.xml")
        dummyComicInfoXml.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ComicInfo><Series>Test Series</Series><Title>Test Title</Title><Pages><Page Image=\"0\" Bookmark=\"Test\"/></Pages></ComicInfo>")
        
        whenever(mockWinrar.extraiComicInfo(any())).thenReturn(dummyComicInfoXml)
        mockOcr.`when`<String> { Ocr.process(any(), any(), any()) }.thenReturn("001-01")

        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tab = tabPane.selectionModel.selectedItem
        val tabContent = tab.content as AnchorPane

        val txtPasta = tabContent.lookup("#txtPastaProcessar") as JFXTextField
        val btnCarregar = tabContent.lookup("#btnCarregar") as JFXButton
        
        robot.interact {
            txtPasta.text = tempDir.absolutePath
            btnCarregar.fire()
        }
        
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) {
            val table = tabContent.lookup("#tbViewProcessar") as TableView<*>
            (table.items?.size ?: 0) > 0
        }
    }

    @Test
    fun testCarregarItens(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        assertEquals(1, table.items.size)
    }

    @Test
    fun testNormalizarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        val item = table.items[0]
        
        // Setup initial tags
        robot.interact {
            item.tags = "0${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMAGEM}capitulo 1"
        }
        
        val btnNormaliza = tabContent.lookup("#btnTagsNormaliza") as JFXButton
        val cbLinguagem = tabContent.lookup("#cbLinguagem") as JFXComboBox<Linguagem>
        
        robot.interact {
            // Explicitly set language to PORTUGUESE for deterministic results
            cbLinguagem.value = Linguagem.PORTUGUESE
            btnNormaliza.fire()
        }
        
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(item.tags.contains("Capítulo 001"), "A tag deveria ser normalizada para 'Capítulo 001', atual: '${item.tags}'")
    }

    @Test
    fun testSalvarTodos(robot: FxRobot) {
        helperCarregarItens(robot)
        whenever(mockWinrar.insereComicInfo(any(), any())).thenReturn(true)
        
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnSalvar = tabContent.lookup("#btnSalvarTodos") as JFXButton
        
        robot.interact {
            btnSalvar.fire()
        }
        
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS) {
            !btnSalvar.isDisable
        }
        Mockito.verify(mockWinrar, Mockito.atLeastOnce()).insereComicInfo(any(), any())
    }
}
