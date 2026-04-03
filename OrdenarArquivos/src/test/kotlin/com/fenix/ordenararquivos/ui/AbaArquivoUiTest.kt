package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.process.Ocr
import javafx.fxml.FXMLLoader
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
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.test.assertNotNull

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaArquivoUiTest {

    private lateinit var mainController: TelaInicialController
    private lateinit var arquivoController: AbaArquivoController
    private val tempDir = File("temp_ui_test")
    private lateinit var mockOcr: MockedStatic<Ocr>

    @Start
    fun start(stage: Stage) {
        DataBase.isTeste = true
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()
        
        // Access private controller
        val field = mainController.javaClass.getDeclaredField("arquivoController")
        field.isAccessible = true
        arquivoController = field.get(mainController) as AbaArquivoController

        val scene = Scene(root)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp() {
        // Mock OCR
        mockOcr = Mockito.mockStatic(Ocr::class.java)
        mockOcr.`when`<String> { 
            Ocr.process(Mockito.any(), anyString(), anyString()) 
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
        // Basic check if components are present
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(TextField::class.java)
        assertNotNull(txtPastaOrigem)
        
        robot.interact {
            txtPastaOrigem.text = File(tempDir, "origem").absolutePath
            robot.lookup("#txtPastaDestino").queryAs(TextField::class.java).text = File(tempDir, "destino").absolutePath
        }
        
        // Verify if some initial values are loaded
        val txtVolume = robot.lookup("#txtVolume").queryAs(TextField::class.java)
        assertNotNull(txtVolume)
    }
}
