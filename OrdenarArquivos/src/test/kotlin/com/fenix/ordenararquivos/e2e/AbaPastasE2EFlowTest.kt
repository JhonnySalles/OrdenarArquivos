package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.*
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
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
class AbaPastasE2EFlowTest : BaseTest() {

    @TempDir lateinit var tempDir: Path

    private lateinit var stage: Stage
    private lateinit var pastasController: AbaPastasController
    private val mockTelaInicial = mock<TelaInicialController>()
    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()

    @Start
    fun start(stage: Stage) {
        this.stage = stage
        val loader = FXMLLoader(AbaPastasController.fxmlLocate)
        loader.setControllerFactory { type: Class<*> ->
            if (type == AbaPastasController::class.java) {
                AbaPastasController().apply {
                    pastasController = this
                    controllerPai = mockTelaInicial
                }
            } else type.getDeclaredConstructor().newInstance()
        }
        val root: AnchorPane = loader.load()
        
        // Injetar mocks no controller
        injectMocks(pastasController)

        val stackPane = javafx.scene.layout.StackPane(root)
        whenever(mockTelaInicial.rootStack).thenReturn(stackPane)
        whenever(mockTelaInicial.rootTab).thenReturn(com.jfoenix.controls.JFXTabPane())

        stage.scene = Scene(stackPane, 1000.0, 800.0)
        stage.show()
    }

    private fun injectMocks(controller: AbaPastasController) {
        mapOf(
            "mServiceManga" to mockMangaService,
            "mServiceComicInfo" to mockComicInfoService
        ).forEach { (name, mock) ->
            try {
                val field = AbaPastasController::class.java.getDeclaredField(name)
                field.isAccessible = true
                field.set(controller, mock)
            } catch (e: Exception) {}
        }
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockTelaInicial, mockMangaService, mockComicInfoService)

        // Mockar componentes de UI do TelaInicial para evitar NPE no binding de progresso
        whenever(mockTelaInicial.rootProgress).thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicial.rootMessage).thenReturn(javafx.scene.control.Label())
        whenever(mockTelaInicial.rootStack).thenReturn(javafx.scene.layout.StackPane())
        whenever(mockTelaInicial.rootTab).thenReturn(com.jfoenix.controls.JFXTabPane())

        whenever(mockMangaService.listar()).thenReturn(listOf("Naruto", "One Piece", "Bleach"))
        whenever(mockMangaService.findAll(anyOrNull(), any(), any())).thenReturn(emptyList())
        whenever(mockComicInfoService.find(any(), anyOrNull())).thenReturn(null)
    }

    @Test
    @Order(1)
    fun testFullFlowAbaPastas(robot: FxRobot) {
        // --- 0. PREPARAÇÃO (5 Pastas) ---
        for (i in 1..5) {
            File(tempDir.toFile(), "Pasta $i Original").apply { mkdirs() }
        }

        val tbTabPastas_Arquivos = pastasController.javaClass.getDeclaredField("tbTabPastas_Arquivos").apply { isAccessible = true }.get(pastasController) as Tab
        val contentArquivos = tbTabPastas_Arquivos.content

        // --- 1. CARREGAR ---
        robot.interact {
            val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
            txtPasta.text = tempDir.toAbsolutePath().toString()
            robot.lookup("#btnCarregar").queryAs(JFXButton::class.java).fire()
        }

        val tbView = robot.from(contentArquivos).lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { tbView.items.size == 5 }

        // --- 2. INFORMAR MANGA ---
        robot.interact {
            val cbManga = robot.lookup("#cbManga").queryAs(JFXComboBox::class.java)
            cbManga.editor.text = "Naruto"
            // Disparar o focus loss para que o controller atualize os itens
            cbManga.editor.parent.requestFocus()
        }
        
        // --- 3, 4 e 5. VOLUMES, CAPÍTULOS E TÍTULOS ---
        robot.interact {
            tbView.items.forEachIndexed { index, pasta ->
                pasta.volume = if (index < 2) 1.0f else 2.0f
                pasta.capitulo = (index + 1).toFloat()
                pasta.titulo = "Titulo Especial $index"
            }
            tbView.refresh()
        }

        // Validar coluna formatado atualizada
        assertTrue(tbView.items[0].nome == "Naruto")
        assertTrue(tbView.items[0].volume == 1.0f)
        assertTrue(tbView.items[2].volume == 2.0f)

        // --- 6. SCAN E MENU CONTEXTO (ANTERIORES) ---
        robot.interact {
            tbView.selectionModel.select(2) // Registro do meio (Pasta 3)
            tbView.items[2].scan = "Scan-A"
            tbView.refresh()
        }
        robot.rightClickOn(robot.from(tbView).lookup(".table-row-cell").nth(2).query<Node>())
        robot.clickOn("Aplicar scan nos arquivos anteriores")
        
        assertEquals("Scan-A", tbView.items[0].scan)
        assertEquals("Scan-A", tbView.items[1].scan)

        // --- 7. MUDAR SCAN E APLICAR DEMAIS (PRÓXIMOS) ---
        robot.interact {
            tbView.items[2].scan = "Scan-B"
            tbView.refresh()
        }
        robot.rightClickOn(robot.from(tbView).lookup(".table-row-cell").nth(2).query<Node>())
        robot.clickOn("Aplicar scan nos arquivos próximos")
        
        assertEquals("Scan-B", tbView.items[3].scan)
        assertEquals("Scan-B", tbView.items[4].scan)

        // --- 8. GERAR CAPAS ---
        robot.clickOn("#btnGerarCapas")
        // Como temos vol 1 e 2, devem surgir 2 novas pastas de "Capa"
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { tbView.items.size == 7 }
        assertTrue(tbView.items.any { it.isCapa })

        // --- 9. COMIC INFO ---
        val tbTabPastas_ComicInfo = pastasController.javaClass.getDeclaredField("tbTabPastas_ComicInfo").apply { isAccessible = true }.get(pastasController) as Tab
        val contentComicInfo = tbTabPastas_ComicInfo.content
        
        robot.interact {
             robot.lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java).selectionModel.select(tbTabPastas_ComicInfo)
        }
        
        val txtMalNome = robot.from(contentComicInfo).lookup("#txtMalNome").queryAs(JFXTextField::class.java)
        val btnMalConsultar = robot.from(contentComicInfo).lookup("#btnMalConsultar").queryAs(JFXButton::class.java)
        val tbViewMal = robot.from(contentComicInfo).lookup("#tbViewMal").queryAs(TableView::class.java)

        robot.clickOn(txtMalNome).write("Naruto")
        
        val mockMangaMal = mock<dev.katsute.mal4j.manga.Manga>()
        val fakeMal = com.fenix.ordenararquivos.model.entities.comicinfo.Mal(1L, "Naruto", "Desc", null, null, mockMangaMal)
        whenever(mockComicInfoService.getMal(anyOrNull(), any())).thenReturn(listOf(fakeMal))

        robot.clickOn(btnMalConsultar)
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { tbViewMal.items.isNotEmpty() }

        robot.interact { tbViewMal.selectionModel.select(0) }
        robot.clickOn("#btnMalAplicar")

        // --- 10 e 11. AMAZON ---
        robot.clickOn("#btnAmazonConsultar")
        // Fechar sem aplicar (procurando botão Voltar no Dialog que abre no rootStack)
        WaitForAsyncUtils.waitForFxEvents()
        robot.clickOn("Voltar")
        
        // Abrir e aplicar
        robot.clickOn("#btnAmazonConsultar")
        robot.clickOn(robot.lookup(".dialog-black").lookup("#txtSerie").queryAs(JFXTextField::class.java)).write("Naruto Series")
        robot.clickOn("Confirmar")
        
        assertEquals("Naruto Series", robot.from(contentComicInfo).lookup("#txtSeries").queryAs(JFXTextField::class.java).text)

        // --- 12. VOLTAR ARQUIVOS ---
        robot.interact {
             robot.lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java).selectionModel.select(tbTabPastas_Arquivos)
        }

        // --- 13. MENUS REMOVER E IMPORTAR ---
        val totalAntes = tbView.items.size
        robot.interact { tbView.selectionModel.select(totalAntes - 1) }
        robot.rightClickOn(robot.lookup(".table-row-cell").nth(totalAntes - 1).query<Node>())
        robot.clickOn("Remover registro")
        
        // Modal de confirmação (AlertasPopup)
        robot.type(KeyCode.ENTER) // Sim
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(totalAntes - 1, tbView.items.size)

        robot.rightClickOn(robot.lookup(".table-row-cell").nth(0).query<Node>())
        robot.clickOn("Importar volumes")
        verify(mockMangaService, atLeastOnce()).findAll(anyOrNull(), any(), any())

        // --- 14. APLICAR E VALIDAR ---
        // Obter lista do campo formatado antes de aplicar
        val nomesEsperados = tbView.items.map { 
            val volStr = java.text.DecimalFormat("00.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(it.volume)
            val capStr = java.text.DecimalFormat("000.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(it.capitulo)
            "[${it.scan}] Naruto - Volume $volStr ${if (it.isCapa) "Capa" else "Capítulo $capStr"}"
        }

        robot.clickOn("#btnAplicar")
        
        // Aguardar limpeza da grid após aplicação bem sucedida
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { tbView.items.isEmpty() }

        // Validação física
        val arquivosFisicos = tempDir.toFile().listFiles()?.map { it.name } ?: emptyList()
        nomesEsperados.forEach { nome ->
            assertTrue(arquivosFisicos.contains(nome), "Deveria existir a pasta: $nome")
        }
    }
}
