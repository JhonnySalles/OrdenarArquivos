package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaArquivoControllerTest {

    private lateinit var controller: TelaInicialController
    private lateinit var arquivoController: AbaArquivoController
    
    private val rarService: WinrarServices = mock()
    private val mangaService: MangaServices = mock()
    private val comicInfoService: ComicInfoServices = mock()
    private val sincronizacaoService: SincronizacaoServices = mock()

    private val TEMP_DIR = File(System.getProperty("user.dir"), "temp")
    private val ORIGEM_DIR = File(TEMP_DIR, "origem")
    private val DESTINO_DIR = File(TEMP_DIR, "destino")

    @Start
    private fun start(stage: Stage) {
        DataBase.isTeste = true
        val db = File(System.getProperty("user.dir"), DataBase.mDATABASE_TEST)
        if (db.exists()) db.delete()

        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        controller = loader.getController<TelaInicialController>()

        // Obtém o controller da aba via reflexão pois ele é private lateinit no TelaInicialController
        val field = controller.javaClass.getDeclaredField("arquivoController")
        field.isAccessible = true
        arquivoController = field.get(controller) as AbaArquivoController

        // Injeta mocks
        arquivoController.mServiceManga = mangaService
        arquivoController.mServiceComicInfo = comicInfoService
        arquivoController.mSincronizacao = sincronizacaoService
        arquivoController.mRarService = rarService

        val scene = Scene(root)
        controller.configurarAtalhos(scene)
        stage.scene = scene
        stage.title = "Teste AbaArquivo"
        
        stage.show()
    }

    @BeforeAll
    fun setup() {
        if (TEMP_DIR.exists()) {
            TEMP_DIR.deleteRecursively()
        }
        TEMP_DIR.mkdirs()
        ORIGEM_DIR.mkdirs()
        DESTINO_DIR.mkdirs()
    }

    @Test
    @Order(1)
    @DisplayName("Deve configurar as pastas de origem e destino corretamente")
    fun testConfiguraPastas(robot: FxRobot) {
        val txtOrigem = robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java)
        val txtDestino = robot.lookup("#txtPastaDestino").queryAs(JFXTextField::class.java)
        
        robot.interact {
            txtOrigem.text = ORIGEM_DIR.absolutePath
            txtDestino.text = DESTINO_DIR.absolutePath
        }
        
        assertEquals(ORIGEM_DIR.absolutePath, txtOrigem.text)
        assertEquals(DESTINO_DIR.absolutePath, txtDestino.text)
    }

    @Test
    @Order(2)
    @DisplayName("Deve gerar a sequência de capítulos corretamente")
    fun testGerarCapitulos(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtGerarInicio").queryAs(JFXTextField::class.java).text = "1"
            robot.lookup("#txtGerarFim").queryAs(JFXTextField::class.java).text = "5"
        }
        robot.clickOn("#btnGerar")
        
        val txtArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertEquals("001-\n002-\n003-\n004-\n005-", txtArea.text)
    }

    @Test
    @Order(3)
    @DisplayName("Deve incrementar e decrementar o volume")
    fun testVolumeButtons(robot: FxRobot) {
        val txtVolume = robot.lookup("#txtVolume").queryAs(JFXTextField::class.java)
        robot.interact { txtVolume.text = "Volume 01" }
        
        robot.clickOn("#btnVolumeMais")
        assertEquals("Volume 02", txtVolume.text)
        
        robot.clickOn("#btnVolumeMenos")
        assertEquals("Volume 01", txtVolume.text)
    }

    @Test
    @Order(4)
    @DisplayName("Deve validar o preenchimento de campos do ComicInfo")
    fun testComicInfoFields(robot: FxRobot) {
        robot.clickOn("ComicInfo")
        
        val txtTitle = robot.lookup("#txtTitle").queryAs(JFXTextField::class.java)
        val txtSeries = robot.lookup("#txtSeries").queryAs(JFXTextField::class.java)
        
        robot.clickOn(txtTitle).write("Manga de Teste")
        robot.clickOn(txtSeries).write("Serie de Teste")
        
        assertEquals("Manga de Teste", txtTitle.text)
        assertEquals("Serie de Teste", txtSeries.text)
    }

    @Test
    @Order(5)
    @DisplayName("Deve simular o carregamento de um manga via Mock")
    fun testMockMangaLoad(robot: FxRobot) {
        val mockManga = Manga(
            id = 1L,
            nome = "Manga Mock",
            volume = "Volume 01",
            capitulo = "Capítulo",
            arquivo = "Manga Mock Volume 01.cbr",
            capitulos = "001-\n002-",
            quantidade = 2
        )
        
        whenever(mangaService.find(any<Manga>())).thenReturn(mockManga)
        
        robot.interact {
            robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text = "[JPN] Manga Mock -"
        }
        
        robot.clickOn("#txtVolume")
        robot.clickOn("#txtAreaImportar")
        
        val txtArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertEquals(mockManga.capitulos, txtArea.text)
    }

    @Test
    @Order(6)
    @DisplayName("Deve limpar todos os campos")
    fun testLimparTudo(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text = "Sujeira"
            robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text = "Dados"
        }
        
        robot.clickOn("#btnLimparTudo")
        
        assertEquals("[JPN] Manga -", robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text)
        assertEquals("", robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text)
    }

    @Test
    @Order(7)
    @DisplayName("Deve importar capítulos para a tabela")
    fun testImportarCapitulos(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text = "001-001\n002-002"
        }
        
        robot.clickOn("#btnImportar")
        
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(javafx.scene.control.TableView::class.java)
        assertEquals(2, tbViewTabela.items.size)
    }

    @Test
    @Order(8)
    @DisplayName("Deve sinalizar erro ao validar sem pasta de origem")
    fun testValidacaoSemOrigem(robot: FxRobot) {
        robot.interact {
            val fOrigem = arquivoController.javaClass.getDeclaredField("mCaminhoOrigem")
            fOrigem.isAccessible = true
            fOrigem.set(arquivoController, null)
        }
        
        robot.clickOn("#btnProcessar")
        
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java)
        assertEquals(javafx.scene.paint.Color.RED, txtPastaOrigem.unFocusColor)
    }

    @Test
    @Order(9)
    @DisplayName("Deve disparar processamento via atalho Ctrl+Espaço")
    fun testShortcutCtrlEspaco(robot: FxRobot) {
        robot.clickOn("#txtNomePastaManga")
        robot.press(javafx.scene.input.KeyCode.CONTROL).type(javafx.scene.input.KeyCode.SPACE).release(javafx.scene.input.KeyCode.CONTROL)
        
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java)
        assertEquals(javafx.scene.paint.Color.RED, txtPastaOrigem.unFocusColor)
    }

    @Test
    @Order(10)
    @DisplayName("Deve extrair números de numerais japoneses corretamente")
    fun testExtractNumberFromJapanese() {
        val method = arquivoController.javaClass.getDeclaredMethod("extractNumberFromJapanese", String::class.java)
        method.isAccessible = true
        
        assertEquals("2", method.invoke(arquivoController, "二"))
        assertEquals("10", method.invoke(arquivoController, "十"))
        assertEquals("2.1", method.invoke(arquivoController, "２．１"))
        assertEquals("123", method.invoke(arquivoController, "123"))
    }

    @Test
    @Order(11)
    @DisplayName("Deve reprocessar capítulos com preenchimento 000")
    fun testReprocessarCapitulos(robot: FxRobot) {
        val txtArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        val txtInicio = robot.lookup("#txtGerarInicio").queryAs(JFXTextField::class.java)
        val txtFim = robot.lookup("#txtGerarFim").queryAs(JFXTextField::class.java)
        
        robot.interact {
            txtInicio.text = "1"
            txtFim.text = "3"
            txtArea.text = "old-01|Tag1\nold-02|Tag2"
        }
        
        val method = arquivoController.javaClass.getDeclaredMethod("reprocessarCapitulos")
        method.isAccessible = true
        robot.interact { method.invoke(arquivoController) }
        
        val lines = txtArea.text.split("\n")
        assertEquals(3, lines.size)
        assertEquals("001-01|Tag1", lines[0])
        assertEquals("002-02|Tag2", lines[1])
        assertEquals("003-000", lines[2])
    }

    @Test
    @Order(12)
    @DisplayName("Deve processar arquivo RAR via drop simulado (Asíncrono)")
    fun testProcessaArquivoRar(robot: FxRobot) {
        val rarFile = File(TEMP_DIR, "Manga Teste Volume 01.rar")
        rarFile.createNewFile()

        whenever(rarService.listarConteudo(any())).thenReturn(listOf("Manga Teste/001.jpg", "Manga Teste/ComicInfo.xml"))
        whenever(mangaService.sugestao(any())).thenReturn(listOf("Manga Teste"))

        val method = arquivoController.javaClass.getDeclaredMethod("processaArquivoRar", File::class.java)
        method.isAccessible = true

        robot.interact {
            method.invoke(arquivoController, rarFile)
        }

        // Aguarda o Task (asíncrono)
        val startTime = System.currentTimeMillis()
        while (controller.rootProgress.progress < 1.0 && System.currentTimeMillis() - startTime < 5000) {
            Thread.sleep(100)
        }

        val txtNomePastaManga = robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java)
        val txtNomePastaCapitulo = robot.lookup("#txtNomePastaCapitulo").queryAs(JFXTextField::class.java)

        assertEquals(" Manga Teste -", txtNomePastaManga.text)
        assertEquals("Capítulo", txtNomePastaCapitulo.text)
        assertEquals(1.0, controller.rootProgress.progress, 0.01)
    }

    @Test
    @Order(13)
    @DisplayName("Deve processar arquivo XML arrastado")
    fun testProcessaArquivoXml(robot: FxRobot) {
        val xmlFile = File(TEMP_DIR, "ComicInfo.xml")
        xmlFile.writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <ComicInfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
              <Title>Metadado XML</Title>
              <Series>Serie XML</Series>
              <Number>1</Number>
              <MalId>12345</MalId>
            </ComicInfo>
        """.trimIndent())

        val method = arquivoController.javaClass.getDeclaredMethod("processaArquivoXml", File::class.java)
        method.isAccessible = true

        robot.interact {
            method.invoke(arquivoController, xmlFile)
        }
        
        // Aguarda possíveis runLater
        WaitForAsyncUtils.waitForFxEvents()

        val txtTitle = robot.lookup("#txtTitle").queryAs(JFXTextField::class.java)
        val txtSeries = robot.lookup("#txtSeries").queryAs(JFXTextField::class.java)
        val txtMalId = robot.lookup("#txtMalId").queryAs(JFXTextField::class.java)

        assertEquals("Metadado XML", txtTitle.text)
        assertEquals("Serie XML", txtSeries.text)
        assertEquals("12345", txtMalId.text)
    }
}
