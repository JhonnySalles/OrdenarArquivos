package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = root
    }

    private fun injectMocksInternal(controller: AbaPastasController) {
        try {
            val fields = AbaPastasController::class.java.getDeclaredFields()
            fields.forEach { f ->
                if (f.name == "mServiceManga") {
                    f.isAccessible = true
                    f.set(controller, mockMangaService)
                }
                if (f.name == "mServiceComicInfo") {
                    f.isAccessible = true
                    f.set(controller, mockComicInfoService)
                }
            }
        } catch (e: Exception) {}
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockMangaService, mockComicInfoService)
    }

    @Test
    @Order(1)
    fun testFullFlowAbaPastas(robot: FxRobot) {
        // 1. SETUP DE PASTAS E NOMES (REGEX)
        val nomesPastas =
                listOf(
                        "capitulo 1",
                        "cap 002",
                        "[Teste de Scan] vol 2 cap 004",
                        "ch 05",
                        "chapter 7",
                        "[Test Scan] vol 7 ch 5"
                )
        nomesPastas.forEach { nome ->
            val dir = File(tempDir.toFile(), nome).apply { mkdirs() }
            File(dir, "imagem1.jpg").createNewFile()
            File(dir, "imagem2.jpg").createNewFile()
        }

        // Informado o caminho da pasta
        robot.clickOn(robot.from(tabContent).lookup("#txtPasta").query<Node>())
        robot.write(tempDir.toAbsolutePath().toString())
        robot.type(KeyCode.TAB)
        WaitForAsyncUtils.waitForFxEvents()

        // Clica em carregar se não disparou automático
        robot.clickOn(robot.from(tabContent).lookup("#btnCarregar").query<Node>())

        // 2. VALIDAÇÃO DE CARREGAMENTO E REGEX
        val tbTabPastas_Arquivos =
                pastasController
                        .javaClass
                        .getDeclaredField("tbTabPastas_Arquivos")
                        .apply { isAccessible = true }
                        .get(pastasController) as
                        Tab
        val contentArquivos = tbTabPastas_Arquivos.content
        val tbView =
                robot.from(contentArquivos)
                        .lookup("#tbViewProcessar")
                        .queryAs(TableView::class.java) as
                        TableView<Pasta>

        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbView.items.size == 6 }

        // Ordenar deterministicamente para validação (File.listFiles() não garante ordem)
        val itemsOrdenados =
                tbView.items.sortedWith(compareBy({ it.volume }, { it.capitulo }, { it.arquivo }))
        val capsLidos = itemsOrdenados.map { it.capitulo }

        // Ordem alfabética esperada:
        // 1. [Test Scan] vol 7 ch 5     -> 5.0
        // 2. [Teste de Scan] vol 2 cap 004 -> 4.0
        // 3. cap 002                    -> 2.0
        // 4. capitulo 1                 -> 1.0
        // 5. ch 05                      -> 5.0
        // 6. chapter 7                  -> 7.0
        val capitulosEsperados = listOf(1.0f, 2.0f, 5.0f, 7.0f, 4.0f, 5.0f)
        assertEquals(capitulosEsperados, capsLidos)

        assertEquals(1.0f, itemsOrdenados[0].capitulo)
        assertEquals(2.0f, itemsOrdenados[1].capitulo)
        assertEquals(5.0f, itemsOrdenados[2].capitulo)
        assertEquals(7.0f, itemsOrdenados[3].capitulo)
        assertEquals(4.0f, itemsOrdenados[4].capitulo)
        assertEquals(5.0f, itemsOrdenados[5].capitulo)

        // 3. INFORMAR MANGA (MOCK COM 3 VOLUMES)
        val mockMangaV1 =
                com.fenix.ordenararquivos.model.entities.Manga().apply {
                    nome = "Naruto"
                    volume = "1"
                    caminhos =
                            mutableListOf(
                                    com.fenix.ordenararquivos.model.entities.Caminhos(
                                            "001",
                                            "1",
                                            "Scan",
                                            ""
                                    ),
                                    com.fenix.ordenararquivos.model.entities.Caminhos(
                                            "002",
                                            "1",
                                            "Scan",
                                            ""
                                    )
                            )
                }
        val mockMangaV2 =
                com.fenix.ordenararquivos.model.entities.Manga().apply {
                    nome = "Naruto"
                    volume = "2"
                    caminhos =
                            mutableListOf(
                                    com.fenix.ordenararquivos.model.entities.Caminhos(
                                            "004",
                                            "2",
                                            "Scan",
                                            ""
                                    ),
                                    com.fenix.ordenararquivos.model.entities.Caminhos(
                                            "005",
                                            "2",
                                            "Scan",
                                            ""
                                    )
                            )
                }
        val mockMangaV7 =
                com.fenix.ordenararquivos.model.entities.Manga().apply {
                    nome = "Naruto"
                    volume = "7"
                    caminhos =
                            mutableListOf(
                                    com.fenix.ordenararquivos.model.entities.Caminhos(
                                            "007",
                                            "7",
                                            "Scan",
                                            ""
                                    )
                            )
                }

        whenever(mockMangaService.findAll(eq("Naruto"), any(), any(), any()))
                .thenReturn(listOf(mockMangaV1, mockMangaV2, mockMangaV7))
        whenever(mockMangaService.listar()).thenReturn(listOf("Naruto"))

        robot.clickOn(robot.from(tabContent).lookup("#cbManga").query<Node>())
        robot.write("Naruto")
        robot.type(KeyCode.ENTER)

        // Simular saida do campo para obter mComicInfo e volumes
        robot.interact {
            robot.from(tabContent)
                    .lookup("#txtPasta")
                    .queryAs(JFXTextField::class.java)
                    .requestFocus()
        }
        WaitForAsyncUtils.waitForFxEvents()

        // 4. VALIDAÇÃO DE VOLUMES (SINCRO COM MOCK)
        // Naruto mock: cap 1 -> vol 1, cap 2 -> vol 1, cap 4 -> vol 2, cap 5 -> vol 2, cap 7 -> vol
        // 7.
        // itemsOrdenados:
        // 0. Capitulo 1        -> Cap 1.0  -> Vol 1
        // 1. cap 002           -> Cap 2.0  -> Vol 1
        // 2. ch 05             -> Cap 5.0  -> Vol 2 (from mock Naruto mockMangaV2)
        // 3. chapter 7         -> Cap 7.0  -> Vol 7 (from mock Naruto mockMangaV7)
        // 4. vol 2 cap 004     -> Cap 4.0  -> Vol 2
        // 5. vol 7 ch 5        -> Cap 5.0  -> Vol 7 (Wait, no, it.volume > 0 initially for this
        // folder)

        assertEquals(1f, itemsOrdenados[0].volume)
        assertEquals(1f, itemsOrdenados[1].volume)
        assertEquals(2f, itemsOrdenados[2].volume)
        assertEquals(7f, itemsOrdenados[3].volume)
        assertEquals(2f, itemsOrdenados[4].volume)
        assertEquals(7f, itemsOrdenados[5].volume)

        // 5. ABA COMIC INFO E CONSULTA
        val tbTabPastas_ComicInfo =
                pastasController
                        .javaClass
                        .getDeclaredField("tbTabPastas_ComicInfo")
                        .apply { isAccessible = true }
                        .get(pastasController) as
                        Tab
        val contentComicInfo = tbTabPastas_ComicInfo.content

        robot.interact {
            robot.from(tabContent)
                    .lookup("#tbTabRootPastas")
                    .queryAs(com.jfoenix.controls.JFXTabPane::class.java)
                    .selectionModel
                    .select(tbTabPastas_ComicInfo)
        }
        WaitForAsyncUtils.waitForFxEvents()

        val mockMal1 =
                com.fenix.ordenararquivos.model.entities.comicinfo.Mal(
                        1L,
                        "Naruto Original",
                        "Desc 1",
                        null,
                        null,
                        mock()
                )
        val mockMal2 =
                com.fenix.ordenararquivos.model.entities.comicinfo.Mal(
                        2L,
                        "Naruto Shippuden",
                        "Desc 2",
                        null,
                        null,
                        mock()
                )
        whenever(mockComicInfoService.getMal(anyOrNull(), any()))
                .thenReturn(listOf(mockMal1, mockMal2))
        whenever(mockComicInfoService.updateMal(any(), any(), any())).thenAnswer { inv ->
            val comic =
                    inv.getArgument<com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo>(0)
            val mal = inv.getArgument<com.fenix.ordenararquivos.model.entities.comicinfo.Mal>(1)
            comic.idMal = mal.id
            comic.title = mal.nome
            null
        }

        robot.clickOn(robot.from(contentComicInfo).lookup("#txtMalNome").query<Node>())
                .write("Naruto")
        robot.clickOn(robot.from(contentComicInfo).lookup("#btnMalConsultar").query<Node>())

        val tbViewMal =
                robot.from(contentComicInfo).lookup("#tbViewMal").queryAs(TableView::class.java)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbViewMal.items.size == 2 }

        // Valida clique na grid aplica aos textfields
        robot.interact { tbViewMal.selectionModel.select(0) }
        robot.clickOn(robot.from(contentComicInfo).lookup("#btnMalAplicar").query<Node>())

        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            robot.from(contentComicInfo)
                    .lookup("#txtMalId")
                    .queryAs(JFXTextField::class.java)
                    .text == "1"
        }
        val txtMalId =
                robot.from(contentComicInfo).lookup("#txtMalId").queryAs(JFXTextField::class.java)
        assertEquals("1", txtMalId.text)

        // 6. VOLTAR GRID E GERAR CAPAS
        robot.interact {
            robot.from(tabContent)
                    .lookup("#tbTabRootPastas")
                    .queryAs(com.jfoenix.controls.JFXTabPane::class.java)
                    .selectionModel
                    .select(tbTabPastas_Arquivos)
        }
        robot.clickOn(robot.from(tabContent).lookup("#btnGerarCapas").query<Node>())

        // Refresh items reference since mObsListaProcessar was replaced
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) { tbView.items.any { it.isCapa } }
        val itemsAposCapas =
                tbView.items.sortedWith(compareBy({ it.volume }, { it.capitulo }, { it.arquivo }))

        val capasFisicas =
                tempDir.toFile().listFiles()?.filter { it.isDirectory && it.name.endsWith("Capa") }
                        ?: emptyList()
        assertTrue(capasFisicas.isNotEmpty(), "Deveria ter criado pastas de capa fisicamente")

        // 7. MENUS DE CONTEXTO
        // 7.1 SCANS
        robot.interact {
            tbView.selectionModel.select(itemsAposCapas[1]) // cap 002
            itemsAposCapas[1].scan = "NovaScan"
        }
        robot.rightClickOn(
                robot.from(contentArquivos).lookup(".table-row-cell").nth(1).query<Node>()
        )
        robot.clickOn(
                "Aplicar scan nos arquivos proximos"
        ) // No controlador está sem acento no 'proximos' desse item
        assertTrue(
                tbView.items.filter { it.capitulo >= 2.0f }.all { it.scan == "NovaScan" },
                "Deveria ter aplicado scan nos proximos"
        )

        // 7.2 TITULOS
        robot.interact {
            tbView.selectionModel.select(itemsAposCapas[1])
            itemsAposCapas[1].titulo = "Titulo Antigo"
        }
        robot.rightClickOn(
                robot.from(contentArquivos).lookup(".table-row-cell").nth(1).query<Node>()
        )
        robot.clickOn("Apagar titulos nos arquivos proximos") // No controlador está sem acento
        assertTrue(
                tbView.items.filter { it.capitulo >= 2.0f }.all { it.titulo.isEmpty() },
                "Deveria ter apagado titulos"
        )

        // 8. RENOMEAR
        robot.clickOn(robot.from(tabContent).lookup("#btnRenomear").query<Node>())
        // Renomear é uma Task, aguardar o processamento (grid limpa quando termina)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { tbView.items.isEmpty() }

        val pastasRenomeadas =
                tempDir.toFile().listFiles()?.filter { it.isDirectory }?.map { it.name }
                        ?: emptyList()
        assertTrue(
                pastasRenomeadas.any { it.contains("Capítulo 001") },
                "Deveria ter renomeado as pastas"
        )

        // 9. COMPACTAR
        // Recarregar os itens renomeados
        robot.write(tempDir.toAbsolutePath().toString())
        robot.clickOn(robot.from(tabContent).lookup("#btnCarregar").query<Node>())
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) { tbView.items.isNotEmpty() }

        // Mock static Winrar
        val mockWinrar = Mockito.mockStatic(com.fenix.ordenararquivos.process.Winrar::class.java)
        try {
            mockWinrar
                    .`when`<Boolean> {
                        com.fenix.ordenararquivos.process.Winrar.compactar(
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
                    }
                    .thenReturn(true)

            robot.interact {
                tbView.items.forEach { it.isSelecionado = true }
                tbView.refresh()
            }
            robot.clickOn(robot.from(tabContent).lookup("#btnCompactar").query<Node>())

            // Aguardar conclusão da compactação
            val btnCompactar =
                    robot.from(tabContent).lookup("#btnCompactar").queryAs(JFXButton::class.java)
            WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { !btnCompactar.isDisabled }

            // Verifica se "tentou" renomear/compactar (os arquivos reais dependeriam do Winrar
            // instalado)
            // Mas como mockamos, o teste deve passar até aqui.
        } finally {
            mockWinrar.close()
        }
    }
}
