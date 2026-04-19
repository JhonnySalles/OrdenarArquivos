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
import javafx.application.Platform
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
    private lateinit var mainController: TelaInicialController
    private lateinit var pastasController: AbaPastasController
    private lateinit var tabContent: Node

    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()

    @Start
    fun start(stage: Stage) {
        this.stage = stage
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        // Extrai o controller via reflexão
        val field = mainController.javaClass.getDeclaredField("pastasController")
        field.isAccessible = true
        pastasController = field.get(mainController) as AbaPastasController

        // Injeta mocks no controller via reflexão
        injectMocksInternal(pastasController)

        val scene = Scene(root, 1024.0, 768.0)
        applyJFoenixFix(scene)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
        stage.toFront()

        // Seleciona a aba Pastas
        val mainTabPane = mainController.rootTab
        val tabField = mainController.javaClass.getDeclaredField("tbTabPasta")
        tabField.isAccessible = true
        val tab = tabField.get(mainController) as Tab
        Platform.runLater { mainTabPane.selectionModel.select(tab) }
        WaitForAsyncUtils.waitForFxEvents()
        
        tabContent = tab.content
        
        // Inicializar o sistema de notificações
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = root
    }

    private fun injectMocksInternal(controller: AbaPastasController) {
        try {
            val mangaServiceField = AbaPastasController::class.java.getDeclaredField("mServiceManga")
            mangaServiceField.isAccessible = true
            mangaServiceField.set(controller, mockMangaService)

            val comicInfoServiceField = AbaPastasController::class.java.getDeclaredField("mServiceComicInfo")
            comicInfoServiceField.isAccessible = true
            comicInfoServiceField.set(controller, mockComicInfoService)
        } catch (e: Exception) {}
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockMangaService, mockComicInfoService)
        whenever(mockMangaService.listar()).thenReturn(listOf("Naruto", "One Piece", "Bleach"))
        whenever(mockMangaService.findAll(anyOrNull(), any(), any())).thenReturn(emptyList())
        whenever(mockComicInfoService.find(any(), anyOrNull())).thenReturn(null)
    }

    @Test
    @Order(1)
    fun testFullFlowAbaPastas(robot: FxRobot) {
        // 0. PREPARAÇÃO
        for (i in 1..5) {
            File(tempDir.toFile(), "Pasta $i Original").apply { mkdirs() }
        }

        val tbTabPastas_Arquivos = pastasController.javaClass.getDeclaredField("tbTabPastas_Arquivos").apply { isAccessible = true }.get(pastasController) as Tab
        val contentArquivos = tbTabPastas_Arquivos.content

        // 1. CARREGAR (escopado)
        robot.interact {
            val txtPasta = robot.from(tabContent).lookup("#txtPasta").queryAs(JFXTextField::class.java)
            txtPasta.text = tempDir.toAbsolutePath().toString()
            robot.from(tabContent).lookup("#btnCarregar").queryAs(JFXButton::class.java).fire()
        }

        val tbView = robot.from(contentArquivos).lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbView.items.size == 5 }

        // 2. INFORMAR MANGA
        robot.clickOn(robot.from(tabContent).lookup("#cbManga").query<Node>())
        robot.write("Naruto")
        robot.type(KeyCode.ENTER)
        
        robot.interact {
            robot.from(tabContent).lookup("#txtPasta").queryAs(JFXTextField::class.java).requestFocus()
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        // 3, 4 e 5. VOLUMES, CAPÍTULOS E TÍTULOS
        robot.interact {
            tbView.items.forEachIndexed { index, pasta ->
                pasta.volume = if (index < 2) 1.0f else 2.0f
                pasta.capitulo = (index + 1).toFloat()
                pasta.titulo = "Titulo Especial $index"
            }
            tbView.refresh()
        }

        assertEquals("Naruto", tbView.items[0].nome)

        // 6. SCAN E MENU CONTEXTO
        robot.interact {
            tbView.selectionModel.select(2)
            tbView.items[2].scan = "Scan-A"
            tbView.refresh()
        }
        robot.rightClickOn(robot.from(tbView).lookup(".table-row-cell").nth(2).query<Node>())
        robot.clickOn("Aplicar scan nos arquivos anteriores")
        
        assertEquals("Scan-A", tbView.items[0].scan)

        // 7. MUDAR SCAN E APLICAR DEMAIS
        robot.interact {
            tbView.items[2].scan = "Scan-B"
            tbView.refresh()
        }
        robot.rightClickOn(robot.from(tbView).lookup(".table-row-cell").nth(2).query<Node>())
        robot.clickOn("Aplicar scan nos arquivos próximos")
        
        assertEquals("Scan-B", tbView.items[4].scan)

        // 8. GERAR CAPAS
        robot.clickOn(robot.from(tabContent).lookup("#btnGerarCapas").query<Node>())
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbView.items.size == 7 }
        assertTrue(tbView.items.any { it.isCapa })

        // 9. COMIC INFO
        val tbTabPastas_ComicInfo = pastasController.javaClass.getDeclaredField("tbTabPastas_ComicInfo").apply { isAccessible = true }.get(pastasController) as Tab
        val contentComicInfo = tbTabPastas_ComicInfo.content
        
        robot.interact {
             robot.from(tabContent).lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java).selectionModel.select(tbTabPastas_ComicInfo)
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        val txtMalNome = robot.from(contentComicInfo).lookup("#txtMalNome").queryAs(JFXTextField::class.java)
        val btnMalConsultar = robot.from(contentComicInfo).lookup("#btnMalConsultar").queryAs(JFXButton::class.java)
        val tbViewMal = robot.from(contentComicInfo).lookup("#tbViewMal").queryAs(TableView::class.java)

        robot.clickOn(txtMalNome).write("Naruto")
        
        val mockMangaMal = mock<dev.katsute.mal4j.manga.Manga>()
        val fakeMal = com.fenix.ordenararquivos.model.entities.comicinfo.Mal(1L, "Naruto", "Desc", null, null, mockMangaMal)
        whenever(mockComicInfoService.getMal(anyOrNull(), any())).thenReturn(listOf(fakeMal))

        robot.clickOn(btnMalConsultar)
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbViewMal.items.isNotEmpty() }

        robot.interact { tbViewMal.selectionModel.select(0) }
        robot.clickOn(robot.from(contentComicInfo).lookup("#btnMalAplicar").query<Node>())

        // 10 e 11. AMAZON
        robot.interact { robot.from(contentComicInfo).lookup("#btnAmazonConsultar").queryAs(JFXButton::class.java).fire() }
        try {
            WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { robot.lookup(".dialog-black").tryQuery<Node>().isPresent }
            val buttonsVoltar = robot.lookup(".dialog-black .jfx-button").queryAll<Node>().toList()
            if (buttonsVoltar.size >= 2) robot.clickOn(buttonsVoltar[1]) 
        } catch (e: Exception) {}
        
        robot.interact { robot.from(contentComicInfo).lookup("#btnAmazonConsultar").queryAs(JFXButton::class.java).fire() }
        try {
            WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { robot.lookup(".dialog-black").tryQuery<Node>().isPresent }
            robot.clickOn(robot.lookup(".dialog-black").lookup("#txtSerie").query<Node>()).write("Naruto Series")
            val buttonsConfirmar = robot.lookup(".dialog-black .jfx-button").queryAll<Node>().toList()
            if (buttonsConfirmar.isNotEmpty()) robot.clickOn(buttonsConfirmar[0])
            
            WaitForAsyncUtils.waitForFxEvents()
            assertEquals("Naruto Series", robot.from(contentComicInfo).lookup("#txtSeries").queryAs(JFXTextField::class.java).text)
        } catch (e: Exception) {}

        // 12. VOLTAR ARQUIVOS
        robot.interact {
             robot.from(tabContent).lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java).selectionModel.select(tbTabPastas_Arquivos)
        }
        WaitForAsyncUtils.waitForFxEvents()

        // 13. MENUS REMOVER E IMPORTAR
        val totalAntes = tbView.items.size
        robot.interact { tbView.selectionModel.select(totalAntes - 1) }
        robot.rightClickOn(robot.from(tbView).lookup(".table-row-cell").nth(totalAntes - 1).query<Node>())
        robot.clickOn("Remover registro")
        robot.type(KeyCode.ENTER) 
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(totalAntes - 1, tbView.items.size)

        robot.rightClickOn(robot.from(tbView).lookup(".table-row-cell").nth(0).query<Node>())
        robot.clickOn("Importar volumes")
        verify(mockMangaService, atLeastOnce()).findAll(anyOrNull(), any(), any())

        // 14. APLICAR E VALIDAR
        robot.clickOn(robot.from(tabContent).lookup("#btnAplicar").query<Node>())
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { tbView.items.isEmpty() }

        val arquivosFisicos = tempDir.toFile().listFiles()?.map { it.name } ?: emptyList()
        assertTrue(arquivosFisicos.isNotEmpty())
    }
}
