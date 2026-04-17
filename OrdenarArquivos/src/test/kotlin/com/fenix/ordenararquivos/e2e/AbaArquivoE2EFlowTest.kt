package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.*
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
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
        Mockito.reset(mockMangaService, mockComicInfoService, mockSincronizacao, mockWinrar)

        // Mock padrão para não quebrar o fluxo de UI
        whenever(mockMangaService.find(any(), any())).thenReturn(null)
        whenever(mockComicInfoService.find(any(), anyOrNull())).thenReturn(null)
    }

    @Test
    @Order(1)
    fun testFullFlowAbaArquivo(robot: FxRobot) {
        // 1. Preparar pastas físicas
        val sourceDir = tempDir.resolve("source").toFile().apply { mkdirs() }
        val destDir = tempDir.resolve("dest").toFile().apply { mkdirs() }
        File(sourceDir, "001.jpg").writeText("fake image 1")
        File(sourceDir, "002.jpg").writeText("fake image 2")

        // 2. Configurar caminhos no controller (simulando seleção de pasta)
        val txtPastaOrigem = robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java)
        val txtPastaDestino = robot.lookup("#txtPastaDestino").queryAs(JFXTextField::class.java)
        
        robot.interact {
            val fOrigem = arquivoController.javaClass.getDeclaredField("mCaminhoOrigem")
            fOrigem.isAccessible = true
            fOrigem.set(arquivoController, sourceDir)

            val fDestino = arquivoController.javaClass.getDeclaredField("mCaminhoDestino")
            fDestino.isAccessible = true
            fDestino.set(arquivoController, destDir)

            // Atualizar campos de texto para refletir a "seleção"
            txtPastaOrigem.text = sourceDir.absolutePath
            txtPastaDestino.text = destDir.absolutePath
        }

        // 3. Preencher dados do mangá
        robot.interact { 
            stage.requestFocus() 
            robot.lookup("#cbLinguagem").queryAs(com.jfoenix.controls.JFXComboBox::class.java).value = 
                    com.fenix.ordenararquivos.model.enums.Linguagem.JAPANESE
            robot.lookup("#txtNomePastaManga").queryAs(com.jfoenix.controls.JFXTextField::class.java).text = "[JPN] One Piece -"
            robot.lookup("#txtVolume").queryAs(com.jfoenix.controls.JFXTextField::class.java).text = "01"
        }

        // 4. Fluxo de Importação (Gerar capítulos via UI)
        val txtGerarInicio = robot.lookup("#txtGerarInicio").queryAs(JFXTextField::class.java)
        val txtGerarFim = robot.lookup("#txtGerarFim").queryAs(JFXTextField::class.java)
        robot.interact {
            txtGerarInicio.text = "1"
            txtGerarFim.text = "2"
        }
        val btnGerar = robot.lookup("#btnGerar").queryAs(com.jfoenix.controls.JFXButton::class.java)
        robot.interact { btnGerar.fire() }
        
        // Aguarda o texto ser gerado no campo de importar
        val txtAreaImportar = robot.lookup("#txtAreaImportar").queryAs(com.jfoenix.controls.JFXTextArea::class.java)
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) { 
            !txtAreaImportar.text.isNullOrBlank()
        }
        
        // Importar via fire()
        val btnImportar = robot.lookup("#btnImportar").queryAs(com.jfoenix.controls.JFXButton::class.java)
        robot.interact { btnImportar.fire() }
        
        // 5. Validar Processamento Inicial
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        
        // Aguarda a tabela ser populada (processamento de importação)
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { tbViewTabela.items.size >= 2 }
        assertEquals(2, tbViewTabela.items.size, "Tabela deveria ter 2 capítulos importados")

        // 5. Simular enriquecimento via Popups (Aqui usamos Mocks de Jsoup se necessário,
        // mas como já testamos os popups isoladamente, vamos focar no fluxo E2E da aba principal)

        // 6. Configurar compactação
        robot.interact {
            robot.lookup("#cbCompactarArquivo").queryAs(JFXCheckBox::class.java).isSelected = true
            robot.lookup("#txtNomeArquivo").queryAs(JFXTextField::class.java).text =
                    "One Piece v01.cbz"
        }

        // 7. Processar (Compactar)
        // Precisamos mockar o retorno do compactar para não tentar rodar o WinRAR real
        whenever(
                        mockWinrar.compactar(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any()
                        )
                )
                .thenReturn(true)

        robot.clickOn("#btnProcessar")

        // Aguardar o processamento da Task (máximo 1s conforme solicitado)
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            !robot.lookup("#btnProcessar").queryAs(JFXButton::class.java).isDisable
        }

        // 8. Verificações Finais
        verify(mockWinrar, atLeastOnce())
                .compactar(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                )

        // Verificar se os campos foram "resetados" ou se o histórico foi atualizado
        val lsVwHistorico = robot.lookup("#lsVwHistorico").queryAs(JFXListView::class.java)
        assertFalse(
                lsVwHistorico.items.isEmpty(),
                "Histórico de processamento não deve estar vazio"
        )
    }
}
