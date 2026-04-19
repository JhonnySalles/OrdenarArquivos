package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.PopupAmazon
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.control.TableColumn
import javafx.scene.control.TabPane
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
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
class AbaPastasUiTest : BaseTest() {

    private lateinit var pastasController: AbaPastasController
    private lateinit var mockMangaService: MangaServices
    private lateinit var mockComicInfoService: ComicInfoServices

    @TempDir lateinit var tempDir: Path
    private lateinit var mockPopupAmazon: MockedStatic<PopupAmazon>

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive =
                    DriverManager.getConnection(
                            "jdbc:sqlite:file:pastas_testdb?mode=memory&cache=shared"
                    )
            DataBase.instancia
        }

        @AfterAll
        @JvmStatic
        fun globalTearDown() {
            staticKeepAlive?.close()
            staticKeepAlive = null
            DataBase.isTeste = false
        }
    }

    private lateinit var mockTelaInicialController: TelaInicialController
    private lateinit var rootStack: StackPane
    private lateinit var rootNode: Parent

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()
        mockMangaService = mock()
        mockComicInfoService = mock()

        val loader = FXMLLoader(AbaPastasController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaPastasController::class.java) {
                AbaPastasController().apply {
                    // Injeção de dependências via reflection
                    listOf("mServiceManga", "mServiceComicInfo").forEach { fieldName ->
                        try {
                            val field = AbaPastasController::class.java.getDeclaredField(fieldName)
                            field.isAccessible = true
                            when (fieldName) {
                                "mServiceManga" -> field.set(this, mockMangaService)
                                "mServiceComicInfo" -> field.set(this, mockComicInfoService)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    pastasController = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        rootNode = loader.load<Parent>()
        rootStack = StackPane(rootNode)
        
        val scene = Scene(rootStack, 1024.0, 768.0)
        applyJFoenixFix(scene)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        AlertasPopup.isTeste = true
        AlertasPopup.lastAlertText = null
        AlertasPopup.lastAlertTitle = null

        // Inicializa containers estáticos para notificações e alertas
        AlertasPopup.rootStackPane = rootStack
        AlertasPopup.nodeBlur = rootNode
        com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = rootNode as AnchorPane

        mockPopupAmazon = Mockito.mockStatic(PopupAmazon::class.java)

        Mockito.reset(mockMangaService, mockComicInfoService, mockTelaInicialController)

        // Injeta o controller pai e mocks de progresso
        pastasController.controllerPai = mockTelaInicialController
        whenever(mockTelaInicialController.rootProgress).thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicialController.rootMessage).thenReturn(javafx.scene.control.Label())
        whenever(mockTelaInicialController.rootStack).thenReturn(rootStack)
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())
    }

    @Test
    @Order(1)
    fun testCamposDefault(robot: FxRobot) {
        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        assertEquals("", txtPasta.text)
    }

    @Test
    @Order(2)
    fun testEdicaoColunasGrid(robot: FxRobot) {
        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>

        val pastaTest =
                Pasta(
                        pasta = File("caminho/teste"),
                        arquivo = "Pasta 01",
                        nome = "Manga Teste",
                        volume = 1f,
                        capitulo = 1f,
                        scan = "Scan Original",
                        titulo = "Titulo Original"
                )

        robot.interact { tbViewProcessar.items.add(pastaTest) }

        // Editar Scan
        robot.doubleClickOn("Scan Original")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("Nova Scan")
        robot.interact {
            val column = tbViewProcessar.columns[1] as TableColumn<Pasta, String>
            val event = TableColumn.CellEditEvent<Pasta, String>(
                tbViewProcessar,
                javafx.scene.control.TablePosition<Pasta, String>(tbViewProcessar, 0, column),
                TableColumn.editCommitEvent<Pasta, String>(),
                "Nova Scan"
            )
            column.onEditCommit.handle(event)
        }

        // Editar Titulo
        robot.doubleClickOn("Titulo Original")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("Novo Titulo")
        robot.interact {
            val column = tbViewProcessar.columns[4] as TableColumn<Pasta, String>
            val event = TableColumn.CellEditEvent<Pasta, String>(
                tbViewProcessar,
                javafx.scene.control.TablePosition<Pasta, String>(tbViewProcessar, 0, column),
                TableColumn.editCommitEvent<Pasta, String>(),
                "Novo Titulo"
            )
            column.onEditCommit.handle(event)
        }

        // Editar Volume (formatado como 01)
        robot.doubleClickOn("01")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("2")
        robot.interact {
            val column = tbViewProcessar.columns[2] as TableColumn<Pasta, Number>
            val event = TableColumn.CellEditEvent<Pasta, Number>(
                tbViewProcessar,
                javafx.scene.control.TablePosition<Pasta, Number>(tbViewProcessar, 0, column),
                TableColumn.editCommitEvent<Pasta, Number>(),
                2.0f
            )
            column.onEditCommit.handle(event)
        }

        // Editar Capítulo (formatado como 001)
        robot.doubleClickOn("001")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("5")
        robot.interact {
            val column = tbViewProcessar.columns[3] as TableColumn<Pasta, Number>
            val event = TableColumn.CellEditEvent<Pasta, Number>(
                tbViewProcessar,
                javafx.scene.control.TablePosition<Pasta, Number>(tbViewProcessar, 0, column),
                TableColumn.editCommitEvent<Pasta, Number>(),
                5.0f
            )
            column.onEditCommit.handle(event)
        }

        WaitForAsyncUtils.waitForFxEvents()

        assertEquals(
                "Nova Scan",
                pastaTest.scan,
                "Scan com valor inesperado. Valor atual: ${pastaTest.scan}"
        )
        assertEquals(
                "Novo Titulo",
                pastaTest.titulo,
                "Titulo com valor inesperado. Valor atual: ${pastaTest.titulo}"
        )
        assertEquals(
                2f,
                pastaTest.volume,
                "Volume com valor inesperado. Valor atual: ${pastaTest.volume}"
        )
        assertEquals(
                5f,
                pastaTest.capitulo,
                "Capitulo com valor inesperado. Valor atual: ${pastaTest.capitulo}"
        )

        // Validar coluna formatada
        val formatado = robot.lookup("#tbViewProcessar")
                        .queryAs(TableView::class.java)
                        .columns
                        .last()
                        .getCellData(0) as String
        assertTrue(
                formatado.contains("[Nova Scan]"),
                "Texto formatado com scan inesperado. Valor atual: ${formatado}"
        )
        assertTrue(
                formatado.contains("Volume 02"),
                "Texto formatado com volume inesperado. Valor atual: ${formatado}"
        )
        assertTrue(
                formatado.contains("Capítulo 005"),
                "Texto formatado com capitulo inesperado. Valor atual: ${formatado}"
        )
    }

    @Test
    @Order(3)
    fun testCicloCompletoPastas(robot: FxRobot) {
        // Criar estrutura de pastas temporárias com scan no início para correta extração
        val sub1 = tempDir.resolve("[Scan A] Manga Vol 01 Cap 01").toFile().apply { mkdirs() }
        val sub2 = tempDir.resolve("[Scan A] Manga Vol 02 Cap 02").toFile().apply { mkdirs() }
        val sub3 = tempDir.resolve("[Scan A] Manga Vol 03 Cap 03").toFile().apply { mkdirs() }

        File(sub1, "pag01.jpg").createNewFile()
        File(sub2, "pag01.jpg").createNewFile()
        File(sub3, "pag01.jpg").createNewFile()

        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        robot.interact { txtPasta.text = tempDir.toAbsolutePath().toString() }

        robot.clickOn("#btnCarregar")
        WaitForAsyncUtils.waitForFxEvents()

        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        assertEquals(
                3,
                tbViewProcessar.items.size,
                "Quantidade de pastas inesperada. Valor atual: ${tbViewProcessar.items.size}"
        )

        // Validar extração via Regex desas pastas
        assertEquals(
                "Scan A",
                tbViewProcessar.items[0].scan,
                "Scan com valor inesperado. Valor atual: ${tbViewProcessar.items[0].scan}"
        )
        assertEquals(
                1f,
                tbViewProcessar.items[0].volume,
                "Volume com valor inesperado. Valor atual: ${tbViewProcessar.items[0].volume}"
        )
        assertEquals(
                1f,
                tbViewProcessar.items[0].capitulo,
                "Capitulo com valor inesperado. Valor atual: ${tbViewProcessar.items[0].capitulo}"
        )

        // Simular preenchimento do manga
        robot.clickOn(robot.lookup("#cbManga").queryAs(Node::class.java)).write("Manga Teste").push(KeyCode.ENTER)

        // Simular perda de foco para atualizar os nomes
        val tbNode = robot.lookup("#tbViewProcessar").queryAs(Node::class.java)
        robot.doubleClickOn(tbNode)
        WaitForAsyncUtils.waitForFxEvents()

        // Validar coluna formatada antes de aplicar
        val clFormatado = tbViewProcessar.columns.last()
        val formatado1 = clFormatado.getCellData(0) as String
        assertEquals(
                "[Scan A] Manga Teste - Volume 01 Capítulo 001",
                formatado1,
                "Texto formatado com valor inesperado. Valor atual: ${formatado1}"
        )

        // Aplicar renomeio
        robot.clickOn(robot.lookup("#btnAplicar").queryAs(Node::class.java))

        // Aguarda processamento Task
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) {
            tempDir.toFile().listFiles()?.any { it.name == "[Scan A] Manga Teste - Volume 01 Capítulo 001" } == true
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Validar que pastas originais foram renomeadas conforme o formato sugerido
        val files = tempDir.toFile().listFiles()
        assertTrue(
                files?.any { it.name == "[Scan A] Manga Teste - Volume 01 Capítulo 001" } == true,
                "Pasta com nome inesperado. Valor atual: ${files}"
        )
    }

    @Test
    @Order(4)
    fun testCarregamentoComicInfo(robot: FxRobot) {
        val comicInfoFake =
                ComicInfo(
                        java.util.UUID.randomUUID(),
                        456L,
                        "Capa Naruto",
                        "Naruto Shippuden",
                        "Naruto",
                        "Editora Panini",
                        "Alternate",
                        "Arc 1",
                        "Group 1",
                        "Imprint 1",
                        "Ação; Aventura",
                        "pt",
                        AgeRating.Teen
                )

        whenever(mockComicInfoService.find(eq("Naruto"), any())).thenReturn(comicInfoFake)

        val tbTabRoot = robot.lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java)
        robot.interact { tbTabRoot.selectionModel.select(1) }
        WaitForAsyncUtils.waitForFxEvents()

        robot.clickOn("#cbManga").write("Naruto").push(KeyCode.ENTER)
        robot.clickOn("#txtTitle") 
        
        WaitForAsyncUtils.waitForFxEvents()
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) {
            robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text == "Naruto Shippuden"
        }

        assertEquals(
                "Naruto Shippuden",
                robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text
        )
    }

    @Test
    @Order(5)
    fun testMockMalRequest(robot: FxRobot) {
        val tabRoot = robot.lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java)
        robot.interact { tabRoot.selectionModel.select(1) }
        WaitForAsyncUtils.waitForFxEvents()

        val devMalManga1 = mock<dev.katsute.mal4j.manga.Manga>()
        val malClassic = Mal(121L, "Naruto Classic", "Desc Classic", null, null, devMalManga1)
        val devMalManga2 = mock<dev.katsute.mal4j.manga.Manga>()
        val malShippuden = Mal(122L, "Naruto Shippuden", "Desc Shippuden", null, null, devMalManga2)

        whenever(mockComicInfoService.getMal(anyOrNull(), any())).thenReturn(listOf(malClassic, malShippuden))
        whenever(mockComicInfoService.updateMal(any(), any(), any())).thenAnswer { invocation ->
            val comic = invocation.getArgument<ComicInfo>(0)
            val mal = invocation.getArgument<Mal>(1)
            comic.title = mal.nome
            null
        }

        robot.clickOn("#txtMalNome").write("Naruto")
        robot.clickOn("#btnMalConsultar")

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) {
            robot.lookup("#tbViewMal").queryAs(TableView::class.java).items.isNotEmpty()
        }

        robot.doubleClickOn("Naruto Classic")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)
        
        assertEquals(
                "Naruto Classic",
                robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text
        )
    }

    @Test
    @Order(6)
    fun testValidacoesEntrada(robot: FxRobot) {
        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        val btnCarregar = robot.lookup("#btnCarregar").queryAs(JFXButton::class.java)

        robot.interact {
            AlertasPopup.lastAlertText = null
            txtPasta.text = "" 
        }
        robot.interact { btnCarregar.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Alerta", AlertasPopup.lastAlertTitle)

        val btnAplicar = robot.lookup("#btnAplicar").queryAs(JFXButton::class.java)
        robot.interact {
            AlertasPopup.lastAlertText = null
            txtPasta.text = "caminho/qualquer"
            robot.lookup("#cbManga").queryAs(JFXComboBox::class.java).value = null
        }
        robot.interact { btnAplicar.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Alerta", AlertasPopup.lastAlertTitle)
    }

    @AfterEach
    fun tearDown() {
        if (::mockPopupAmazon.isInitialized) {
            mockPopupAmazon.close()
        }
        AlertasPopup.isTeste = false
        AlertasPopup.lastAlertTitle = null
        AlertasPopup.lastAlertText = null
    }
}
