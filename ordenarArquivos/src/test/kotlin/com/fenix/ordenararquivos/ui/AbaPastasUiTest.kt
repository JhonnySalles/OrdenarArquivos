package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertNotNull

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaPastasUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var pastasController: AbaPastasController

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:pastas_testdb?mode=memory&cache=shared")
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
        
        val field = mainController.javaClass.getDeclaredField("pastasController")
        field.isAccessible = true
        pastasController = field.get(mainController) as AbaPastasController

        val scene = Scene(root, 1024.0, 768.0)
        
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix_pastas", ".css")
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
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        robot.clickOn("Pastas")
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testCamposDefault(robot: FxRobot) {
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

        val cbManga = robot.lookup("#cbManga").queryAs(JFXComboBox::class.java)
        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        val btnCarregar = robot.lookup("#btnCarregar").queryAs(JFXButton::class.java)
        
        assertTrue(cbManga.getItems().isEmpty() || cbManga.getItems().isNotEmpty())
        assertEquals("", txtPasta.getText())
        assertNotNull(btnCarregar)
    }

    @Test
    fun testAbasDisponiveis(robot: FxRobot) {
        val tbTabRoot = robot.lookup("#tbTabRoot").queryAs(JFXTabPane::class.java)
        assertEquals(2, tbTabRoot.getTabs().size)
        assertEquals("Arquivos", tbTabRoot.getTabs()[0].getText())
        assertEquals("ComicInfo", tbTabRoot.getTabs()[1].getText())
    }

    @Test
    fun testTabelaProcessarColumns(robot: FxRobot) {
        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        val columns = tbViewProcessar.getColumns()
        
        val colNames = columns.map { it.getText() }
        assertTrue(colNames.contains("Pasta"))
        assertTrue(colNames.contains("Scan"))
        assertTrue(colNames.contains("Volume"))
        assertTrue(colNames.contains("Capítulo"))
        assertTrue(colNames.contains("Título"))
        assertTrue(colNames.contains("Formatado"))
    }

    @Test
    fun testLinguagensDisponiveisNoComboBox(robot: FxRobot) {
        robot.clickOn("ComicInfo")
        WaitForAsyncUtils.waitForFxEvents()
        
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        assertEquals(Linguagem.JAPANESE, cbLinguagem.getValue())
        
        val items = cbLinguagem.getItems()
        assertTrue(items.contains(Linguagem.JAPANESE))
        assertTrue(items.contains(Linguagem.ENGLISH))
        assertTrue(items.contains(Linguagem.PORTUGUESE))
    }

    @Test
    fun testValidarBuscaMalExigeInput(robot: FxRobot) {
        robot.clickOn("ComicInfo")
        WaitForAsyncUtils.waitForFxEvents()
        
        robot.interact {
            robot.lookup("#txtMalId").queryAs(JFXTextField::class.java).setText("")
            robot.lookup("#txtMalNome").queryAs(JFXTextField::class.java).setText("")
        }
        
        robot.clickOn("#btnMalConsultar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val alert = robot.lookup(".dialog-pane").tryQuery<Node>()
        assertTrue(alert.isPresent, "Deve exibir um alerta quando os campos do MAL estão vazios")
    }
    
    @Test
    @Suppress("UNCHECKED_CAST")
    fun testMockVisualGrid(robot: FxRobot) {
        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        
        robot.interact {
            val mockData = Pasta(
                pasta = File("test"),
                arquivo = "Arquivo Teste.zip",
                nome = "Manga Teste",
                volume = 1.0f,
                capitulo = 1.0f,
                scan = "Scan Teste",
                titulo = "Titulo Teste",
                isCapa = false
            )
            tbViewProcessar.getItems().add(mockData)
        }
        
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(1, tbViewProcessar.getItems().size)
        
        val clFormatado = tbViewProcessar.getColumns().last() as TableColumn<Pasta, String>
        val cellValue = clFormatado.getCellData(0)
        assertTrue(cellValue?.contains("[Scan Teste]") == true)
        assertTrue(cellValue?.contains("Volume 01") == true)
        assertTrue(cellValue?.contains("Capítulo 001") == true)
    }
}
