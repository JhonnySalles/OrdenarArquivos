package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaComicInfoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import com.fenix.ordenararquivos.model.entities.Processar

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaComicInfoUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var comicInfoController: AbaComicInfoController
    private val tempDir = File("temp_ui_comicinfo_test")
    
    private lateinit var mockWinrar: WinrarServices
    private lateinit var mockOcr: OcrServices

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_comicinfo?mode=memory&cache=shared")
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
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()
        
        // Extract comicInfoController via reflection
        val field = mainController.javaClass.getDeclaredField("comicInfoController")
        field.isAccessible = true
        comicInfoController = field.get(mainController) as AbaComicInfoController

        // Mock Services and inject
        mockWinrar = Mockito.mock(WinrarServices::class.java)
        mockOcr = Mockito.mock(OcrServices::class.java)
        
        val winrarField = comicInfoController.javaClass.getDeclaredField("mRarService")
        winrarField.isAccessible = true
        winrarField.set(comicInfoController, mockWinrar)
        
        val ocrField = comicInfoController.javaClass.getDeclaredField("mOcrService")
        ocrField.isAccessible = true
        ocrField.set(comicInfoController, mockOcr)

        val scene = Scene(root, 1024.0, 768.0)
        
        // Fix JFoenix skins for tests
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix_comic", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin"; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainController.configurarAtalhos(scene)
        stage.scene = scene
        WaitForAsyncUtils.waitForFxEvents()
        stage.show()
        stage.toFront()
    }

    @BeforeEach
    fun setUp() {
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()
        
        // Create a dummy rar file
        File(tempDir, "test_manga.rar").writeText("dummy content")
        
        // Setup default mock behavior
        val dummyComicInfoXml = File(tempDir, "ComicInfo.xml")
        dummyComicInfoXml.writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <ComicInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <Series>Test Series</Series>
              <Title>Test Title</Title>
              <Publisher>Test Publisher</Publisher>
              <Year>2023</Year>
              <Month>10</Month>
              <Day>27</Day>
              <Pages>
                <Page Image="0" Bookmark="Capítulo 01" />
                <Page Image="1" />
              </Pages>
            </ComicInfo>
        """.trimIndent())
        
        Mockito.`when`(mockWinrar.extraiComicInfo(Mockito.any(File::class.java))).thenReturn(dummyComicInfoXml)
        Mockito.`when`(mockOcr.processOcr(Mockito.any(File::class.java), Mockito.anyString(), Mockito.anyString()))
            .thenReturn("001-01\n002-05")
    }

    @AfterEach
    fun tearDown() {
        if (tempDir.exists()) tempDir.deleteRecursively()
    }

    @Test
    fun testInitialUIState(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        assertNotNull(cbLinguagem)
        assertEquals(Linguagem.JAPANESE, cbLinguagem.value)
        
        val txtPasta = robot.lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        assertEquals("", txtPasta.text)
        
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        assertTrue(table.items.isEmpty())
        
        assertNotNull(robot.lookup("#btnCarregar"))
        assertNotNull(robot.lookup("#btnOcrProcessar"))
    }

    @Test
    fun testCarregarItens(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        
        val txtPasta = robot.lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        robot.interact {
            txtPasta.text = tempDir.absolutePath
        }
        
        robot.clickOn("#btnCarregar")
        
        // Wait for background Task
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500) // Give it a moment to process the dummy file
        WaitForAsyncUtils.waitForFxEvents()

        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        assertEquals(1, table.items.size, "A tabela deveria ter 1 item carregado")
        
        val item = table.items[0] as Processar
        assertEquals("test_manga.rar", item.arquivo)
        assertEquals("Test Series", item.comicInfo?.series)
    }

    @Test
    fun testGerarTags(robot: FxRobot) {
        testCarregarItens(robot)
        
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        robot.interact {
            cbLinguagem.value = Linguagem.PORTUGUESE
        }
        
        robot.clickOn("#btnTagsProcessar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        val item = table.items[0] as Processar
        assertTrue(item.tags.contains("Capítulo 01"), "As tags deveriam conter 'Capítulo 01'. Atual: ${item.tags}")
    }

    @Test
    fun testNormalizarTags(robot: FxRobot) {
        testCarregarItens(robot)
        
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        val item = table.items[0] as Processar
        robot.interact {
            item.tags = "0|capitulo 01"
        }
        
        robot.clickOn("#btnTagsNormaliza")
        WaitForAsyncUtils.waitForFxEvents()
        
        assertTrue(item.tags.contains("Capítulo 01.0"), "Tags não normalizadas corretamente. Atual: ${item.tags}")
    }

    @Test
    fun testOcrProcessTask(robot: FxRobot) {
        testCarregarItens(robot)
        
        // Mock returning a file for extractSumario
        val dummySumario = File(tempDir, "zSumário.png")
        dummySumario.writeText("OCR mock content")
        Mockito.`when`(mockWinrar.extraiSumario(Mockito.any(File::class.java), Mockito.any(File::class.java))).thenReturn(dummySumario)

        robot.clickOn("#btnOcrProcessar")
        
        // Wait for task to finish
        var attempts = 0
        while (attempts < 10) {
            WaitForAsyncUtils.waitForFxEvents()
            val text = robot.lookup("#btnOcrProcessar").queryAs(JFXButton::class.java).text
            if (text.contains("OCR proximos 10")) break
            Thread.sleep(500)
            attempts++
        }
        
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        val item = table.items[0] as Processar
        assertTrue(item.isProcessado)
    }
    
    @Test
    fun testLanguageToggle(robot: FxRobot) {
        testCarregarItens(robot)
        
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        
        // Test Japanese
        robot.interact { cbLinguagem.value = Linguagem.JAPANESE }
        robot.clickOn("#btnTagsProcessar")
        WaitForAsyncUtils.waitForFxEvents()
        val item = (robot.lookup("#tbViewProcessar").queryAs(TableView::class.java).items[0] as Processar)
        assertTrue(item.tags.contains("第"), "Deveria conter '第' para Japonês. Atual: ${item.tags}")
        
        // Test English
        robot.interact { cbLinguagem.value = Linguagem.ENGLISH }
        robot.clickOn("#btnTagsProcessar")
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(item.tags.contains("Chapter"), "Deveria conter 'Chapter' para Inglês. Atual: ${item.tags}")
    }

    @Test
    fun testSalvarTodos(robot: FxRobot) {
        testCarregarItens(robot)
        
        robot.clickOn("#btnSalvarTodos")
        
        // Wait for task
        var attempts = 0
        while (attempts < 10) {
            WaitForAsyncUtils.waitForFxEvents()
            val text = robot.lookup("#btnSalvarTodos").queryAs(JFXButton::class.java).text
            if (text.contains("Salvar todos")) break
            Thread.sleep(500)
            attempts++
        }
        
        Mockito.verify(mockWinrar, Mockito.atLeastOnce()).insereComicInfo(Mockito.any(File::class.java), Mockito.any(File::class.java))
    }
}
