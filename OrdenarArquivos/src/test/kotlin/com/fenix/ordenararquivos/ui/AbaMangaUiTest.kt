package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaMangaController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaMangaUiTest : BaseTest() {

    private lateinit var controller: AbaMangaController
    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()
    private val mockTelaInicialController = mock<TelaInicialController>()

    private val mangaList = listOf(
        Manga(id = 1, nome = "Naruto", volume = "01", comic = "Ninja"),
        Manga(id = 2, nome = "One Piece", volume = "05", comic = "Pirate")
    )

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(AbaMangaController::class.java.getResource("/view/AbaManga.fxml"))
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaMangaController::class.java) {
                AbaMangaController().apply {
                    // Injeção de dependências via reflection para os mocks
                    val fields = mapOf(
                        "mServiceManga" to mockMangaService,
                        "mServiceComicInfo" to mockComicInfoService
                    )
                    fields.forEach { (name, mock) ->
                        val field = AbaMangaController::class.java.getDeclaredField(name)
                        field.isAccessible = true
                        field.set(this, mock)
                    }
                    controller = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Parent>()
        stage.scene = Scene(root)
        stage.show()
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockMangaService, mockComicInfoService, mockTelaInicialController)
        controller.controllerPai = mockTelaInicialController
        
        whenever(mockTelaInicialController.rootMessage).thenReturn(Label())
        whenever(mockTelaInicialController.rootProgress).thenReturn(ProgressBar())

        // Mock padrão para carregamento inicial
        whenever(mockMangaService.findAll(anyOrNull(), any(), any())).thenReturn(mangaList)
    }

    @Test
    @Order(1)
    fun testMangaTablePopulation(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        
        // Aguarda o carregamento assíncrono do controller
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { tbViewManga.items.isNotEmpty() }
        
        assertEquals(2, tbViewManga.items.size)
        assertEquals("Naruto", tbViewManga.items[0].nome)
        assertEquals("One Piece", tbViewManga.items[1].nome)
    }

    @Test
    @Order(2)
    fun testFiltroManga(robot: FxRobot) {
        val txtFiltro = robot.lookup("#txtFiltro").queryAs(JFXTextField::class.java)
        
        // Mock para o filtro específico
        whenever(mockMangaService.findAll(eq("One"), any(), any())).thenReturn(listOf(mangaList[1]))

        robot.clickOn(txtFiltro).write("One")
        WaitForAsyncUtils.waitForFxEvents()
        
        // O controller chama carregarDados() no KeyReleased
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { 
            tbViewManga.items.any { it.nome == "One Piece" } && tbViewManga.items.size == 1 
        }

        verify(mockMangaService, atLeastOnce()).findAll(eq("One"), any(), any())
    }

    @Test
    @Order(3)
    fun testInlineEditManga(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbViewManga.items.isNotEmpty() }

        val cell = robot.lookup(".table-row-cell").nth(0).lookup(".table-cell").nth(1).query() // Coluna Nome
        
        robot.doubleClickOn(cell)
        robot.write("Naruto Uzumaki").type(javafx.scene.input.KeyCode.ENTER)
        
        assertEquals("Naruto Uzumaki", tbViewManga.items[0].nome)
    }

    @Test
    @Order(4)
    fun testSaveAction(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbViewManga.items.isNotEmpty() }

        // Localiza o botão de confirmar na coluna de ações da primeira linha
        val actionsCell = robot.lookup(".table-row-cell").nth(0).lookup(".table-cell").nth(8).query()
        val btnConfirmar = robot.from(actionsCell).lookup(".background-Green2").queryAs(JFXButton::class.java)

        robot.clickOn(btnConfirmar)
        
        verify(mockMangaService).save(any(), any(), any(), any())
    }

    @Test
    @Order(5)
    fun testDeleteAction(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbViewManga.items.isNotEmpty() }

        val actionsCell = robot.lookup(".table-row-cell").nth(0).lookup(".table-cell").nth(8).query()
        val btnExcluir = robot.from(actionsCell).lookup(".background-Red2").queryAs(JFXButton::class.java)

        // Mock confirmation alert manually or via robot
        Platform.runLater {
            // No TestFX, lidar com diálogos bloqueantes requer interact ou rodar em thread separada
            // Mas aqui vamos apenas disparar o clique e esperar que o modal apareça
        }
        
        robot.clickOn(btnExcluir)
        
        // Clica no botão OK do alerta de confirmação
        robot.clickOn("OK")
        
        verify(mockMangaService).deleteManga(any())
    }
}
