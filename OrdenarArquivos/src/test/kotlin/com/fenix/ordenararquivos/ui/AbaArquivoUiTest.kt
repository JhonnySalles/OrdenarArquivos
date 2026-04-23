package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.jfoenix.controls.*
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
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

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaArquivoUiTest : BaseTest() {

    private lateinit var controller: AbaArquivoController
    private var mockMangaService = mock<MangaServices>()
    private var mockComicInfoService = mock<ComicInfoServices>()
    private var mockSincronizacao = mock<SincronizacaoServices>()
    private var mockTelaInicialController = mock<TelaInicialController>()
    private lateinit var rootStack: StackPane

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

    @Start
    fun start(stage: Stage) {
        Ocr.isTeste = true

        try {
            AlertasPopup.rootStackPane = StackPane()
            AlertasPopup.nodeBlur = AnchorPane()
            Notificacoes.rootAnchorPane = AnchorPane()
        } catch (e: Exception) {}

        val loader = FXMLLoader(AbaArquivoController.fxmlLocate)
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == AbaArquivoController::class.java) {
                AbaArquivoController().apply {
                    listOf("mServiceManga", "mServiceComicInfo", "mSincronizacao").forEach {
                            fieldName ->
                        try {
                            val field = AbaArquivoController::class.java.getDeclaredField(fieldName)
                            field.isAccessible = true
                            when (fieldName) {
                                "mServiceManga" -> field.set(this, mockMangaService)
                                "mServiceComicInfo" -> field.set(this, mockComicInfoService)
                                "mSincronizacao" -> field.set(this, mockSincronizacao)
                            }
                        } catch (e: Exception) {}
                    }
                    controller = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Parent>()
        rootStack = StackPane(root)
        stage.scene = Scene(rootStack)
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        // Garantir que eventos anteriores terminaram
        WaitForAsyncUtils.waitForFxEvents()

        Mockito.reset(
                mockMangaService,
                mockComicInfoService,
                mockSincronizacao,
                mockTelaInicialController
        )

        // Injeta o controller pai para evitar UninitializedPropertyAccessException
        controller.controllerPai = mockTelaInicialController

        // Mock das propriedades de progresso para evitar NPE no processar()
        whenever(mockTelaInicialController.rootProgress).thenReturn(ProgressBar())
        whenever(mockTelaInicialController.rootMessage).thenReturn(Label())
        whenever(mockTelaInicialController.rootStack).thenReturn(rootStack)
        whenever(mockTelaInicialController.rootTab).thenReturn(JFXTabPane())

        // Limpar estado do controller para evitar vazamento entre testes
        robot.interact {
            try {
                // Resetar campos de texto
                val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
                robot.from(root as Node)
                        .lookup("#txtNomePastaManga")
                        .queryAs(JFXTextField::class.java)
                        .text = ""
                robot.from(root as Node)
                        .lookup("#txtVolume")
                        .queryAs(JFXTextField::class.java)
                        .text = ""
                robot.from(root as Node)
                        .lookup("#txtTitle")
                        .queryAs(JFXTextField::class.java)
                        .text = ""
                robot.from(root as Node)
                        .lookup("#txtSeries")
                        .queryAs(JFXTextField::class.java)
                        .text = ""

                // Resetar via reflection se necessário para isolamento total
                val fManga = controller.javaClass.getDeclaredField("mManga")
                fManga.isAccessible = true
                fManga.set(controller, null)
            } catch (e: Exception) {}
        }

        // Mock padrão robusto: usamos apenas a versão completa (2 args)
        // para evitar que o Kotlin injete valores raw (false) ao chamar o overload de 1 arg.
        doAnswer {
                    if (it.arguments.isNotEmpty() && it.arguments[0] is Manga)
                            it.arguments[0] as Manga
                    else null
                }
                .whenever(mockMangaService)
                .find(any<Manga>(), any())

        // Mock padrão para ComicInfo find (2 args)
        doReturn(null).whenever(mockComicInfoService).find(any(), anyOrNull())
    }

    @Test
    @Order(1)
    fun testVolumeMaisIncrementsValue(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
        val txtVolume =
                robot.from(root as Node).lookup("#txtVolume").queryAs(JFXTextField::class.java)
        val txtNomePastaManga =
                robot.from(root as Node)
                        .lookup("#txtNomePastaManga")
                        .queryAs(JFXTextField::class.java)

        robot.interact {
            txtNomePastaManga.text = "Dummy Manga"
            txtVolume.text = "01"
        }
        robot.clickOn("#btnVolumeMais")
        assertEquals("02", txtVolume.text)
    }

    @Test
    @Order(2)
    fun testVolumeMenosDecrementsValue(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
        val txtVolume =
                robot.from(root as Node).lookup("#txtVolume").queryAs(JFXTextField::class.java)
        val txtNomePastaManga =
                robot.from(root as Node)
                        .lookup("#txtNomePastaManga")
                        .queryAs(JFXTextField::class.java)

        robot.interact {
            txtNomePastaManga.text = "Dummy Manga"
            txtVolume.text = "02"
        }
        robot.clickOn("#btnVolumeMenos")
        assertEquals("01", txtVolume.text)
    }

    @Test
    @Order(3)
    fun testShortcutToggleExtra(robot: FxRobot) {
        val textArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.clickOn("#txtAreaImportar")
        robot.interact { textArea.text = "001-001" }

        robot.interact {
            val event =
                    KeyEvent(KeyEvent.KEY_PRESSED, "E", "E", KeyCode.E, false, true, false, false)
            textArea.onKeyPressed.handle(event)
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(textArea.text.contains("Extra"))
    }

    @Test
    @Order(6)
    fun testMangaConsultation(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
        val tabRoot = robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java)

        // Ir para a aba ComicInfo
        robot.interact { tabRoot.selectionModel.select(1) }
        WaitForAsyncUtils.waitForFxEvents()

        // 1. Mock
        val devMalManga1 = mock<dev.katsute.mal4j.manga.Manga>()
        val malClassic = Mal(121L, "Naruto Classic", "Desc Classic", null, null, devMalManga1)
        val devMalManga2 = mock<dev.katsute.mal4j.manga.Manga>()
        val malShippuden = Mal(122L, "Naruto Shippuden", "Desc Shippuden", null, null, devMalManga2)

        whenever(mockComicInfoService.getMal(anyOrNull(), any()))
                .thenReturn(listOf(malClassic, malShippuden))

        whenever(mockComicInfoService.updateMal(any(), any(), any())).thenAnswer { invocation ->
            val comic = invocation.getArgument<ComicInfo>(0)
            val mal = invocation.getArgument<Mal>(1)
            comic.title = mal.nome
            comic.series = mal.nome + " Series"
            comic.publisher = "Editora " + mal.nome
            null
        }

        // Realizar consulta MAL
        robot.interact {
            robot.lookup("#tbTabRootArquivo")
                    .queryAs(JFXTabPane::class.java)
                    .selectionModel
                    .select(1)
        }
        WaitForAsyncUtils.waitForFxEvents()

        robot.clickOn("#txtMalNome").write("Naruto")
        robot.clickOn("#btnMalConsultar")

        // Aguardar o modelo de dados ser populado
        WaitForAsyncUtils.waitForFxEvents()
        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            robot.lookup("#tbViewMal").queryAs(TableView::class.java).items.isNotEmpty()
        }

        val tbViewMal = robot.lookup("#tbViewMal").queryAs(TableView::class.java) as TableView<Mal>
        assertEquals(2, tbViewMal.items.size)

        // 2. Fluxo: Duplo Clique
        robot.doubleClickOn("Naruto Classic")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)

        assertEquals(
                "Naruto Classic",
                robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text
        )

        // 3. Fluxo: Aplicar
        robot.clickOn("Naruto Shippuden")
        robot.clickOn("#btnMalAplicar")

        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)
        assertEquals(
                "Naruto Shippuden",
                robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text
        )

        // 4. Salvar
        robot.interact {
            val fDestino = controller.javaClass.getDeclaredField("mCaminhoDestino")
            fDestino.isAccessible = true
            fDestino.set(controller, File(System.getProperty("java.io.tmpdir")))

            robot.lookup("#txtNomeArquivo").queryAs(JFXTextField::class.java).text = "test_mal.cbr"
        }

        robot.clickOn("#btnGravarComicInfo")
        WaitForAsyncUtils.waitForFxEvents()

        verify(mockComicInfoService, atLeastOnce()).updateMal(any<ComicInfo>(), any<Mal>(), any())
    }

    @Test
    @Order(11)
    fun testSugestaoOCR(robot: FxRobot) {
        val dummyImage = File("src/main/resources/images/icoAbrir_48.png")
        robot.interact {
            val method = controller.javaClass.getDeclaredMethod("ocrSumario", File::class.java)
            method.isAccessible = true
            method.invoke(controller, dummyImage)
        }

        WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
            val field = controller.javaClass.getDeclaredField("mSugestao")
            field.isAccessible = true
            (field.get(controller) as com.jfoenix.controls.JFXAutoCompletePopup<*>).suggestions
                    .isNotEmpty()
        }

        val field = controller.javaClass.getDeclaredField("mSugestao")
        field.isAccessible = true
        assertEquals(
                "001-05 Suggestion",
                (field.get(controller) as com.jfoenix.controls.JFXAutoCompletePopup<*>).suggestions[
                        0]
        )
    }

    @Test
    @Order(3)
    fun testProcessarArquivos(robot: FxRobot) {
        val txtPasta = robot.lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java)
        robot.interact { txtPasta.text = System.getProperty("java.io.tmpdir") }

        // robot.clickOn("#btnAreaImportarPesquisar") // Invalid ID

        val txtArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact { txtArea.text = "001-001\n002-002" }

        robot.clickOn("#btnImportar")

        robot.clickOn("#btnProcessar")
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(10)
    fun testCarregamentoComicInfo(robot: FxRobot) {
        // Mock do ComicInfo que deve ser retornado pelo banco ao encontrar "Teste"
        val comicInfoFake =
                ComicInfo(
                        java.util.UUID.randomUUID(),
                        456L,
                        "Teste",
                        "Teste Title",
                        "Teste Series",
                        "Editora Teste",
                        "Alternate Teste",
                        "Arc Teste",
                        "Group Teste",
                        "Imprint Teste",
                        "Ação; Aventura",
                        "pt",
                        AgeRating.Teen
                )

        doReturn(comicInfoFake).whenever(mockComicInfoService).find(any(), anyOrNull())
        doReturn(emptyList<Mal>()).whenever(mockComicInfoService).getMal(anyOrNull(), any())

        // Mock do Manga se necessário ao carregar pelos campos de volume
        val mangaFake =
                Manga(
                        1L,
                        "Teste",
                        "Volume 01",
                        "Capítulo",
                        "Teste.cbz",
                        10,
                        "001-010",
                        java.time.LocalDateTime.now()
                )
        doReturn(mangaFake).whenever(mockMangaService).find(any<Manga>(), any())

        // Ir para a aba ComicInfo (selecionando programaticamente)
        val tabRoot = robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java)
        robot.interact { tabRoot.selectionModel.select(1) }
        WaitForAsyncUtils.waitForFxEvents()

        // 1. Digitar o nome no padrão que o controller espera para extrair "Teste"
        robot.clickOn("#txtNomePastaManga").write("[JPN] Teste -")

        // 2. Clicar em outro campo (Volume) para disparar o listener de perda de foco do
        // NomePastaManga
        robot.clickOn("#txtVolume").write("Volume 01")

        // 3. Clicar em outro campo (Titulo) para disparar o listener do Volume
        robot.clickOn("#txtTitle")

        WaitForAsyncUtils.waitForFxEvents()

        // Esperar a tarefa de consulta ao MAL (disparada por carregaComicInfo) terminar para não
        // poluir outros testes
        try {
            WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
                !robot.lookup("#btnMalConsultar").queryAs(JFXButton::class.java).isDisable
            }
        } catch (e: Exception) {}

        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)

        // Verificar se os campos foram preenchidos com os dados do mock
        assertEquals(
                "Teste Title",
                robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text
        )
        assertEquals(
                "Teste Series",
                robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text
        )
        assertEquals(
                "Editora Teste",
                robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text
        )
        assertEquals(
                "Alternate Teste",
                robot.lookup("#txtAlternateSeries").queryAs(JFXTextField::class.java).text
        )

        val cbAgeRating =
                robot.lookup("#cbAgeRating").queryAs(JFXComboBox::class.java) as
                        JFXComboBox<AgeRating>
        assertEquals(AgeRating.Teen, cbAgeRating.value)
    }

    @Test
    @Order(7)
    fun testValidacaoConsultaMangaCamposVazios(robot: FxRobot) {
        val tabRoot = robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java)

        // Ir para a aba ComicInfo
        robot.interact { tabRoot.selectionModel.select(1) }
        WaitForAsyncUtils.waitForFxEvents()

        robot.interact {
            AlertasPopup.lastAlertText = null
            val txtMalId = robot.lookup("#txtMalId").queryAs(JFXTextField::class.java)
            (txtMalId.parent as javafx.scene.layout.Pane).requestFocus()
            txtMalId.text = ""
            robot.lookup("#txtMalNome").queryAs(JFXTextField::class.java).text = ""
        }
        robot.clickOn("#btnMalConsultar")

        // Espera explícita pelo alerta
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) { AlertasPopup.lastAlertText != null }

        assertEquals("Alerta", AlertasPopup.lastAlertTitle)
        assertTrue(
                AlertasPopup.lastAlertText?.lowercase()?.contains("id ou nome") == true,
                "Mensagem de erro de MAL não encontrada"
        )

        // Testar Aplicar sem seleção
        robot.interact {
            AlertasPopup.lastAlertText = null
            robot.lookup("#tbViewMal")
                    .queryAs(TableView::class.java)
                    .selectionModel
                    .clearSelection()
        }
        robot.clickOn("#btnMalAplicar")
    }

    @Test
    @Order(4)
    fun testGerarCapitulosFlow(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtGerarInicio").queryAs(JFXTextField::class.java).text = "10"
            robot.lookup("#txtGerarFim").queryAs(JFXTextField::class.java).text = "12"
        }
        robot.clickOn("#btnGerar")

        val textArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertTrue(textArea.text.contains("010-"))
        assertTrue(textArea.text.contains("011-"))
        assertTrue(textArea.text.contains("012-"))
    }

    @Test
    @Order(6)
    fun testTabelaOperacoes(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text =
                    "001-001\n002-002"
        }
        robot.clickOn("#btnImportar")

        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        assertEquals(2, tbViewTabela.items.size)

        robot.interact {
            robot.lookup("#txtQuantidade").queryAs(JFXTextField::class.java).text = "10"
        }
        robot.clickOn("#btnSomar")

        val firstItem = tbViewTabela.items[0] as com.fenix.ordenararquivos.model.entities.Caminhos
        assertEquals(11, firstItem.numero)

        robot.interact {
            robot.lookup("#txtQuantidade").queryAs(JFXTextField::class.java).text = "5"
        }
        robot.clickOn("#btnSubtrair")
        assertEquals(6, firstItem.numero)
    }

    @Test
    @Order(8)
    fun testLimparTudoFlow(robot: FxRobot) {
        robot.interact {
            robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text =
                    "To be cleared"
            robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text = "Data"
        }

        robot.clickOn("#btnLimparTudo")
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals(
                "[JPN] Manga -",
                robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text
        )
        assertEquals("", robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text)
    }

    @Test
    @Order(14)
    fun testGravarComicInfoPersistence(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)

        // Mocking setup for save
        robot.interact {
            val tabRoot = robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java)
            tabRoot.selectionModel.select(1)
        }
        WaitForAsyncUtils.waitForFxEvents()

        robot.interact {
            robot.from(root).lookup("#txtPastaDestino").queryAs(JFXTextField::class.java).text =
                    System.getProperty("java.io.tmpdir")
            robot.from(root).lookup("#txtNomeArquivo").queryAs(JFXTextField::class.java).text =
                    "test.cbr"

            // Força o carregamento da pasta para evitar NPE por mCaminhoDestino == null
            val method = controller.javaClass.getDeclaredMethod("carregaPastaDestino")
            method.isAccessible = true
            method.invoke(controller)
        }

        robot.clickOn("#btnGravarComicInfo")
        WaitForAsyncUtils.waitForFxEvents()

        verify(mockComicInfoService, atLeastOnce()).save(any(), any(), any(), any())
    }

    @Test
    @Order(9)
    fun testTabSwitching(robot: FxRobot) {
        val tabPane = robot.lookup("#tbTabRootArquivo").queryAs(JFXTabPane::class.java)

        robot.interact {
            tabPane.selectionModel.select(1) // ComicInfo
        }
        assertEquals(1, tabPane.selectionModel.selectedIndex)

        robot.interact {
            tabPane.selectionModel.select(2) // Capas
        }
        assertEquals(2, tabPane.selectionModel.selectedIndex)
    }

    @Test
    @Order(16)
    fun testAjustarNomesFlow(robot: FxRobot) {
        val tempDir =
                File(
                        System.getProperty("java.io.tmpdir"),
                        "test_ajuste_" + java.util.UUID.randomUUID()
                )
        tempDir.mkdirs()
        try {
            val file1 = File(tempDir, "1.jpg").apply { writeText("dummy") }
            val file10 = File(tempDir, "10.jpg").apply { writeText("dummy") }

            robot.interact {
                val fOrigem = controller.javaClass.getDeclaredField("mCaminhoOrigem")
                fOrigem.isAccessible = true
                fOrigem.set(controller, tempDir)

                val fDestino = controller.javaClass.getDeclaredField("mCaminhoDestino")
                fDestino.isAccessible = true
                fDestino.set(controller, tempDir)
            }

            robot.clickOn("#btnAjustarNomes")

            // Aguarda o processamento async (padding 3 é o padrão do controller)
            WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) {
                tempDir.listFiles()?.any { it.name == "001.jpg" } == true
            }

            assertTrue(File(tempDir, "001.jpg").exists())
            assertTrue(File(tempDir, "010.jpg").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    @Order(5)
    fun testShortcutsAdvanced(robot: FxRobot) {
        val textArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)

        // Teste Ctrl+O (Ordenar)
        robot.interact {
            textArea.text = "002-002\n001-001"
            val event =
                    KeyEvent(KeyEvent.KEY_PRESSED, "O", "O", KeyCode.O, false, true, false, false)
            textArea.onKeyPressed.handle(event)
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("001-001\n002-002", textArea.text.trim())

        // Teste Ctrl+T (Limpar Tags)
        robot.interact {
            textArea.text = "001-001|Tag"
            val event =
                    KeyEvent(KeyEvent.KEY_PRESSED, "T", "T", KeyCode.T, false, true, false, false)
            textArea.onKeyPressed.handle(event)
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("001-001", textArea.text.trim())
    }

    @Test
    @Order(7)
    fun testValidationAlerts(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
        // Garantir campos vazios
        robot.interact {
            val fOrigem = controller.javaClass.getDeclaredField("mCaminhoOrigem")
            fOrigem.isAccessible = true
            fOrigem.set(controller, null)

            val fDestino = controller.javaClass.getDeclaredField("mCaminhoDestino")
            fDestino.isAccessible = true
            fDestino.set(controller, null)
        }

        robot.clickOn(robot.from(root).lookup("#btnProcessar").query() as Node)

        // O controller define a cor de unFocus para RED em caso de erro
        val txtPastaOrigem =
                robot.from(root).lookup("#txtPastaOrigem").queryAs(JFXTextField::class.java)
        assertEquals(javafx.scene.paint.Color.RED, txtPastaOrigem.unFocusColor)
    }

    @Test
    @Order(17)
    fun testHistoricoPersistence(robot: FxRobot) {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_hist")
        tempDir.mkdirs()

        try {
            robot.interact {
                val fOrigem = controller.javaClass.getDeclaredField("mCaminhoOrigem")
                fOrigem.isAccessible = true
                fOrigem.set(controller, tempDir)

                val fDestino = controller.javaClass.getDeclaredField("mCaminhoDestino")
                fDestino.isAccessible = true
                fDestino.set(controller, tempDir)

                robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java).text =
                        "Hist Manga"
                robot.lookup("#txtVolume").queryAs(JFXTextField::class.java).text = "01"
                robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java).text = "001-001"

                // Adicionar linguagem para passar na validação
                val cbLinguagem =
                        robot.lookup("#cbLinguagem").queryAs(JFXComboBox::class.java) as
                                JFXComboBox<com.fenix.ordenararquivos.model.enums.Linguagem>
                cbLinguagem.selectionModel.select(
                        com.fenix.ordenararquivos.model.enums.Linguagem.PORTUGUESE
                )
            }

            robot.clickOn("#btnImportar")
            WaitForAsyncUtils.waitForFxEvents()

            // Aguarda a tabela popular para passar na validação do validaCampos()
            val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
            WaitForAsyncUtils.waitFor(1, TimeUnit.SECONDS) { tbViewTabela.items.isNotEmpty() }

            // Necessário preencher para passar na validação do controller (cbCompactarArquivo
            // selecionado por padrão)
            robot.interact {
                robot.lookup("#txtNomeArquivo").queryAs(JFXTextField::class.java).text =
                        "Hist Manga Volume 01 (Jap) Sem capa.cbr"
            }

            robot.clickOn("#btnProcessar")
            WaitForAsyncUtils.waitForFxEvents()

            val lsVwHistorico = robot.lookup("#lsVwHistorico").queryAs(JFXListView::class.java)
            assertTrue(lsVwHistorico.items.isNotEmpty())
            val hist = lsVwHistorico.items[0] as com.fenix.ordenararquivos.model.entities.Historico
            assertTrue(hist.nome.contains("Hist Manga"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    @Order(18)
    fun testCheckboxesEstadoInicial(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
        robot.interact {
            assertTrue(
                    robot.from(root)
                            .lookup("#cbCompactarArquivo")
                            .queryAs(JFXCheckBox::class.java)
                            .isSelected
            )
            assertTrue(
                    robot.from(root)
                            .lookup("#cbVerificaPaginaDupla")
                            .queryAs(JFXCheckBox::class.java)
                            .isSelected
            )
            assertTrue(
                    robot.from(root)
                            .lookup("#cbMesclarCapaTudo")
                            .queryAs(JFXCheckBox::class.java)
                            .isSelected
            )
        }
    }

    @Test
    @Order(19)
    fun testLimparTabelaFlow(robot: FxRobot) {
        val tbViewTabela = robot.lookup("#tbViewTabela").queryAs(TableView::class.java)
        robot.interact {
            @Suppress("UNCHECKED_CAST")
            val items =
                    tbViewTabela.items as
                            javafx.collections.ObservableList<
                                    com.fenix.ordenararquivos.model.entities.Caminhos>
            items.add(
                    com.fenix.ordenararquivos.model.entities.Caminhos(
                            1,
                            null,
                            "Original",
                            1,
                            "Cap 01",
                            "Manga",
                            "Tag"
                    )
            )
        }
        assertEquals(1, tbViewTabela.items.size)

        robot.clickOn("#btnLimpar")
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals(0, tbViewTabela.items.size)
    }

    @Test
    @Order(20)
    fun testSeparadoresCampos(robot: FxRobot) {
        robot.interact {
            val txtSepPag = robot.lookup("#txtSeparadorPagina").queryAs(JFXTextField::class.java)
            val txtSepCap = robot.lookup("#txtSeparadorCapitulo").queryAs(JFXTextField::class.java)

            txtSepPag.isDisable = false
            txtSepCap.isDisable = false

            txtSepPag.text = "::"
            txtSepCap.text = "@@"

            assertEquals("::", txtSepPag.text)
            assertEquals("@@", txtSepCap.text)
        }
    }

    @Test
    @Order(21)
    fun testShortcutDuplicarLinha(robot: FxRobot) {
        val txtArea = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
        robot.interact {
            txtArea.text = "Linha Teste"
            txtArea.positionCaret(txtArea.text.length)
            txtArea.requestFocus()
        }

        robot.clickOn("#txtAreaImportar")
        robot.press(KeyCode.CONTROL).type(KeyCode.D).release(KeyCode.CONTROL)
        WaitForAsyncUtils.waitForFxEvents()

        robot.interact {
            val txtAreaCheck = robot.lookup("#txtAreaImportar").queryAs(JFXTextArea::class.java)
            assertTrue(txtAreaCheck.text.contains("Linha Teste\nLinha Teste"))
        }
    }

    @Test
    @Order(22)
    fun testHistoricoRestaurarEstado(robot: FxRobot) {
        testHistoricoPersistence(robot) // Garante que há algo no histórico

        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)

        // 1. Limpa tudo e aguarda reset
        robot.clickOn("#btnLimparTudo")
        WaitForAsyncUtils.waitForFxEvents()

        robot.interact {
            assertEquals(
                    "[JPN] Manga -",
                    robot.from(root)
                            .lookup("#txtNomePastaManga")
                            .queryAs(JFXTextField::class.java)
                            .text
            )

            // 2. Seleciona o item no histórico de forma explícita
            val lsVwHistorico = robot.lookup("#lsVwHistorico").queryAs(JFXListView::class.java)
            lsVwHistorico.selectionModel.select(0)
        }

        // 3. Double click no componente
        robot.doubleClickOn("#lsVwHistorico")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)

        // 4. Valida restauração
        robot.interact {
            val txtNome = robot.lookup("#txtNomePastaManga").queryAs(JFXTextField::class.java)
            Assertions.assertNotEquals("[JPN] Manga -", txtNome.text)
            assertTrue(txtNome.text.contains("Hist Manga"))
        }
    }

    @Test
    @Order(23)
    fun testProximaPastaDestino(robot: FxRobot) {
        val root = robot.lookup("#apRoot").queryAs(AnchorPane::class.java)
        val baseDir = Files.createTempDirectory("test_proxima_pasta").toFile()
        val v1 = File(baseDir, "Volume 01").apply { mkdirs() }
        val v2 = File(baseDir, "Volume 02").apply { mkdirs() }

        try {
            robot.interact {
                val txtPastaDestino =
                        robot.from(root)
                                .lookup("#txtPastaDestino")
                                .queryAs(JFXTextField::class.java)
                txtPastaDestino.text = v1.absolutePath
            }

            robot.clickOn("#btnProximaPastaDestino")
            WaitForAsyncUtils.waitForFxEvents()
            Thread.sleep(500)

            val txtPastaDestinoResult =
                    robot.from(root).lookup("#txtPastaDestino").queryAs(JFXTextField::class.java)
            assertEquals(v2.absolutePath, txtPastaDestinoResult.text)
        } finally {
            baseDir.deleteRecursively()
        }
    }
}
