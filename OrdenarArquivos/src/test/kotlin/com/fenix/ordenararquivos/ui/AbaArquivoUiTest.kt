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
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
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
import kotlin.test.assertNotNull

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
}
