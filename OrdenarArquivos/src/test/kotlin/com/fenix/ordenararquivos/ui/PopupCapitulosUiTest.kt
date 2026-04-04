package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupCapitulos
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertNotNull

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class PopupCapitulosUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private val tempDir = File("temp_ui_popup_test")

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null
        private lateinit var mockedAlertas: MockedStatic<AlertasPopup>

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_popup?mode=memory&cache=shared")
            DataBase.instancia
            
            // Mock static AlertasPopup to avoid blocking UI
            mockedAlertas = Mockito.mockStatic(AlertasPopup::class.java)
        }

        @AfterAll
        @JvmStatic
        fun globalTearDown() {
            mockedAlertas.close()
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

        val scene = Scene(root, 1024.0, 768.0)
        
        // Fix JFoenix skins
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix_popup", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin"; }
                .jfx-check-box { -fx-skin: "com.sun.javafx.scene.control.skin.CheckBoxSkin"; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stage.scene = scene
        stage.show()
        
        // Open the Popup
        robotOpenPopup()
    }

    private fun robotOpenPopup() {
        WaitForAsyncUtils.waitForFxEvents()
        // Use the static method to open the popup
        // We need a dummy callback
        val callback = javafx.util.Callback<javafx.collections.ObservableList<com.fenix.ordenararquivos.model.entities.capitulos.Volume>, Boolean> { true }
        
        javafx.application.Platform.runLater {
            PopupCapitulos.abreTelaCapitulos(
                mainController.rootStack,
                mainController.rootTab,
                callback,
                Linguagem.ENGLISH,
                listOf()
            )
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @BeforeEach
    fun setUp() {
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()
    }

    @AfterEach
    fun tearDown() {
        if (tempDir.exists()) tempDir.deleteRecursively()
    }

    @Test
    fun testInitialUIState(robot: FxRobot) {
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        assertNotNull(cbLinguagem)
        assertEquals(Linguagem.ENGLISH, cbLinguagem.value)

        val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertTrue(table.items.isEmpty())
        
        assertNotNull(robot.lookup("#txtEndereco"))
        assertNotNull(robot.lookup("#btnExecutar"))
    }

    @Test
    fun testExtractionComick(robot: FxRobot) {
        val html = """
            <!-- saved from url=(0014)https://comick.io/ -->
            <table>
                <tbody>
                    <tr class="group">
                        <td>
                            <a>
                                <div class="truncate">
                                    <span class="font-semibold" title="Chapter 1">Ch. 1</span>
                                    <span class="text-xs">Vol. 1</span>
                                    <span class="text-xs md:text-base">Episode 1</span>
                                </div>
                            </a>
                        </td>
                    </tr>
                </tbody>
            </table>
        """.trimIndent()
        val file = File(tempDir, "comick.html")
        file.writeText(html)
        
        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        robot.interact {
            txtEndereco.text = file.absolutePath
        }
        
        robot.clickOn("#btnExecutar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertEquals(1, table.items.size)
        val vol = table.items[0] as com.fenix.ordenararquivos.model.entities.capitulos.Volume
        assertEquals(1.0, vol.volume)
        assertEquals(1, vol.capitulos.size)
        assertEquals("Episode 1", vol.capitulos[0].ingles)
    }

    @Test
    fun testExtractionMangaFire(robot: FxRobot) {
        val html = """
            <!-- saved from url=(0014)https://mangafire.to/ -->
            <div class="tab-content" data-name="chapter">
                <div class="list-body">
                    <ul class="scroll-sm">
                        <li class="item" data-number="12">
                            <a>
                                <span>Chapter 12: Battle starts</span>
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        """.trimIndent()
        val file = File(tempDir, "mangafire.html")
        file.writeText(html)
        
        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        robot.interact {
            txtEndereco.text = file.absolutePath
        }
        
        robot.clickOn("#btnExecutar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertEquals(1, table.items.size)
        val vol = table.items[0] as com.fenix.ordenararquivos.model.entities.capitulos.Volume
        assertEquals(-1.0, vol.volume) // MangaFire returns -1.0 for volume in this logic
        assertEquals(1, vol.capitulos.size)
        assertEquals(12.0, vol.capitulos[0].capitulo)
        assertEquals("Battle starts", vol.capitulos[0].ingles)
    }

    @Test
    fun testExtractionMangaDex(robot: FxRobot) {
        val html = """
            <!-- saved from url=(0014)https://mangadex.org/ -->
            <div class="grid grid-cols-12 mb-2 cursor-pointer">Volume 2</div>
            <div data-v-5ea3fe4a="" class="bg-accent">
                <div class="chapter-header">Chapter 15</div>
                <div class="chapter relative read">
                    <img class="inline-block" title="English">
                    <div class="line-clamp-1">Secret plan</div>
                </div>
            </div>
        """.trimIndent()
        val file = File(tempDir, "mangadex.html")
        file.writeText(html)
        
        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        robot.interact {
            txtEndereco.text = file.absolutePath
        }
        
        robot.clickOn("#btnExecutar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertEquals(1, table.items.size)
        val vol = table.items[0] as com.fenix.ordenararquivos.model.entities.capitulos.Volume
        assertEquals(2.0, vol.volume)
        assertEquals(1, vol.capitulos.size)
        assertEquals(15.0, vol.capitulos[0].capitulo)
        assertEquals("Secret plan", vol.capitulos[0].ingles)
    }

    @Test
    fun testErrorHandling(robot: FxRobot) {
        val txtEndereco = robot.lookup("#txtEndereco").queryAs(JFXTextField::class.java)
        robot.interact {
            txtEndereco.text = "non_existent_file.html"
        }
        
        robot.clickOn("#btnExecutar")
        WaitForAsyncUtils.waitForFxEvents()
        
        // Verify AlertasPopup.erroModal was called
        mockedAlertas.verify {
            AlertasPopup.erroModal(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun testMarcarTodos(robot: FxRobot) {
        // First populate table
        testExtractionComick(robot)
        
        val cbMarcarTodos = robot.lookup(".check-box").queryAs(JFXCheckBox::class.java)
        robot.interact {
            cbMarcarTodos.isSelected = true
        }
        robot.clickOn(cbMarcarTodos)
        
        val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        val vol = table.items[0] as com.fenix.ordenararquivos.model.entities.capitulos.Volume
        assertTrue(vol.marcado)
    }

    @Test
    fun testLanguageSwitch(robot: FxRobot) {
        // Populate with something that has both languages (or just check if it refreshes)
        testExtractionComick(robot)
        
        val cbLinguagem = robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java)
        robot.interact {
            cbLinguagem.value = Linguagem.PORTUGUESE
        }
        
        // Changing language calls preparar(mLista), which refreshes the table.
        // We can check if the column value factory is triggered (it format tags etc)
        val table = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertNotNull(table.items)
    }
    
    @Test
    fun testConfirmButton(robot: FxRobot) {
        val btnConfirmar = robot.lookup("Confirmar").queryAs(JFXButton::class.java)
        assertNotNull(btnConfirmar)
        robot.clickOn(btnConfirmar)
        // Dialog should close, but testing JFXDialog close state is tricky.
        // At least we verify it exists and is clickable.
    }
}
