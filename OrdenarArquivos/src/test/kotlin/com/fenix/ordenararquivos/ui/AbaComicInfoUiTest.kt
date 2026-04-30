package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaComicInfoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaComicInfoUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var comicinfoController: AbaComicInfoController

    @TempDir lateinit var tempDir: Path

    private lateinit var mockWinrar: WinrarServices
    private lateinit var mockOcrServices: OcrServices
    private var mockOcr: MockedStatic<Ocr>? = null

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            staticKeepAlive =
                    DriverManager.getConnection(
                            "jdbc:sqlite:file:testdb_comicinfo?mode=memory&cache=shared"
                    )
        }

        @AfterAll
        @JvmStatic
        fun globalTearDown() {
            staticKeepAlive?.close()
        }
    }

    private lateinit var mockTelaInicialController: TelaInicialController
    private lateinit var rootStack: StackPane
    private lateinit var rootNode: Parent

    @Start
    fun start(stage: Stage) {
        mockTelaInicialController = mock<TelaInicialController>()
        mockWinrar = mock<WinrarServices>()
        mockOcrServices = mock<OcrServices>()

        val loader = FXMLLoader(AbaComicInfoController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaComicInfoController::class.java) {
                AbaComicInfoController().apply {
                    // Injeção de dependências via reflection
                    listOf("mRarService", "mOcrService").forEach { fieldName ->
                        try {
                            val field =
                                    AbaComicInfoController::class.java.getDeclaredField(fieldName)
                            field.isAccessible = true
                            when (fieldName) {
                                "mRarService" -> field.set(this, mockWinrar)
                                "mOcrService" -> field.set(this, mockOcrServices)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    comicinfoController = this
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
        stage.toFront()
    }

    @BeforeEach
    fun setUp() {
        Ocr.isTeste = true
        AlertasPopup.isTeste = true
        AlertasPopup.testResult = true
        AlertasPopup.lastAlertTitle = null
        AlertasPopup.lastAlertText = null

        // Inicialização de componentes estáticos de UI para evitar
        // UninitializedPropertyAccessException
        AlertasPopup.rootStackPane = rootStack
        AlertasPopup.nodeBlur = rootNode

        if (rootNode is AnchorPane) {
            Notificacoes.rootAnchorPane = rootNode as AnchorPane
        } else {
            // Cria um AnchorPane temporário se o root não for um, para evitar erro nas notificações
            Notificacoes.rootAnchorPane = AnchorPane()
        }

        File("temp").mkdirs()

        mockOcr = Mockito.mockStatic(Ocr::class.java)

        Mockito.reset(mockWinrar, mockOcrServices, mockTelaInicialController)

        // Injeta o controller pai e mocks de progresso
        mainController = mockTelaInicialController
        comicinfoController.controllerPai = mockTelaInicialController
        whenever(mockTelaInicialController.rootProgress)
                .thenReturn(javafx.scene.control.ProgressBar())
        whenever(mockTelaInicialController.rootMessage).thenReturn(javafx.scene.control.Label())
        whenever(mockTelaInicialController.rootStack).thenReturn(rootStack)
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())
    }

    @AfterEach
    fun tearDown() {
        mockOcr?.close()
        mockOcr = null
        AlertasPopup.isTeste = false
        AlertasPopup.lastAlertTitle = null
        AlertasPopup.lastAlertText = null
        Mockito.validateMockitoUsage()
    }

    private fun helperCarregarItens(robot: FxRobot, numItens: Int = 1) {
        val tempDirFile = tempDir.toFile()
        // Limpar arquivos residuais se houver
        tempDirFile.listFiles()?.forEach { it.delete() }

        for (i in 1..numItens) {
            val dummyFile = File(tempDirFile, "test_manga_${i.toString().padStart(3, '0')}.rar")
            dummyFile.createNewFile()
        }

        val dummyComicInfoXml = File(tempDirFile, "ComicInfo.xml")
        dummyComicInfoXml.writeText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ComicInfo><Series>Test Series</Series><Title>Test Title</Title><Pages><Page Image=\"0\" Bookmark=\"Capítulo 1\"/></Pages></ComicInfo>"
        )

        whenever(mockWinrar.extraiComicInfo(any())).thenReturn(dummyComicInfoXml)
        mockOcr?.`when`<String>(MockedStatic.Verification { Ocr.process(any(), any(), any()) })
                ?.thenReturn("001-01")

        val txtPasta = robot.lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        val btnCarregar = robot.lookup("#btnCarregar").queryAs(JFXButton::class.java)

        robot.interact { txtPasta.text = tempDirFile.absolutePath }

        // Disparar ação de carregar
        robot.interact { btnCarregar.fire() }

        // Aguardar o conteúdo com timeout e estabilização (garantir que carregarItens terminou)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) {
            val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
            table.items.size >= numItens
        }

        // Injetar também na lista privada do controller para consistência absoluta
        robot.interact {
            val table =
                    robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<*>
            val field = comicinfoController.javaClass.getDeclaredField("mObsListaProcessar")
            field.isAccessible = true
            field.set(comicinfoController, table.items)
        }

        // Garantir que a renderização terminando antes de focar
        WaitForAsyncUtils.waitForFxEvents()

        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        robot.interact {
            if (table.items.isNotEmpty()) {
                table.selectionModel.clearAndSelect(0)
                table.requestFocus()
            }
        }
        assertTrue(table.items.isNotEmpty(), "A tabela deveria conter itens após o carregamento.")
    }

    @Test
    @Order(1)
    fun testCarregarItens(robot: FxRobot) {
        helperCarregarItens(robot)
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>
        assertEquals(1, table.items.size)
    }

    @Test
    @Order(2)
    fun testNormalizarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>
        val item = table.items[0]

        // Setup initial tags
        robot.interact {
            item.tags = "0${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMAGEM}capitulo 1"
        }

        val btnNormaliza = robot.lookup("#btnTagsNormaliza").queryAs(JFXButton::class.java)
        val cbLinguagem =
                robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java) as
                        JFXComboBox<Linguagem>

        robot.interact {
            // Seleciona PORTUGUESE explicitamente no combo
            cbLinguagem.selectionModel.select(Linguagem.PORTUGUESE)
            btnNormaliza.fire()
        }

        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(
                item.tags.contains("Capítulo 001"),
                "A tag deveria ser normalizada para 'Capítulo 001', atual: '${item.tags}'"
        )
    }

    @Test
    @Order(3)
    fun testSalvarTodos(robot: FxRobot) {
        helperCarregarItens(robot)
        whenever(mockWinrar.insereComicInfo(any(), any())).thenReturn(true)

        val btnSalvar = robot.lookup("#btnSalvarTodos").queryAs(JFXButton::class.java)

        robot.interact { btnSalvar.fire() }

        // Aumenta o timeout para o processamento assíncrono em headless
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { !btnSalvar.isDisable }
        WaitForAsyncUtils.waitForFxEvents()

        Mockito.verify(mockWinrar, Mockito.atLeastOnce()).insereComicInfo(any(), any())
    }

    @Test
    @Order(4)
    fun testGerarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val btnGerar = robot.lookup("#btnTagsProcessar").queryAs(JFXButton::class.java)
        val cbLinguagem =
                robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java) as
                        JFXComboBox<Linguagem>
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>
        val item = table.items[0]

        robot.interact {
            cbLinguagem.selectionModel.select(Linguagem.PORTUGUESE)
            btnGerar.fire()
        }

        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(item.tags.isNotEmpty(), "As tags não deveriam estar vazias após a geração.")
        assertTrue(
                item.tags.contains("Capítulo 001"),
                "Deveria conter o capítulo normalizado. Atual: ${item.tags}"
        )
        assertTrue(
                item.tags.contains("Test Title"),
                "As tags deveriam conter o título vindo do ComicInfo. Atual: ${item.tags}"
        )
    }

    @Test
    @Order(5)
    fun testAplicarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>
        val item = table.items[0]

        // Definir uma tag no formato "imagem|Capítulo 001 # Titulo" para testar o Split de
        // aplicação
        robot.interact {
            item.tags =
                    "0${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMAGEM} Capítulo 001 ${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMPORTACAO} Meu Titulo"
        }

        val btnAplicar = robot.lookup("#btnTagsAplicar").queryAs(JFXButton::class.java)
        robot.interact { btnAplicar.fire() }

        WaitForAsyncUtils.waitForFxEvents()
        // Validar se as tags foram de fato processadas (conter o capitulo e titulo)
        assertTrue(
                item.tags.contains("Capítulo 001"),
                "Tags deveriam conter o capítulo. Atual: ${item.tags}"
        )
        assertTrue(
                item.tags.contains("Meu Titulo"),
                "Tags deveriam conter o título. Atual: ${item.tags}"
        )
    }

    @Test
    @Order(6)
    fun testMenuContextoRemover(robot: FxRobot) {
        // Carregar 3 itens para o teste
        helperCarregarItens(robot, 3)
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>

        robot.interact {
            table.requestFocus()
            table.selectionModel.clearAndSelect(0)
        }

        // Navegar até o segundo item (índice 1) usando o teclado
        robot.type(KeyCode.DOWN)

        // Abrir menu de contexto no segundo item de forma programática para estabilidade
        robot.interact {
            table.selectionModel.select(1)
            val menu = table.contextMenu
            val itemMenu = menu.items.find { it.text == "Remover registro" }
            itemMenu?.fire()
        }

        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(2, table.items.size, "Deveriam restar 2 itens após remover um.")
    }

    @Test
    @Order(7)
    fun testOcrProcessarTask(robot: FxRobot) {
        helperCarregarItens(robot)
        val btnOcr = robot.lookup("#btnOcrProcessar").queryAs(JFXButton::class.java)

        // Mocks necessários para o fluxo de OCR no controller
        val dummySumario = File(tempDir.toFile(), "sumario.jpg").apply { createNewFile() }
        whenever(mockWinrar.extraiSumario(any(), any())).thenReturn(dummySumario)
        whenever(mockOcrServices.processOcr(any(), any(), any())).thenReturn("001|05|Titulo OCR")

        robot.interact { btnOcr.fire() }

        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>
        // Aguarda a execução da Task de OCR com mais paciência
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) {
            !btnOcr.isDisable && table.items[0].isProcessado
        }

        assertTrue(
                table.items[0].tags.contains("Titulo OCR"),
                "Tags não contém o texto vindo do OCR. Atual: ${table.items[0].tags}"
        )
    }

    @Test
    @Order(8)
    fun testBotaoCapitulos(robot: FxRobot) {
        helperCarregarItens(robot)
        val btnCapitulos = robot.lookup("#btnCapitulos").queryAs(JFXButton::class.java)

        robot.interact { btnCapitulos.fire() }

        WaitForAsyncUtils.waitForFxEvents()
        
        // Verificar se o popup abriu (JFXDialog é adicionado ao rootStack)
        val dialogs = rootStack.children.filterIsInstance<com.jfoenix.controls.JFXDialog>()
        assertTrue(dialogs.isNotEmpty(), "O popup de capítulos deveria ter sido aberto")

        // Fechar o dialog
        robot.interact {
            dialogs.forEach { it.close() }
        }
    }

    @Test
    @Order(9)
    fun testIntegracaoPopupAmazon(robot: FxRobot) {
        helperCarregarItens(robot)
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Processar>
        val btnAmazon = table.items[0].amazon!!

        robot.interact { btnAmazon.fire() }

        WaitForAsyncUtils.waitForFxEvents()
        
        // Verificar se o popup abriu
        val dialogs = rootStack.children.filterIsInstance<com.jfoenix.controls.JFXDialog>()
        assertTrue(dialogs.isNotEmpty(), "O popup da Amazon deveria ter sido aberto")

        // Fechar o dialog
        robot.interact {
            dialogs.forEach { it.close() }
        }
    }

    @Test
    @Order(10)
    fun testMissingComicInfoXml(robot: FxRobot) {
        // Simula carregamento onde um arquivo não tem ComicInfo.xml
        val tempDirFile = tempDir.toFile()
        val fileNoXml = File(tempDirFile, "no_xml.rar").apply { createNewFile() }

        whenever(mockWinrar.extraiComicInfo(eq(fileNoXml))).thenReturn(null)

        val txtPasta = robot.lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        robot.interact { txtPasta.text = tempDirFile.absolutePath }
        robot.clickOn("#btnCarregar")

        WaitForAsyncUtils.waitForFxEvents()
        val table =
                robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as
                        TableView<Processar>

        assertTrue(
                table.items.any { it.arquivo == fileNoXml.name },
                "O arquivo sem XML deveria estar na lista."
        )
    }
}
