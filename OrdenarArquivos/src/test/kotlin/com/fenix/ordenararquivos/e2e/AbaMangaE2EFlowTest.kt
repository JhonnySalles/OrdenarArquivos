package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaMangaController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXTabPane
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.application.Platform
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@Tag("E2E")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaMangaE2EFlowTest : BaseTest() {

    private lateinit var stage: Stage
    private lateinit var mainController: TelaInicialController
    private lateinit var mangaController: AbaMangaController
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
        val field = mainController.javaClass.getDeclaredField("abaMangaController")
        field.isAccessible = true
        mangaController = field.get(mainController) as AbaMangaController

        // Injeta mocks no controller via reflexão
        injectMocksInternal(mangaController)

        val scene = Scene(root, 1024.0, 768.0)
        applyJFoenixFix(scene)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
        stage.toFront()

        // Seleciona a aba Manga
        val mainTabPane = mainController.rootTab
        val tabField = mainController.javaClass.getDeclaredField("tbTabManga")
        tabField.isAccessible = true
        val tab = tabField.get(mainController) as Tab
        Platform.runLater { mainTabPane.selectionModel.select(tab) }
        WaitForAsyncUtils.waitForFxEvents()
        
        tabContent = tab.content
        
        // Inicializar o sistema de notificações
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = root
    }

    private fun injectMocksInternal(controller: AbaMangaController) {
        try {
            val mangaServiceField = AbaMangaController::class.java.getDeclaredField("mServiceManga")
            mangaServiceField.isAccessible = true
            mangaServiceField.set(controller, mockMangaService)

            val comicInfoServiceField = AbaMangaController::class.java.getDeclaredField("mServiceComicInfo")
            comicInfoServiceField.isAccessible = true
            comicInfoServiceField.set(controller, mockComicInfoService)
        } catch (e: Exception) {}
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Mockito.reset(mockMangaService, mockComicInfoService)
        
        val manga1 = Manga(id = 1L, nome = "Naruto", volume = "01", capitulo = "01", arquivo = "Naruto_01.cbz", capitulos = "1", quantidade = 1, comic = "Naruto")
        val manga2 = Manga(id = 2L, nome = "One Piece", volume = "01", capitulo = "01", arquivo = "OP_01.cbz", capitulos = "1", quantidade = 1, comic = "OP")

        whenever(mockMangaService.findAll(anyOrNull(), any(), any())).thenReturn(listOf(manga1, manga2))
        whenever(mockMangaService.findAll(eq("Naruto"), any(), any())).thenReturn(listOf(manga1))

        // Disparar carregamento via reflexão
        robot.interact {
            val method = AbaMangaController::class.java.getDeclaredMethod("carregarDados", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            method.isAccessible = true
            method.invoke(mangaController, false)
        }

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) {
            robot.from(tabContent).lookup("#tbViewManga").queryAs(TableView::class.java).items.size >= 2
        }
    }

    @Test
    @Order(1)
    fun testMangaCRUDInTable(robot: FxRobot) {
        val tbViewManga = robot.from(tabContent).lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>

        // 1. Validar Filtro
        robot.interact {
            val txtFiltro = robot.from(tabContent).lookup("#txtFiltro").queryAs(JFXTextField::class.java)
            txtFiltro.text = "Naruto"
            val method = AbaMangaController::class.java.getDeclaredMethod("carregarDados", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            method.isAccessible = true
            method.invoke(mangaController, false)
        }
        
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbViewManga.items.size == 1 }
        assertEquals("Naruto", tbViewManga.items[0].nome)

        // 2. Limpar Filtro
        robot.interact {
            val txtFiltro = robot.from(tabContent).lookup("#txtFiltro").queryAs(JFXTextField::class.java)
            txtFiltro.text = ""
            val method = AbaMangaController::class.java.getDeclaredMethod("carregarDados", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            method.isAccessible = true
            method.invoke(mangaController, false)
        }
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbViewManga.items.size == 2 }
    }
}
