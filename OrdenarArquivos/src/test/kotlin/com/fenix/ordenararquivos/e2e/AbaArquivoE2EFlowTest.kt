package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.*
import com.fenix.ordenararquivos.process.Ocr
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javafx.collections.ObservableList
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@Tag("E2E")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaArquivoE2EFlowTest : BaseTest() {

    @TempDir lateinit var tempDir: Path

    private lateinit var stage: Stage
    private lateinit var mainController: TelaInicialController
    private lateinit var arquivoController: AbaArquivoController

    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()
    private val mockSincronizacao = mock<SincronizacaoServices>()
    private val mockWinrar = mock<WinrarServices>()

    @Start
    fun start(stage: Stage) {
        this.stage = stage
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            when (controllerClass) {
                TelaInicialController::class.java ->
                        TelaInicialController().also { mainController = it }
                AbaArquivoController::class.java ->
                        AbaArquivoController().also { arquivoController = it }
                else -> controllerClass.getDeclaredConstructor().newInstance()
            }
        }
        val root: AnchorPane = loader.load()

        // Injetar mocks nos controllers via reflection se necessário ou se o factory não bastar
        // Para AbaArquivo, o TelaInicialController injeta as dependências no initialize
        // Mas podemos forçar aqui para garantir os mocks do teste.
        injectMocks(arquivoController)

        stage.scene = Scene(root, 1200.0, 800.0)
        stage.show()
        stage.toFront()

        // Garantir que a UI está pronta
        WaitForAsyncUtils.waitForFxEvents()
    }

    private fun injectMocks(controller: AbaArquivoController) {
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
            } catch (e: Exception) {
                // Algumas podem não existir dependendo da versão
            }
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

        // 2. Configurar caminhos no controller (simulando interação real)
        robot.clickOn("#txtPastaOrigem")
        robot.write(sourceDir.absolutePath)
        robot.type(KeyCode.ENTER)
        
        robot.clickOn("#txtPastaDestino")
        robot.write(destDir.absolutePath)
        robot.type(KeyCode.ENTER)

        // Aguardar o carregamento da lista de imagens
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) { 
            robot.lookup("#lsVwImagens").queryAs(JFXListView::class.java).items.size >= 2 
        }

        // 3. Preencher dados do mangá
        robot.interact { 
            stage.requestFocus() 
            robot.lookup("#cbLinguagem").queryAs(com.jfoenix.controls.JFXComboBox::class.java).value = 
                    com.fenix.ordenararquivos.model.enums.Linguagem.JAPANESE
            robot.lookup("#txtNomePastaManga").queryAs(com.jfoenix.controls.JFXTextField::class.java).text = "[JPN] One Piece -"
            robot.lookup("#txtVolume").queryAs(com.jfoenix.controls.JFXTextField::class.java).text = "01"
        }

        // 4. Seleções especiais na lista de imagens
        val lsVwImagens = robot.lookup("#lsVwImagens").queryAs(JFXListView::class.java)
        
        // 4. Fluxo de Arquivos - Escopo
        val tabArquivos = arquivoController.javaClass.getDeclaredField("tbTabArquivo_Arquivos").apply { isAccessible = true }.get(arquivoController) as Tab
        val contentArquivos = tabArquivos.content

        // Redefinir componentes locais para usar o escopo da aba
        val lsVwImagensScoped = robot.from(contentArquivos).lookup("#lsVwImagens").queryAs(com.jfoenix.controls.JFXListView::class.java)
        val txtAreaImportar = robot.from(contentArquivos).lookup("#txtAreaImportar").queryAs(com.jfoenix.controls.JFXTextArea::class.java)
        val tbViewTabela = robot.from(contentArquivos).lookup("#tbViewTabela").queryAs(TableView::class.java)
        val btnImportar = robot.from(contentArquivos).lookup("#btnImportar").queryAs(com.jfoenix.controls.JFXButton::class.java)

        // Seleção de capa (Capa simples) - Clique duplo na primeira imagem
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(0).query<Node>(), MouseButton.PRIMARY)

        // Alt + Clique (Capa completa) - Segunda imagem
        robot.press(KeyCode.ALT)
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(6).query<Node>(), MouseButton.PRIMARY)
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(5).query<Node>(), MouseButton.PRIMARY)
        robot.release(KeyCode.ALT)

        // 5. Geração inicial via Início/Fim
        robot.clickOn("#txtGerarInicio", MouseButton.PRIMARY).write("1")
        robot.clickOn("#txtGerarFim", MouseButton.PRIMARY).write("3")
        robot.clickOn("#btnGerar", MouseButton.PRIMARY)

        // 6. Shift + Clique (Sumário / OCR)
        robot.press(KeyCode.SHIFT)
        robot.doubleClickOn(robot.from(lsVwImagensScoped).lookup(".list-cell").nth(2).query<Node>(), MouseButton.PRIMARY)
        robot.release(KeyCode.SHIFT)
        
        // 7. Aguardar o popup de sugestão e selecionar
        // Aguardar o popup aparecer (o OCR mockado é rápido, mas roda em thread separada)
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) {
            robot.lookup(".jfx-autocomplete-popup").queryAll<Node>().isNotEmpty()
        }
        
        // Selecionar a primeira sugestão (que contém todo o texto mockado)
        robot.type(KeyCode.DOWN).type(KeyCode.ENTER)
        WaitForAsyncUtils.waitForFxEvents()

        // 7. Edições na TextArea (Ctrl+D, Ctrl+E)
        robot.clickOn(txtAreaImportar)
        robot.type(KeyCode.END)
        
        // Simular Ctrl+D (Duplicar)
        robot.press(KeyCode.CONTROL).type(KeyCode.D).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        // Simular Ctrl+E (Extra)
        robot.press(KeyCode.CONTROL).type(KeyCode.E).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()
        
        // Renomear a linha gerada para "Extra 01-20|Extra 01"
        robot.interact {
            val lines = txtAreaImportar.text.split("\n").toMutableList()
            if (lines.isNotEmpty()) {
                lines[lines.size - 1] = "Extra 01-20|Extra 01"
            }
            txtAreaImportar.text = lines.joinToString("\n")
        }

        // Botão Aplicar/Importar
        robot.clickOn(btnImportar, MouseButton.PRIMARY)

        // Adicionar Extra 02 via Ctrl+Enter na TextArea
        robot.clickOn(txtAreaImportar)
        robot.interact { 
            txtAreaImportar.text = txtAreaImportar.text + "\nExtra 02-20|Extra 02"
            txtAreaImportar.positionCaret(txtAreaImportar.text.length)
        }
        robot.press(KeyCode.CONTROL).type(KeyCode.ENTER).release(KeyCode.CONTROL)

        // Validar Grid
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(tbViewTabela.items.size >= 5, "Tabela deveria ter os capítulos gerados e os extras")

        // 8. Fluxo Comic Info
        val tabComicInfo = arquivoController.javaClass.getDeclaredField("tbTabArquivo_ComicInfo").apply { isAccessible = true }.get(arquivoController) as Tab
        val contentNode = tabComicInfo.content
        
        robot.interact {
            robot.lookup("#tbTabRootArquivo").queryAs(com.jfoenix.controls.JFXTabPane::class.java).selectionModel.select(tabComicInfo)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        val txtMalNome = robot.from(contentNode).lookup("#txtMalNome").queryAs(com.jfoenix.controls.JFXTextField::class.java)
        val btnMalConsultar = robot.from(contentNode).lookup("#btnMalConsultar").queryAs(com.jfoenix.controls.JFXButton::class.java)
        val tbViewMal = robot.from(contentNode).lookup("#tbViewMal").queryAs(TableView::class.java)

        robot.clickOn(txtMalNome, MouseButton.PRIMARY)
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("One Piece")
        
        // Mocking the result for MAL search
        val mockMangaMal = mock<dev.katsute.mal4j.manga.Manga>()
        val fakeMal = com.fenix.ordenararquivos.model.entities.comicinfo.Mal(1L, "One Piece", "Desc", null, null, mockMangaMal)
        
        // Mock do serviço de busca (usando anyOrNull para garantir o match independente do estado dos campos)
        whenever(mockComicInfoService.getMal(anyOrNull(), anyOrNull())).thenReturn(listOf(fakeMal))
        
        // Mock do preenchimento dos campos ao selecionar o item
        whenever(mockComicInfoService.find(any(), anyOrNull())).thenReturn(com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo().apply {
            comic = "One Piece"
            idMal = 1L
        })

        // Clica em Consultar para disparar a Task assíncrona
        robot.clickOn(btnMalConsultar, MouseButton.PRIMARY)
        
        // Aguardar o modelo de dados ser populado 
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) {
            tbViewMal.items.isNotEmpty()
        }
        
        val btnMalAplicar = robot.from(contentNode).lookup("#btnMalAplicar").queryAs(com.jfoenix.controls.JFXButton::class.java)

        // Seleciona o primeiro resultado programaticamente para garantir o foco e disparo de listeners
        robot.interact {
            tbViewMal.selectionModel.select(0)
        }
        
        // Pequena pausa para o JavaFX processar a seleção antes de aplicar
        TimeUnit.MILLISECONDS.sleep(500)
        
        robot.clickOn(btnMalAplicar, MouseButton.PRIMARY)
        
        // Preencher checkboxes e afins
        robot.interact {
            robot.lookup("#cbAgeRating").queryAs(com.jfoenix.controls.JFXComboBox::class.java).selectionModel.selectFirst()
            robot.lookup("#cbVerificaPaginaDupla").queryAs(JFXCheckBox::class.java).isSelected = true
        }

        // 9. Processamento Final (Ctrl + Espaço)
        robot.interact {
            robot.lookup("#tbTabRootArquivo").queryAs(com.jfoenix.controls.JFXTabPane::class.java).selectionModel.select(0)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        // Mock Winrar
        whenever(mockWinrar.compactar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(true)
        whenever(mockWinrar.insereArquivo(any<File>(), any<File>())).thenReturn(true)

        robot.press(KeyCode.CONTROL).type(KeyCode.SPACE).release(KeyCode.CONTROL)

        // Aguardar conclusão
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { !robot.lookup("#btnProcessar").queryAs(JFXButton::class.java).text.equals("Cancelar", ignoreCase = true) }

        // Validar pastas (mockamos o winrar, mas o fluxo de arquivos temporários deve ter ocorrido)
        // No E2E completo real, verificaríamos o outputDir.
        
        // 10. Segunda execução via Botão Processar (após trocar destino)
        val destDir2 = tempDir.resolve("dest2").toFile().apply { mkdirs() }
        robot.clickOn("#txtPastaDestino")
        robot.press(KeyCode.CONTROL).type(KeyCode.A).release(KeyCode.CONTROL).type(KeyCode.BACK_SPACE)
        robot.write(destDir2.absolutePath)
        robot.type(KeyCode.ENTER)
        
        robot.clickOn("#btnProcessar", MouseButton.PRIMARY)
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { !robot.lookup("#btnProcessar").queryAs(JFXButton::class.java).text.equals("Cancelar", ignoreCase = true) }

        // 9. Processar e Validar Winrar
        // O processo de compactação é disparado uma única vez ao final, enviando a lista completa de pastas
        verify(mockWinrar, atLeastOnce()).compactar(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()
        )
    }
}
