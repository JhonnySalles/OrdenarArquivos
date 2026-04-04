package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.process.Ocr
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXPasswordField
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.stage.Window
import javafx.collections.ObservableList
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.io.FileOutputStream
import java.sql.DriverManager
import java.util.zip.ZipFile
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXTabPane
import com.fenix.ordenararquivos.TestStatusListener
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaArquivoUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var arquivoController: AbaArquivoController
    private val tempDir = File("temp_ui_test")
    private lateinit var mockOcr: MockedStatic<Ocr>

    // Static keepAlive for SQLite memory DB
    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            // Force DB initialization before any UI test
            DataBase.isTeste = true
            DataBase.closeConnection()
            // Using shared cache and mode=memory to persist between connections in the same process
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared")
            DataBase.instancia // Runs Flyway
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
        
        // Access private controller via reflection for testing
        val field = mainController.javaClass.getDeclaredField("arquivoController")
        field.isAccessible = true
        arquivoController = field.get(mainController) as AbaArquivoController

        val scene = Scene(root, 1024.0, 768.0)
        
        // Workaround for JFoenix NPE: replace problematic JFoenix skins with standard ones for tests
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-password-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin"; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin"; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainController.configurarAtalhos(scene)
        stage.scene = scene
        
        // Ensure UI is ready
        WaitForAsyncUtils.waitForFxEvents()
        
        stage.show()
        stage.toFront()
    }

    @BeforeEach
    fun setUp() {
        // Mock OCR with Kotlin-friendly matchers (bypassing non-null checks)
        mockOcr = Mockito.mockStatic(Ocr::class.java)
        mockOcr.`when`<String> { 
            Ocr.process(
                Mockito.any(File::class.java) ?: File(""), 
                Mockito.anyString() ?: "", 
                Mockito.anyString() ?: ""
            ) 
        }.thenReturn("001-01\n002-05")

        // Prepare temporary directory
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()
        val origem = File(tempDir, "origem")
        val destino = File(tempDir, "destino")
        origem.mkdirs()
        destino.mkdirs()

        // Extract test.zip
        val zipFile = File("src/test/resources/test.zip")
        if (zipFile.exists()) {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val outFile = File(origem, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }

        // Populate Database for testing
        setupTestData()
    }

    @AfterEach
    fun tearDown() {
        if (this::mockOcr.isInitialized) {
            mockOcr.close()
        }
        if (tempDir.exists()) tempDir.deleteRecursively()
    }

    private fun setupTestData() {
        val conn = DataBase.instancia
        conn.createStatement().use { st ->
            st.execute("DELETE FROM Manga")
            st.execute("INSERT INTO Manga (nome, volume, capitulo) VALUES ('Test Manga', '01', '01')")
        }
    }

    @Test
    fun testTabAbaArquivoLoads(robot: FxRobot) {
        // Wait for JavaFX events to settle
        WaitForAsyncUtils.waitForFxEvents()

        // Basic check if components are present
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(TextField::class.java)
        assertNotNull(txtPastaOrigem)
        
        robot.interact {
            txtPastaOrigem.text = File(tempDir, "origem").absolutePath
            val txtPastaDestino = robot.lookup("#txtPastaDestino").queryAs(TextField::class.java)
            txtPastaDestino.text = File(tempDir, "destino").absolutePath
        }
        
        // Wait again for any UI updates
        WaitForAsyncUtils.waitForFxEvents()

        // Verify if some initial values are loaded
        val txtVolume = robot.lookup("#txtVolume").queryAs(TextField::class.java)
        assertNotNull(txtVolume)
    }

    @Test
    fun testDefaultFieldValues(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("[JPN] Manga -", robot.lookup("#txtNomePastaManga").queryAs(TextField::class.java).text, "NomePastaManga com valor inesperado. Valor atual: ${robot.lookup("#txtNomePastaManga").queryAs(TextField::class.java).text}")
        assertEquals("Volume 01", robot.lookup("#txtVolume").queryAs(TextField::class.java).text, "Volume com valor inesperado. Valor atual: ${robot.lookup("#txtVolume").queryAs(TextField::class.java).text}")
        assertEquals("Capítulo", robot.lookup("#txtNomePastaCapitulo").queryAs(TextField::class.java).text, "NomePastaCapitulo com valor inesperado. Valor atual: ${robot.lookup("#txtNomePastaCapitulo").queryAs(TextField::class.java).text}")
        assertEquals("-", robot.lookup("#txtSeparadorPagina").queryAs(TextField::class.java).text, "SeparadorPagina com valor inesperado. Valor atual: ${robot.lookup("#txtSeparadorPagina").queryAs(TextField::class.java).text}")
        assertEquals("|", robot.lookup("#txtSeparadorCapitulo").queryAs(TextField::class.java).text, "SeparadorCapitulo com valor inesperado. Valor atual: ${robot.lookup("#txtSeparadorCapitulo").queryAs(TextField::class.java).text}")
    }

    @Test
    fun testCheckboxesDefaultState(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(robot.lookup("#cbVerificaPaginaDupla").queryAs(JFXCheckBox::class.java).isSelected)
        assertTrue(robot.lookup("#cbCompactarArquivo").queryAs(JFXCheckBox::class.java).isSelected)
        assertTrue(robot.lookup("#cbMesclarCapaTudo").queryAs(JFXCheckBox::class.java).isSelected)
        assertTrue(robot.lookup("#cbAjustarMargemCapa").queryAs(JFXCheckBox::class.java).isSelected)
        assertTrue(robot.lookup("#cbOcrSumario").queryAs(JFXCheckBox::class.java).isSelected)
        assertTrue(robot.lookup("#cbGerarCapitulo").queryAs(JFXCheckBox::class.java).isSelected)
    }

    @Test
    fun testSimularPastaFieldIsPresent(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        assertNotNull(robot.lookup("#txtSimularPasta").queryAs(TextField::class.java))
    }

    @Test
    fun testVolumeMaisIncrementsValue(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtVolume = robot.lookup("#txtVolume").queryAs(TextField::class.java)
        robot.interact { txtVolume.text = "Volume 01" }
        
        robot.clickOn("#btnVolumeMais")
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("Volume 02", txtVolume.text)
    }

    @Test
    fun testVolumeMenosDecrementsValue(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtVolume = robot.lookup("#txtVolume").queryAs(TextField::class.java)
        robot.interact { txtVolume.text = "Volume 05" }
        
        robot.clickOn("#btnVolumeMenos")
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("Volume 04", txtVolume.text, "Volume com valor inesperado. Valor atual: ${txtVolume.text}")
        
        robot.interact { txtVolume.text = "Volume 00" }
        robot.clickOn("#btnVolumeMenos")
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("Volume 00", txtVolume.text, "Volume com valor inesperado. Valor atual: ${txtVolume.text}")
    }

    @Test
    fun testGerarCapitulosPopulatesArea(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtGerarInicio = robot.lookup("#txtGerarInicio").queryAs(TextField::class.java)
        val txtGerarFim = robot.lookup("#txtGerarFim").queryAs(TextField::class.java)
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)

        robot.interact {
            txtGerarInicio.text = "5"
            txtGerarFim.text = "7"
        }
        
        robot.clickOn("#btnGerar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val content = txtAreaImportar.text
        assertTrue(content.contains("5"))
        assertTrue(content.contains("6"))
        assertTrue(content.contains("7"))
    }

    @Test
    fun testBtnLimparTudoClearsAllFields(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(TextField::class.java)
        val txtPastaDestino = robot.lookup("#txtPastaDestino").queryAs(TextField::class.java)
        val txtNomePastaManga = robot.lookup("#txtNomePastaManga").queryAs(TextField::class.java)

        robot.interact {
            txtPastaOrigem.text = "C:\\test\\origem"
            txtPastaDestino.text = "C:\\test\\destino"
            txtNomePastaManga.text = "Manga Editado"
        }
        
        robot.clickOn("#btnLimparTudo")
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("", txtPastaOrigem.text, "PastaOrigem com valor inesperado. Valor atual: ${txtPastaOrigem.text}")
        assertEquals("", txtPastaDestino.text, "PastaDestino com valor inesperado. Valor atual: ${txtPastaDestino.text}")
        assertEquals("[JPN] Manga -", txtNomePastaManga.text, "NomePastaManga com valor inesperado. Valor atual: ${txtNomePastaManga.text}")
    }

    @Test
    fun testTableViewIsVisible(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val tabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertNotNull(tabela)
        assertEquals(4, tabela.columns.size)
    }

    @Test
    fun testAccordionArquivosExists(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val acd = robot.lookup("#acdArquivos").queryAs(Accordion::class.java)
        assertNotNull(acd)
        assertEquals(2, acd.panes.size)
    }

    @Test
    fun testTabPaneHasThreeTabs(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val tabPane = robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java)
        assertNotNull(tabPane)
        assertEquals(3, tabPane.tabs.size, "TabRoot com quantidade de tabs inesperada. Valor atual: ${tabPane.tabs.size}")
        assertEquals("Arquivos", tabPane.tabs[0].text, "Arquivos com valor inesperado. Valor atual: ${tabPane.tabs[0].text}")
        assertEquals("ComicInfo", tabPane.tabs[1].text, "ComicInfo com valor inesperado. Valor atual: ${tabPane.tabs[1].text}")
        assertEquals("Capas", tabPane.tabs[2].text, "Capas com valor inesperado. Valor atual: ${tabPane.tabs[2].text}")
    }

    @Test
    fun testShortcutFocusArea(robot: FxRobot) {
        Thread.sleep(1000) // Delay solicitado para estabilização
        WaitForAsyncUtils.waitForFxEvents()

        // Garante que a janela está ativa e no topo para receber foco
        robot.interact {
            val stage = robot.listWindows().filterIsInstance<Stage>().firstOrNull()
            if (stage != null && !stage.isFocused) {
                stage.toFront()
                stage.requestFocus()
            }
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        robot.clickOn("#txtVolume") // Move focus away
        
        robot.press(KeyCode.CONTROL).type(KeyCode.M).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertTrue(txtAreaImportar.isFocused)
    }

    @Test
    fun testShortcutSuggestionMenu(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)

        // Acessa o campo privado mSugestao via Reflection total para evitar erros de módulo (supertipos protegidos)
        val field = arquivoController.javaClass.getDeclaredField("mSugestao")
        field.isAccessible = true
        val mSugestaoObj = field.get(arquivoController)

        // Obtém a lista de sugestões via reflection
        val getSuggestionsMethod = mSugestaoObj.javaClass.getMethod("getSuggestions")
        val suggestions = getSuggestionsMethod.invoke(mSugestaoObj) as MutableList<Any?>

        robot.interact {
            suggestions.clear()
            suggestions.add("Volume 01-01|Capítulo 01")
            txtAreaImportar.requestFocus()
        }

        robot.press(KeyCode.CONTROL).type(KeyCode.S).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()

        // Verifica se o popup está visível utilizando reflection
        val isShowingMethod = mSugestaoObj.javaClass.getMethod("isShowing")
        val isShowing = isShowingMethod.invoke(mSugestaoObj) as Boolean
        assertTrue(isShowing, "O menu de sugestão deve estar visível após o atalho")
    }

    @Test
    fun testShortcutSortLines(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "002-01\n001-01") 
            txtAreaImportar.requestFocus()
        }
        
        robot.press(KeyCode.CONTROL).type(KeyCode.O).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("001-01\n002-01", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutDuplicateLine(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-01") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        robot.press(KeyCode.CONTROL).type(KeyCode.D).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("001-01\n001-01", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutDeleteTags(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-01|Capítulo 01") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        robot.press(KeyCode.CONTROL).type(KeyCode.T).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("001-01", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutSubChapter(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-01") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        robot.press(KeyCode.CONTROL).type(KeyCode.DIGIT5).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        // Ctrl + 5 on 001-01 should result in 001.5-01 based on logic in line 2663
        assertEquals("001.5-01", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutToggleExtra(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-01") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        robot.press(KeyCode.CONTROL).type(KeyCode.E).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertTrue(txtAreaImportar.text.contains("Extra", ignoreCase = true), "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutMoveLineUpDown(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "L1\nL2") 
            txtAreaImportar.requestFocus()
            // Select L2 (end of text)
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        // Ctrl + Shift + UP to move L2 up
        robot.press(KeyCode.CONTROL, KeyCode.SHIFT).type(KeyCode.UP).release(KeyCode.SHIFT, KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("L2\nL1", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutInvertPageChapter(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-003\n002-001") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(0)
        }
        
        // Ctrl + Shift + LEFT/RIGHT to invert
        robot.press(KeyCode.CONTROL, KeyCode.SHIFT).type(KeyCode.LEFT).release(KeyCode.SHIFT, KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("003-001\n002-001", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutInvertAll(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "002-001\n004-003\n006-005") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(0)
        }
        
        // Ctrl + Shift + Alt + LEFT to invert all
        robot.press(KeyCode.CONTROL, KeyCode.SHIFT, KeyCode.ALT).type(KeyCode.LEFT).release(KeyCode.ALT, KeyCode.SHIFT, KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("001-002\n003-004\n005-006", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutInsertNumbering(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-01|TAG") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(0)
        }
        
        // Ctrl + Alt + 2 to change chapter to 2
        robot.press(KeyCode.CONTROL, KeyCode.ALT).type(KeyCode.NUMPAD2).release(KeyCode.ALT, KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("2-01|TAG", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutMoveTagUpDown(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "001-05\n002-01|Description 2") 
            txtAreaImportar.requestFocus()
            // Select second line
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        // Alt + Shift + UP to move T2 tag up
        robot.press(KeyCode.ALT, KeyCode.SHIFT).type(KeyCode.UP).release(KeyCode.SHIFT, KeyCode.ALT)
        WaitForAsyncUtils.waitForFxEvents()
        
        // As per AbaArquivoController:2886-2887, moves tag to previous line
        assertEquals("001-05|Description 2\n002-01", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")

        // Ctrl + Alt + DOWN to move T2 tag down
        robot.press(KeyCode.ALT, KeyCode.SHIFT).type(KeyCode.DOWN).release(KeyCode.SHIFT, KeyCode.ALT)
        WaitForAsyncUtils.waitForFxEvents()
        
        // As per AbaArquivoController:2886-2887, moves tag to next line
        assertEquals("001-05\n002-01|Description 2", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }

    @Test
    fun testShortcutMoveLineWithAlt(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { 
            txtAreaImportar.replaceText(0, txtAreaImportar.length, "L1\nL2") 
            txtAreaImportar.requestFocus()
            txtAreaImportar.positionCaret(txtAreaImportar.length)
        }
        
        // Ctrl + Alt + UP also moves line (line 2679 in controller)
        robot.press(KeyCode.CONTROL, KeyCode.ALT).type(KeyCode.UP).release(KeyCode.ALT, KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("L2\nL1", txtAreaImportar.text, "AreaImportar com valor inesperado. Valor atual: ${txtAreaImportar.text}")
    }
}
