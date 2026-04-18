package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaMangaController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.jfoenix.controls.JFXTextField
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
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

    private lateinit var mangaController: AbaMangaController
    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()
    private val mockTelaInicial = mock<TelaInicialController>()

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(AbaMangaController.fxmlLocate)
        loader.setControllerFactory { type: Class<*> ->
            if (type == AbaMangaController::class.java) {
                AbaMangaController().apply {
                    mangaController = this
                    controllerPai = mockTelaInicial
                    injectMocksInternal(this)
                }
            } else AbaMangaController()
        }
        val root: AnchorPane = loader.load()
        
        // JFXDialog precisa de um StackPane na cena para ser exibido.
        val stackPane = javafx.scene.layout.StackPane(root)
        whenever(mockTelaInicial.rootStack).thenReturn(stackPane)
        whenever(mockTelaInicial.rootTab).thenReturn(com.jfoenix.controls.JFXTabPane())

        stage.scene = Scene(stackPane, 1000.0, 700.0)
        stage.show()
    }

    private fun injectMocksInternal(controller: AbaMangaController) {
        val mangaServiceField = AbaMangaController::class.java.getDeclaredField("mServiceManga")
        mangaServiceField.isAccessible = true
        mangaServiceField.set(controller, mockMangaService)

        val comicInfoServiceField = AbaMangaController::class.java.getDeclaredField("mServiceComicInfo")
        comicInfoServiceField.isAccessible = true
        comicInfoServiceField.set(controller, mockComicInfoService)
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Mockito.reset(mockMangaService, mockComicInfoService, mockTelaInicial)
        
        // Mockar componentes de UI do TelaInicial para evitar NPE no binding de progresso
        whenever(mockTelaInicial.rootProgress).thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicial.rootMessage).thenReturn(javafx.scene.control.Label())
        // rootStack e rootTab já são configurados no start ou podem ser resetados aqui se necessário, 
        // mas no start já garantimos que o stackPane da cena é o que o mock retorna.

        val manga1 = Manga(id = 1L, nome = "Naruto", volume = "01", capitulo = "01", arquivo = "Naruto_01.cbz", capitulos = "1", quantidade = 1, comic = "Naruto")
        val manga2 = Manga(id = 2L, nome = "One Piece", volume = "01", capitulo = "01", arquivo = "OP_01.cbz", capitulos = "1", quantidade = 1, comic = "OP")

        whenever(mockMangaService.findAll(anyOrNull(), any(), any())).thenReturn(listOf(manga1, manga2))
        whenever(mockMangaService.findAll(eq("Naruto"), any(), any())).thenReturn(listOf(manga1))

        // Disparar carregamento via reflexão para garantir execução determinística no headless
        robot.interact {
            val method = AbaMangaController::class.java.getDeclaredMethod("carregarDados", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            method.isAccessible = true
            method.invoke(mangaController, false)
        }

        WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS) {
            robot.lookup("#tbViewManga").queryAs(TableView::class.java).items.size >= 2
        }
    }

    @Test
    @Order(1)
    fun testMangaCRUDInTable(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>

        // 1. Validar Filtro
        robot.interact {
            val txtFiltro = robot.lookup("#txtFiltro").queryAs(JFXTextField::class.java)
            txtFiltro.text = "Naruto"
            val method = AbaMangaController::class.java.getDeclaredMethod("carregarDados", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            method.isAccessible = true
            method.invoke(mangaController, false)
        }
        
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { tbViewManga.items.size == 1 }
        assertEquals("Naruto", tbViewManga.items[0].nome)

        // 2. Limpar Filtro
        robot.interact {
            val txtFiltro = robot.lookup("#txtFiltro").queryAs(JFXTextField::class.java)
            txtFiltro.text = ""
            val method = AbaMangaController::class.java.getDeclaredMethod("carregarDados", Boolean::class.javaPrimitiveType ?: Boolean::class.java)
            method.isAccessible = true
            method.invoke(mangaController, false)
        }
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) { tbViewManga.items.size == 2 }
    }
}
