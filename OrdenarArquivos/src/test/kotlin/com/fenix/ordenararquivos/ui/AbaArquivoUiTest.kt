package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.process.Winrar
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXPasswordField
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXTabPane
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TextField
import javafx.scene.control.TextArea
import javafx.scene.control.TableView
import javafx.scene.control.ListView
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Tab
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.stage.Window
import javafx.collections.ObservableList
import javafx.util.Callback
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.sql.DriverManager
import java.util.zip.ZipFile
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaArquivoUiTest : BaseTest() {

    lateinit var mainController: TelaInicialController
    lateinit var arquivoController: AbaArquivoController
    lateinit var mockMangaService: MangaServices
    lateinit var mockComicInfoService: ComicInfoServices
    lateinit var mockWinRarService: WinrarServices

    @TempDir
    @JvmField
    var tempDir: Path? = null
    
    var mockOcr: MockedStatic<Ocr>? = null
    var mockWinrarStatic: MockedStatic<Winrar>? = null

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared")
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
        
        val field = mainController.javaClass.getDeclaredField("arquivoController")
        field.isAccessible = true
        arquivoController = field.get(mainController) as AbaArquivoController

        mockMangaService = mock()
        mockComicInfoService = mock()
        mockWinRarService = mock()

        injectMock("mServiceManga", mockMangaService)
        injectMock("mServiceComicInfo", mockComicInfoService)
        injectMock("mRarService", mockWinRarService)

        val scene = Scene(root, 1024.0, 768.0)
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-password-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin"; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin"; }
                .jfx-tab-pane { -fx-skin: "javafx.scene.control.skin.TabPaneSkin"; }
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
        injectMock("mServiceManga", mockMangaService)
        injectMock("mServiceComicInfo", mockComicInfoService)
        injectMock("mRarService", mockWinRarService)

        if (mockOcr == null) {
            mockOcr = Mockito.mockStatic(Ocr::class.java)
        }
        mockOcr!!.`when`<String> { 
            Ocr.process(any(), anyString(), anyString()) 
        }.thenReturn("001-01\n002-05")

        if (mockWinrarStatic == null) {
            mockWinrarStatic = Mockito.mockStatic(Winrar::class.java)
        }

        val dir = tempDir!!.toFile()
        val origem = File(dir, "origem")
        val destino = File(dir, "destino")
        origem.mkdirs()
        destino.mkdirs()

        val zipFile = File("src/test/resources/test.zip")
        if (zipFile.exists()) {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val outFile = File(origem, entry.name)
                        if (entry.isDirectory) { outFile.mkdirs() } 
                        else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { input.copyTo(it) }
                        }
                    }
                }
            }
        }
        setupTestData()
    }

    @AfterEach
    fun tearDown() {
        mockOcr?.close()
        mockOcr = null
        mockWinrarStatic?.close()
        mockWinrarStatic = null
    }

    private fun injectMock(fieldName: String, mock: Any) {
        try {
            val field = AbaArquivoController::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(arquivoController, mock)
        } catch (e: Exception) {
            println("Erro ao injetar mock no campo $fieldName: ${e.message}")
        }
    }

    private fun setupTestData() {
        val conn = DataBase.instancia
        conn.createStatement().use { st ->
            st.execute("DELETE FROM Manga")
            st.execute("INSERT INTO Manga (nome, volume, capitulo) VALUES ('Test Manga', '01', '01')")
        }
    }

    private fun setupPastas(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtPastaOrigem").queryAs(TextField::class.java).setText(File(tempDir!!.toFile(), "origem").absolutePath)
            robot.lookup("#txtPastaDestino").queryAs(TextField::class.java).setText(File(tempDir!!.toFile(), "destino").absolutePath)
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testTabAbaArquivoLoads(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(TextField::class.java)
        assertNotNull(txtPastaOrigem)
        robot.interact {
            txtPastaOrigem.setText(File(tempDir!!.toFile(), "origem").absolutePath)
            robot.lookup("#txtPastaDestino").queryAs(TextField::class.java).setText(File(tempDir!!.toFile(), "destino").absolutePath)
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertNotNull(robot.lookup("#txtVolume").queryAs(TextField::class.java))
    }

    @Test
    fun testDefaultFieldValues(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("[JPN] Manga -", robot.lookup("#txtNomePastaManga").queryAs(TextField::class.java).text)
        assertEquals("Volume 01", robot.lookup("#txtVolume").queryAs(TextField::class.java).text)
        assertEquals("Capítulo", robot.lookup("#txtNomePastaCapitulo").queryAs(TextField::class.java).text)
    }

    @Test
    fun testCheckboxesDefaultState(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(robot.lookup("#cbVerificaPaginaDupla").queryAs(JFXCheckBox::class.java).isSelected)
        assertTrue(robot.lookup("#cbCompactarArquivo").queryAs(JFXCheckBox::class.java).isSelected)
    }

    @Test
    fun testVolumeMaisIncrementsValue(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtVolume = robot.lookup("#txtVolume").queryAs(TextField::class.java)
        robot.interact { txtVolume.setText("Volume 01") }
        robot.clickOn("#btnVolumeMais")
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("Volume 02", txtVolume.text)
    }

    @Test
    fun testGerarCapitulosPopulatesArea(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact {
            robot.lookup("#txtGerarInicio").queryAs(TextField::class.java).text = "5"
            robot.lookup("#txtGerarFim").queryAs(TextField::class.java).text = "7"
        }
        robot.clickOn("#btnGerar")
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(txtAreaImportar.text.contains("5") && txtAreaImportar.text.contains("7"))
    }

    @Test
    fun testShortcutFocusArea(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        robot.clickOn("#txtVolume")
        robot.press(KeyCode.CONTROL).type(KeyCode.M).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).isFocused)
    }

    @Test
    fun testShortcutToggleExtra(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.text = "001-01"
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        robot.press(KeyCode.CONTROL).type(KeyCode.E).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(txtAreaImportar.text.contains("Extra", ignoreCase = true))
    }


    @Test
    fun testMangaConsultation(robot: FxRobot) {
        // Selecionar aba ComicInfo
        robot.interact {
            robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java).selectionModel.select(1)
        }
        WaitForAsyncUtils.waitForFxEvents()

        val mockComic = ComicInfo().apply {
            title = "Consulted Manga"
            series = "Consulted Series"
            publisher = "Consulted Publisher"
            genre = "Action"
            alternateSeries = "Alt Series"
            seriesGroup = "Group X"
            storyArc = "Arc 1"
            imprint = "Imprint A"
            notes = "Mock Notes"
        }
        whenever(mockComicInfoService.getMal(anyOrNull(), any())).thenReturn(listOf(mockComic))

        whenever(mockComicInfoService.updateMal(any(), any(), any())).thenAnswer { invocation ->
            val comic = invocation.getArgument<ComicInfo>(0)
            comic.title = "Consulted Manga"
            comic.series = "Consulted Series"
            comic.publisher = "Consulted Publisher"
            comic.genre = "Action"
            comic.alternateSeries = "Alt Series"
            comic.seriesGroup = "Group X"
            comic.storyArc = "Arc 1"
            comic.imprint = "Imprint A"
            comic.notes = "Mock Notes"
            null
        }

        robot.clickOn("#txtMalNome").write("Test")
        robot.clickOn("#btnMalConsultar")
        Thread.sleep(2000)
        WaitForAsyncUtils.waitForFxEvents()

        // Selecionar o item na tabela MAL (agora deve ter 1 item)
        val tableView = robot.lookup("#tbViewMal").queryAs(TableView::class.java)
        robot.interact { tableView.selectionModel.select(0) }
        robot.clickOn("#btnMalAplicar")
        Thread.sleep(1000)
        WaitForAsyncUtils.waitForFxEvents()

        // Validar todos os campos carregados
        assertEquals("Consulted Manga", robot.lookup("#txtTitle").queryAs(TextField::class.java).text)
        assertEquals("Consulted Series", robot.lookup("#txtSeries").queryAs(TextField::class.java).text)
        assertEquals("Consulted Publisher", robot.lookup("#txtPublisher").queryAs(TextField::class.java).text)
        assertEquals("Action", robot.lookup("#txtGenre").queryAs(TextField::class.java).text)
        assertEquals("Alt Series", robot.lookup("#txtAlternateSeries").queryAs(TextField::class.java).text)
        assertEquals("Group X", robot.lookup("#txtSeriesGroup").queryAs(TextField::class.java).text)
        assertEquals("Arc 1", robot.lookup("#txtStoryArc").queryAs(TextField::class.java).text)
        assertEquals("Imprint A", robot.lookup("#txtImprint").queryAs(TextField::class.java).text)
        assertEquals("Mock Notes", robot.lookup("#txtNotes").queryAs(JFXTextArea::class.java).text)
    }

    @Test
    fun testSugestaoOCR(robot: FxRobot) {
        setupPastas(robot)
        File(tempDir!!.toFile(), "origem/001-05").mkdirs()
        
        robot.clickOn("#btnPesquisarPastaOrigem")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(1000)

        val listView = robot.lookup("#lsVwImagens").queryAs(ListView::class.java)
        robot.interact { listView.selectionModel.select(0) }
        
        robot.clickOn("#btnOcr")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(2000)

        assertTrue(robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text.contains("001-01"))
    }

    @Test
    fun testGerarCapa(robot: FxRobot) {
        setupPastas(robot)
        File(tempDir!!.toFile(), "origem/001.jpg").createNewFile()
        
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(1000)

        val listView = robot.lookup("#lsVwImagens").queryAs(ListView::class.java)
        robot.interact { listView.selectionModel.select(0) }
        
        robot.clickOn("#btnGerarCapa")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(2000)
        
        assertNotNull(robot.lookup("#imgPreview").queryAs(javafx.scene.image.ImageView::class.java))
    }

    @Test
    fun testProcessarArquivos(robot: FxRobot) {
        setupPastas(robot)
        val origem = File(tempDir!!.toFile(), "origem/Volume 01")
        origem.mkdirs()
        File(origem, "001.jpg").createNewFile()

        robot.clickOn("#btnPesquisarPastaOrigem")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(1000)

        // IMPORTAR os arquivos para a tabela antes de processar
        robot.interact {
            robot.lookup("#txtGerarInicio").queryAs(TextField::class.java).setText("1")
            robot.lookup("#txtGerarFim").queryAs(TextField::class.java).setText("1")
        }
        robot.clickOn("#btnGerar")
        robot.clickOn("#btnImporta")
        WaitForAsyncUtils.waitForFxEvents()
        
        mockWinrarStatic!!.`when`<Boolean> { 
            Winrar.compactar(
                any<File>(), any<File>(), any<Manga>(), any<ComicInfo>(), any<MutableList<File>>(),
                any<MutableMap<String, File>>(), any<Linguagem>(), any<Boolean>(), any<Boolean>(),
                any<Boolean>(), any<Callback<Triple<Long, Long, String>, Boolean>>()
            ) 
        }.thenReturn(true)

        robot.clickOn("#btnProcessar")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(3000)

        mockWinrarStatic!!.verify({ 
            Winrar.compactar(
                any<File>(), any<File>(), any<Manga>(), any<ComicInfo>(), any<MutableList<File>>(),
                any<MutableMap<String, File>>(), any<Linguagem>(), any<Boolean>(), any<Boolean>(),
                any<Boolean>(), any<Callback<Triple<Long, Long, String>, Boolean>>()
            ) 
        }, Mockito.atLeastOnce())
    }
}
