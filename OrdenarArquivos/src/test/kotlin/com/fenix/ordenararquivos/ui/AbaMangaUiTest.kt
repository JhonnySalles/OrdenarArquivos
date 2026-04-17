package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaMangaController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import java.util.concurrent.TimeUnit
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
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

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaMangaUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var controller: AbaMangaController
    private val mockMangaService = mock<MangaServices>()
    private val mockComicInfoService = mock<ComicInfoServices>()

    private val mangaList =
            listOf(
                    Manga(id = 1, nome = "Naruto", volume = "01", comic = "Ninja"),
                    Manga(id = 2, nome = "One Piece", volume = "05", comic = "Pirate")
            )

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        val field = mainController.javaClass.getDeclaredField("abaMangaController")
        field.isAccessible = true
        controller = field.get(mainController) as AbaMangaController

        // Injeção de dependências via reflection para os mocks
        val mangaServiceField = AbaMangaController::class.java.getDeclaredField("mServiceManga")
        mangaServiceField.isAccessible = true
        mangaServiceField.set(controller, mockMangaService)

        val comicInfoServiceField =
                AbaMangaController::class.java.getDeclaredField("mServiceComicInfo")
        comicInfoServiceField.isAccessible = true
        comicInfoServiceField.set(controller, mockComicInfoService)

        val scene = Scene(root, 1024.0, 768.0)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        AlertasPopup.isTeste = true
        AlertasPopup.testResult = true
        Mockito.reset(mockMangaService, mockComicInfoService)

        // Navega para a aba Manga de forma programática
        robot.interact {
            val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
            tabPane.selectionModel.select(3) // 3 = Manga
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Mock padrão para carregamento inicial
        whenever(mockMangaService.findAll(anyOrNull(), any(), any())).thenReturn(mangaList)

        // Força recarregamento e aguarda a conclusão da Task
        robot.interact {
            val method = controller.javaClass.getDeclaredMethod("carregarDados", Boolean::class.java)
            method.isAccessible = true
            method.invoke(controller, false)
        }
        
        // Sincronização robusta: espera até que a lista seja populada (o que indica fim da Task)
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbViewManga.items.size == 2 }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(1)
    fun testMangaTablePopulation(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        assertEquals(2, tbViewManga.items.size)
    }

    @Test
    @Order(2)
    fun testFiltroManga(robot: FxRobot) {
        val txtFiltro = robot.lookup("#txtFiltro").queryAs(JFXTextField::class.java)
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>

        // Mock para o filtro específico
        whenever(mockMangaService.findAll(eq("One"), any(), any())).thenReturn(listOf(mangaList[1]))

        // Usa interact para definir o texto e disparar o evento de filtro de forma atômica
        robot.interact { 
            txtFiltro.text = "One"
            // Dispara o evento que o controller ouve (onKeyFiltro)
            val event = javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_RELEASED,
                "", "", javafx.scene.input.KeyCode.ENTER, false, false, false, false
            )
            txtFiltro.fireEvent(event)
        }

        // Aguarda o resultado do filtro (o controller limpa a lista e adiciona o novo item)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) {
            tbViewManga.items.size == 1 && tbViewManga.items[0].nome == "One Piece"
        }

        verify(mockMangaService, atLeastOnce()).findAll(eq("One"), any(), any())
    }

    @Test
    @Order(3)
    fun testInlineEditManga(robot: FxRobot) {
        val tbViewManga =
                robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbViewManga.items.isNotEmpty() }

        val clNome = tbViewManga.columns[1] as TableColumn<Manga, String>

        // Forçar entrada no modo de edição programaticamente
        robot.interact {
            tbViewManga.selectionModel.select(0)
            tbViewManga.edit(0, clNome)
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Localiza o TextField gerado pela célula em edição
        val textField = robot.lookup(".table-cell .text-field").queryAs(javafx.scene.control.TextField::class.java)
        
        robot.interact {
            textField.requestFocus()
            textField.text = "Naruto Uzumaki"
            
            // Em ambiente headless, o ENTER as vezes não propaga o commit da célula.
            // Forçamos o commit programaticamente para garantir a atualização do modelo.
            val event = TableColumn.CellEditEvent(
                tbViewManga,
                javafx.scene.control.TablePosition(tbViewManga, 0, clNome),
                TableColumn.editCommitEvent(),
                "Naruto Uzumaki"
            )
            clNome.onEditCommit.handle(event)
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Verifica se o item no TableView foi atualizado
        val manga = tbViewManga.items[0]
        assertTrue(
                manga.nome.contains("Uzumaki"),
                "O nome do manga deveria conter 'Uzumaki'. Atual: ${manga.nome}"
        )
    }

    @Test
    @Order(4)
    fun testSaveAction(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        
        robot.interact {
            tbViewManga.scrollTo(0)
            tbViewManga.selectionModel.select(0)
            tbViewManga.layout()
            tbViewManga.refresh()
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Localiza o botão salvar dentro da tabela de forma mais direta
        val btnSalvar = robot.lookup(".background-Green2").queryAs(JFXButton::class.java)
        assertNotNull(btnSalvar, "Botão Salvar não encontrado na linha selecionada")
        
        robot.interact { btnSalvar.fire() }
        verify(mockMangaService, timeout(2000)).save(any(), any(), any())
    }

    @Test
    @Order(5)
    fun testDeleteAction(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        
        robot.interact {
            tbViewManga.scrollTo(0)
            tbViewManga.selectionModel.select(0)
            tbViewManga.layout()
            tbViewManga.refresh()
        }
        WaitForAsyncUtils.waitForFxEvents()

        val btnExcluir = robot.lookup(".background-Red2").queryAs(JFXButton::class.java)
        assertNotNull(btnExcluir, "Botão Excluir não encontrado na linha selecionada")
        
        robot.interact { btnExcluir.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        verify(mockMangaService, timeout(2000)).deleteManga(any())
    }

    @AfterEach
    fun tearDown() {
        AlertasPopup.isTeste = false
        AlertasPopup.lastAlertTitle = null
        AlertasPopup.lastAlertText = null
    }
}
