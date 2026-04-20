package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.*
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Tag("E2E")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaArquivoE2EFlowTest : BaseTest() {

    @TempDir lateinit var tempDir: Path

    private lateinit var stage: Stage
    private lateinit var mainController: TelaInicialController
    private lateinit var arquivoController: AbaArquivoController
    private lateinit var tabContent: Node

    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()
    private val mockSincronizacao = mock<SincronizacaoServices>()
    private val mockWinrar = mock<WinrarServices>()

    @Start
    fun start(stage: Stage) {
        this.stage = stage
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        // Extrai o controller via reflexão
        val field = mainController.javaClass.getDeclaredField("arquivoController")
        field.isAccessible = true
        arquivoController = field.get(mainController) as AbaArquivoController

        // Injeta mocks no controller via reflexão
        injectMocksInternal(arquivoController)

        val scene = Scene(root, 1200.0, 800.0)
        applyJFoenixFix(scene)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
        stage.toFront()

        // Seleciona a aba Arquivo
        val mainTabPane = mainController.rootTab
        val tabField = mainController.javaClass.getDeclaredField("tbTabArquivo")
        tabField.isAccessible = true
        val tab = tabField.get(mainController) as Tab
        Platform.runLater { mainTabPane.selectionModel.select(tab) }
        WaitForAsyncUtils.waitForFxEvents()
        
        tabContent = tab.content
        
        // Inicializar o sistema de notificações
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = root
    }

    private fun injectMocksInternal(controller: AbaArquivoController) {
        val fields =
                mapOf(
                        "mServiceManga" to mockMangaService,
                        "mServiceComicInfo" to mockComicInfoService,
                        "mSincronizacao" to mockSincronizacao,
                        "mRarService" to mockWinrar
                )
        fields.forEach { (name, mock) ->
            try {
                val field = AbaArquivoController::class.java.getDeclaredField(name)
                field.isAccessible = true
                field.set(controller, mock)
            } catch (e: Exception) {}
        }
    }

    @BeforeEach
    fun setUp() {
        Ocr.isTeste = true
        Ocr.testSuggestion = "001-5|Anotação 01\n002-10|Capítulo de teste\n003-15|Outro de teste\nExtra 01-20|Extra 01"
        
        Mockito.reset(mockMangaService, mockComicInfoService, mockSincronizacao, mockWinrar)
        whenever(mockMangaService.find(any(), any())).thenReturn(null)
        whenever(mockComicInfoService.find(any(), anyOrNull())).thenReturn(null)
    }

    private fun createMockImages(directory: File, count: Int) {
        for (i in 1..count) {
            val name = String.format("%03d.jpg", i)
            File(directory, name).writeText("fake image content $i")
        }
    }

    @Test
    @Order(1)
    fun testFullFlowAbaArquivo(robot: FxRobot) {
        // 1. Preparar pastas físicas e 20 imagens
        val sourceDir = tempDir.resolve("source").toFile().apply { mkdirs() }
        val destDir = tempDir.resolve("dest").toFile().apply { mkdirs() }
        createMockImages(sourceDir, 20)

        // 2. Configurar caminhos no controller
        robot.clickOn(robot.from(tabContent).lookup("#txtPastaOrigem").query<Node>())
        robot.write(sourceDir.absolutePath)
        robot.type(KeyCode.ENTER)
        
        robot.clickOn(robot.from(tabContent).lookup("#txtPastaDestino").query<Node>())
        robot.write(destDir.absolutePath)
        robot.type(KeyCode.ENTER)

        // Aguardar o carregamento da lista de imagens
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { 
            val listView = robot.from(tabContent).lookup("#lsVwImagens").queryAs(com.jfoenix.controls.JFXListView::class.java)
            listView.items != null && listView.items.size >= 2 
        }

        // 3. Preencher dados do mangá
        robot.interact { 
            stage.requestFocus() 
            robot.from(tabContent).lookup("#cbLinguagem").queryAs(com.jfoenix.controls.JFXComboBox::class.java).value = 
                    com.fenix.ordenararquivos.model.enums.Linguagem.JAPANESE
            robot.from(tabContent).lookup("#txtNomePastaManga").queryAs(com.jfoenix.controls.JFXTextField::class.java).text = "[JPN] One Piece -"
            robot.from(tabContent).lookup("#txtVolume").queryAs(com.jfoenix.controls.JFXTextField::class.java).text = "01"
        }

        // 4. Fluxo de Arquivos - Escopo
        val tabArquivos = arquivoController.javaClass.getDeclaredField("tbTabArquivo_Arquivos").apply { isAccessible = true }.get(arquivoController) as Tab
        val contentArquivos = tabArquivos.content

        val lsVwImagensScoped = robot.from(contentArquivos).lookup("#lsVwImagens").queryAs(com.jfoenix.controls.JFXListView::class.java)
        val txtAreaImportar = robot.from(contentArquivos).lookup("#txtAreaImportar").queryAs(com.jfoenix.controls.JFXTextArea::class.java)
        val tbViewTabela = robot.from(contentArquivos).lookup("#tbViewTabela").queryAs(TableView::class.java)
        val btnImportar = robot.from(contentArquivos).lookup("#btnImportar").queryAs(com.jfoenix.controls.JFXButton::class.java)

        // Seleção de capa
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(0).query<Node>(), MouseButton.PRIMARY)

        // Alt + Clique (Capa completa)
        robot.press(KeyCode.ALT)
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(6).query<Node>(), MouseButton.PRIMARY)
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(5).query<Node>(), MouseButton.PRIMARY)
        robot.release(KeyCode.ALT)

        // 5. Geração inicial via Início/Fim
        robot.clickOn(robot.from(contentArquivos).lookup("#txtGerarInicio").query<Node>(), MouseButton.PRIMARY).write("1")
        robot.clickOn(robot.from(contentArquivos).lookup("#txtGerarFim").query<Node>(), MouseButton.PRIMARY).write("3")
        robot.clickOn(robot.from(contentArquivos).lookup("#btnGerar").query<Node>(), MouseButton.PRIMARY)

        // 6. Shift + Clique (Sumário / OCR)
        robot.press(KeyCode.SHIFT)
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(2).query<Node>(), MouseButton.PRIMARY)
        robot.release(KeyCode.SHIFT)
        
        // 7. Aguardar o popup de sugestão
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) {
            robot.lookup(".jfx-autocomplete-popup").queryAll<Node>().isNotEmpty()
        }
        
        robot.type(KeyCode.DOWN).type(KeyCode.ENTER)
        WaitForAsyncUtils.waitForFxEvents()

        // 7. Edições na TextArea
        robot.clickOn(txtAreaImportar)
        robot.type(KeyCode.END)
        robot.press(KeyCode.CONTROL).type(KeyCode.D).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        robot.press(KeyCode.CONTROL).type(KeyCode.E).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        robot.interact {
            val lines = txtAreaImportar.text.split("\n").toMutableList()
            if (lines.isNotEmpty()) {
                lines[lines.size - 1] = "Extra 01-20|Extra 01"
            }
            txtAreaImportar.text = lines.joinToString("\n")
        }

        robot.clickOn(btnImportar, MouseButton.PRIMARY)

        // Adicionar Extra 02
        robot.clickOn(txtAreaImportar)
        robot.interact { 
            txtAreaImportar.text = txtAreaImportar.text + "\nExtra 02-20|Extra 02"
            txtAreaImportar.positionCaret(txtAreaImportar.text.length)
        }
        robot.press(KeyCode.CONTROL).type(KeyCode.ENTER).release(KeyCode.CONTROL)

        // Validar Grid
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(tbViewTabela.items.size >= 5)

        // 8. Fluxo Comic Info
        val tabComicInfo = arquivoController.javaClass.getDeclaredField("tbTabArquivo_ComicInfo").apply { isAccessible = true }.get(arquivoController) as Tab
        val contentComicInfo = tabComicInfo.content
        
        robot.interact {
            robot.from(tabContent).lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java).selectionModel.select(tabComicInfo)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        val txtMalNome = robot.from(contentComicInfo).lookup("#txtMalNome").queryAs(JFXTextField::class.java)
        val btnMalConsultar = robot.from(contentComicInfo).lookup("#btnMalConsultar").queryAs(JFXButton::class.java)
        val tbViewMal = robot.from(contentComicInfo).lookup("#tbViewMal").queryAs(TableView::class.java)

        robot.clickOn(txtMalNome, MouseButton.PRIMARY)
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("One Piece")
        
        val mockMangaMal = mock<dev.katsute.mal4j.manga.Manga>()
        val fakeMal = com.fenix.ordenararquivos.model.entities.comicinfo.Mal(1L, "One Piece", "Desc", null, null, mockMangaMal)
        whenever(mockComicInfoService.getMal(anyOrNull(), anyOrNull())).thenReturn(listOf(fakeMal))
        whenever(mockComicInfoService.find(any(), anyOrNull())).thenReturn(com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo().apply {
            comic = "One Piece"
            idMal = 1L
        })

        robot.clickOn(btnMalConsultar, MouseButton.PRIMARY)
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbViewMal.items.isNotEmpty() }
        
        val btnMalAplicar = robot.from(contentComicInfo).lookup("#btnMalAplicar").queryAs(JFXButton::class.java)
        robot.interact { tbViewMal.selectionModel.select(0) }
        WaitForAsyncUtils.waitForFxEvents()
        robot.clickOn(btnMalAplicar, MouseButton.PRIMARY)
        
        robot.interact {
            robot.from(contentComicInfo).lookup("#cbAgeRating").queryAs(JFXComboBox::class.java).selectionModel.selectFirst()
            robot.from(tabContent).lookup("#cbVerificaPaginaDupla").queryAs(JFXCheckBox::class.java).isSelected = true
        }

        // 9. Processamento Final
        robot.interact {
            robot.from(tabContent).lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java).selectionModel.select(0)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        whenever(mockWinrar.compactar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(true)
        whenever(mockWinrar.insereArquivo(any<File>(), any<File>())).thenReturn(true)

        robot.press(KeyCode.CONTROL).type(KeyCode.SPACE).release(KeyCode.CONTROL)

        // Aguardar conclusão
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { 
            !robot.from(tabContent).lookup("#btnProcessar").queryAs(JFXButton::class.java).text.equals("Cancelar", ignoreCase = true) 
        }

        // 10. Segunda execução
        val destDir2 = tempDir.resolve("dest2").toFile().apply { mkdirs() }
        robot.clickOn(robot.from(tabContent).lookup("#txtPastaDestino").query<Node>())
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write(destDir2.absolutePath)
        robot.type(KeyCode.ENTER)
        
        robot.clickOn(robot.from(tabContent).lookup("#btnProcessar").query<Node>(), MouseButton.PRIMARY)
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { 
            !robot.from(tabContent).lookup("#btnProcessar").queryAs(JFXButton::class.java).text.equals("Cancelar", ignoreCase = true) 
        }

        verify(mockWinrar, atLeastOnce()).compactar(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()
        )
    }
}
