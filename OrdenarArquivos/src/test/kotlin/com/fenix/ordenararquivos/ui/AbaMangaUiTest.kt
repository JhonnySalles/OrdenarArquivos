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
import javafx.scene.Node
import javafx.scene.Parent
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

    private lateinit var mockTelaInicialController: TelaInicialController

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()

        val loader = FXMLLoader(AbaMangaController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaMangaController::class.java) {
                AbaMangaController().apply {
                    // Injeção de dependências via reflection
                    listOf("mServiceManga", "mServiceComicInfo").forEach { fieldName ->
                        try {
                            val field = AbaMangaController::class.java.getDeclaredField(fieldName)
                            field.isAccessible = true
                            when (fieldName) {
                                "mServiceManga" -> field.set(this, mockMangaService)
                                "mServiceComicInfo" -> field.set(this, mockComicInfoService)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    controller = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Parent>()
        val scene = Scene(root, 1024.0, 768.0)
        applyJFoenixFix(scene)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        AlertasPopup.isTeste = true
        AlertasPopup.testResult = true
        Mockito.reset(mockMangaService, mockComicInfoService, mockTelaInicialController)

        // Injeta o controller pai e mocks de progresso
        controller.controllerPai = mockTelaInicialController
        whenever(mockTelaInicialController.rootProgress).thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicialController.rootMessage).thenReturn(javafx.scene.control.Label())
        whenever(mockTelaInicialController.rootStack).thenReturn(javafx.scene.layout.StackPane())
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())

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
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) { tbViewManga.items.size == 2 }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(1)
    fun testMangaTablePopulation(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        assertEquals(2, tbViewManga.items.size)
    }


    @Test
    @Order(3)
    fun testFiltroManga(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
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

        // Aguarda o resultado do filtro
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            tbViewManga.items.size == 1 && tbViewManga.items[0].nome == "One Piece"
        }

        verify(mockMangaService, atLeastOnce()).findAll(eq("One"), any(), any())
    }

    @Test
    @Order(3)
    fun testInlineEditManga(robot: FxRobot) {
        val tbViewManga =
                robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) { tbViewManga.items.isNotEmpty() }

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

    @Test
    @Order(6)
    fun testOpenPopupComicInfo(robot: FxRobot) {
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>

        robot.interact {
            tbViewManga.scrollTo(0)
            tbViewManga.selectionModel.select(0)
            tbViewManga.layout()
            tbViewManga.refresh()
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Chamar o método diretamente para garantir estabilidade no ambiente headless
        robot.interact {
            val manga = tbViewManga.items[0]
            controller.abrirPopupComicInfo(manga)
        }
        
        // Aguardar o JFXDialog aparecer com paciência (animação do JFoenix)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) {
            robot.lookup(".jfx-dialog-layout").tryQuery<javafx.scene.Node>().isPresent
        }

        // Verificar presença do layout do dialog
        val dialogLayout = robot.lookup(".jfx-dialog-layout").tryQuery<javafx.scene.Node>()
        assertTrue(dialogLayout.isPresent, "JFXDialog (.jfx-dialog-layout) não foi detectado")

        val btnCancelar = robot.from(dialogLayout.get()).lookup("#btnCancelar").queryAs(JFXButton::class.java)
        robot.interact { btnCancelar.fire() }
        WaitForAsyncUtils.waitForFxEvents()
        assertFalse(robot.lookup(".jfx-dialog-layout").tryQuery<javafx.scene.Node>().isPresent, "JFXDialog não fechou")
        WaitForAsyncUtils.waitForFxEvents()
        assertFalse(robot.lookup(".jfx-dialog-layout").tryQuery<javafx.scene.Node>().isPresent, "JFXDialog não fechou")
    }

    @AfterEach
    fun tearDown() {
        AlertasPopup.isTeste = false
        AlertasPopup.lastAlertTitle = null
        AlertasPopup.lastAlertText = null
    }
}
