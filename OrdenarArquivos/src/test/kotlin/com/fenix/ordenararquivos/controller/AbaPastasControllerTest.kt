package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@Tag("UI")
@ExtendWith(ApplicationExtension::class, MockitoExtension::class)
class AbaPastasControllerTest {

    private lateinit var controller: AbaPastasController

    @Mock
    lateinit var mockServiceWinrar: WinrarServices

    @Mock
    lateinit var mockServiceManga: MangaServices

    @Mock
    lateinit var mockServiceComicInfo: ComicInfoServices

    @Mock
    lateinit var mockTelaInicial: TelaInicialController

    private val tempDir = File("temp")
    private val origemDir = File(tempDir, "origem")
    private val destinoDir = File(tempDir, "destino")

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupHeadless() {
            if (System.getProperty("os.name").contains("Windows").not()) {
                System.setProperty("testfx.robot", "glass")
                System.setProperty("testfx.headless", "true")
                System.setProperty("prism.order", "sw")
                System.setProperty("glass.platform", "Monocle")
                System.setProperty("monocle.platform", "Headless")
            }
        }
    }

    @Start
    fun start(stage: Stage) {
        val fxmlUrl = javaClass.getResource("/view/AbaPastas.fxml")
        if (fxmlUrl == null) {
            throw IllegalStateException("FXML file not found: /view/AbaPastas.fxml")
        }
        val loader = FXMLLoader(fxmlUrl)
        val root: AnchorPane = loader.load()
        controller = loader.getController()

        // Mock UI elements of parent controller
        val pb = ProgressBar()
        val lbl = Label()
        `when`(mockTelaInicial.rootProgress).thenReturn(pb)
        `when`(mockTelaInicial.rootMessage).thenReturn(lbl)

        // Inject mocks
        //controller.mServiceWinrar = mockServiceWinrar
        //controller.mServiceManga = mockServiceManga
        //controller.mServiceComicInfo = mockServiceComicInfo
        controller.controllerPai = mockTelaInicial

        val scene = Scene(root)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp() {
        // Clear temp directory
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
        tempDir.mkdirs()
        origemDir.mkdirs()
        destinoDir.mkdirs()

        // Extract test.zip
        val zipStream = javaClass.getResourceAsStream("/test.zip")
        if (zipStream != null) {
            ZipInputStream(zipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(origemDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    @Test
    fun testCarregarPastas(robot: FxRobot) {
        // Mock the folder selection
        //`when`(mockServiceWinrar.selecionaPasta(null, "Selecione a pasta de origem")).thenReturn(origemDir)

        // Click on search button
        robot.clickOn("#btnPesquisarPasta")

        // Wait for FX events
        WaitForAsyncUtils.waitForFxEvents()

        // Check if path is set in text field
        val txtPasta = robot.lookup("#txtPasta").queryAs(javafx.scene.control.TextField::class.java)
        assertEquals(origemDir.absolutePath, txtPasta.text)
        
        // Mock loading folders
        /*val pastaTest = Pasta().apply {
            arquivo = "Volume 01"
            caminhoBase = origemDir.absolutePath
            isProcessar = true
        }*/
        //`when`(mockServiceWinrar.listarPastas(origemDir)).thenReturn(listOf(pastaTest))

        // Click on Load button
        robot.clickOn("#btnCarregar")
        
        // Wait for task completion
        WaitForAsyncUtils.waitForFxEvents()
        
        // We might need to wait for the background task to finish.
        // In the controller, btnCarregar starts a Thread.
        Thread.sleep(1000) 
        WaitForAsyncUtils.waitForFxEvents()

        // Verify table has items
        val tbView = robot.lookup("#tbViewProcessar").queryAs(javafx.scene.control.TableView::class.java)
        assertTrue(tbView.items.size > 0, "Table should have at least one item")
    }

    @Test
    fun testProcessaArquivoRar(robot: FxRobot) {
        val method = controller.javaClass.getDeclaredMethod("processaArquivoRar", File::class.java)
        method.isAccessible = true

        val cbManga = robot.lookup("#cbManga").queryAs(com.jfoenix.controls.JFXComboBox::class.java)

        val cenarios = listOf(
            "Manga Teste Volume 01.rar" to "Manga Teste",
            "MANGA TESTE VOLUME 01.rar" to "MANGA TESTE",
            "Outro Manga Capítulo 05.rar" to "Outro Manga",
            "Manga Chapter 10.rar" to "Manga",
            "Manga capitulo 15.rar" to "Manga",
            "Manga - 01.rar" to "Manga"
        )

        cenarios.forEach { (nomeArquivo, esperado) ->
            robot.interact {
                method.invoke(controller, File(nomeArquivo))
            }
            assertEquals(esperado, cbManga.value, "Erro no nome do mangá para: $nomeArquivo")
        }
    }
}
