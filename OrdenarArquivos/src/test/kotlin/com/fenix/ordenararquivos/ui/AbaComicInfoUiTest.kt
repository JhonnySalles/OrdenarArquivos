package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaComicInfoController
import com.fenix.ordenararquivos.controller.PopupAmazon
import com.fenix.ordenararquivos.controller.PopupCapitulos
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
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
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import javafx.scene.input.KeyEvent
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
    private var mockPopupAmazon: MockedStatic<PopupAmazon>? = null
    private var mockPopupCapitulos: MockedStatic<PopupCapitulos>? = null

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

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()

        val field = mainController.javaClass.getDeclaredField("comicinfoController")
        field.isAccessible = true
        comicinfoController = field.get(mainController) as AbaComicInfoController

        mockWinrar = mock<WinrarServices>()
        mockOcrServices = mock<OcrServices>()

        val scene = Scene(root, 1024.0, 768.0)

        // Workaround for JFoenix skins
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix", ".css")
            cssFile.writeText(
                    """
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin" !important; }
                .jfx-password-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin" !important; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin" !important; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin" !important; }
                .jfx-button { -fx-skin: "javafx.scene.control.skin.ButtonSkin" !important; }
                .jfx-tab-pane { -fx-skin: "javafx.scene.control.skin.TabPaneSkin" !important; }
            """.trimIndent()
            )
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {}

        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()

        // Select Comic Info tab
        val tabPane = root.lookup("#tpGlobal") as JFXTabPane
        val tabField = mainController.javaClass.getDeclaredField("tbTabComicInfo")
        tabField.isAccessible = true
        val tab = tabField.get(mainController) as Tab
        Platform.runLater { tabPane.selectionModel.select(tab) }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @BeforeEach
    fun setUp() {
        Ocr.isTeste = true
        AlertasPopup.isTeste = true
        AlertasPopup.testResult = true
        AlertasPopup.lastAlertTitle = null
        AlertasPopup.lastAlertText = null
        File("temp").mkdirs()

        mockOcr = Mockito.mockStatic(Ocr::class.java)

        val winrarField = comicinfoController.javaClass.getDeclaredField("mRarService")
        winrarField.isAccessible = true
        winrarField.set(comicinfoController, mockWinrar)

        val ocrServicesField = comicinfoController.javaClass.getDeclaredField("mOcrService")
        ocrServicesField.isAccessible = true
        ocrServicesField.set(comicinfoController, mockOcrServices)

        AlertasPopup.isTeste = true
        AlertasPopup.lastAlertText = null
        AlertasPopup.lastAlertTitle = null

        Mockito.reset(mockWinrar)
        Mockito.reset(mockOcrServices)
    }

    @AfterEach
    fun tearDown() {
        mockOcr?.close()
        mockPopupAmazon?.close()
        mockPopupCapitulos?.close()
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
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ComicInfo><Series>Test Series</Series><Title>Test Title</Title><Pages><Page Image=\"0\" Bookmark=\"Test\"/></Pages></ComicInfo>"
        )

        whenever(mockWinrar.extraiComicInfo(any())).thenReturn(dummyComicInfoXml)
        mockOcr?.`when`<String>(MockedStatic.Verification { Ocr.process(any(), any(), any()) })
                ?.thenReturn("001-01")

        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tab = tabPane.selectionModel.selectedItem
        val tabContent = tab.content as AnchorPane

        val txtPasta = tabContent.lookup("#txtPastaProcessar") as JFXTextField
        val btnCarregar = tabContent.lookup("#btnCarregar") as JFXButton

        robot.interact { txtPasta.text = tempDirFile.absolutePath }

        // Disparar ação de carregar
        robot.interact { btnCarregar.fire() }

        // Aguardar o conteúdo com timeout e estabilização (garantir que carregarItens terminou)
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS) {
            val table = tabContent.lookup("#tbViewProcessar") as TableView<*>
            table.items.size >= numItens
        }

        // Injetar também na lista privada do controller para consistência absoluta
        robot.interact {
            val table = tabContent.lookup("#tbViewProcessar") as TableView<*>
            val field = comicinfoController.javaClass.getDeclaredField("mObsListaProcessar")
            field.isAccessible = true
            field.set(comicinfoController, table.items)
        }

        // Garantir que a renderização terminando antes de focar
        Thread.sleep(200)
        WaitForAsyncUtils.waitForFxEvents()

        val table = tabContent.lookup("#tbViewProcessar") as TableView<*>
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
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Processar>
        assertEquals(1, table.items.size)
    }

    @Test
    @Order(2)
    fun testNormalizarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        val item = table.items[0]

        // Setup initial tags
        robot.interact {
            item.tags = "0${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMAGEM}capitulo 1"
        }

        val btnNormaliza = tabContent.lookup("#btnTagsNormaliza") as JFXButton
        val cbLinguagem = tabContent.lookup("#cbLinguagem") as JFXComboBox<Linguagem>

        robot.interact {
            // Explicitly set language to PORTUGUESE for deterministic results
            cbLinguagem.value = Linguagem.PORTUGUESE
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

        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnSalvar = tabContent.lookup("#btnSalvarTodos") as JFXButton

        robot.interact { 
            btnSalvar.fire() 
        }

        // Aumenta o timeout para o processamento assíncrono em headless
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS) { !btnSalvar.isDisable }
        WaitForAsyncUtils.waitForFxEvents()
        
        Mockito.verify(mockWinrar, Mockito.atLeastOnce()).insereComicInfo(any(), any())
    }

    @Test
    @Order(4)
    fun testGerarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val btnGerar = robot.lookup("#btnTagsProcessar").queryAs(JFXButton::class.java)
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Processar>
        val item = table.items[0]

        robot.interact { btnGerar.fire() }

        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(item.tags.isNotEmpty())
    }

    @Test
    @Order(5)
    fun testAplicarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        val item = table.items[0]

        // Definir uma tag no formato "imagem|Capítulo 001 # Titulo" para testar o Split de
        // aplicação
        robot.interact {
            item.tags =
                    "0${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMAGEM} Capítulo 001 ${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMPORTACAO} Meu Titulo"
        }

        val btnAplicar = tabContent.lookup("#btnTagsAplicar") as JFXButton
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
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>

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
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnOcr = tabContent.lookup("#btnOcrProcessar") as JFXButton

        // Mocks necessários para o fluxo de OCR no controller
        val dummySumario = File(tempDir.toFile(), "sumario.jpg").apply { createNewFile() }
        whenever(mockWinrar.extraiSumario(any(), any())).thenReturn(dummySumario)
        whenever(mockOcrServices.processOcr(any(), any(), any())).thenReturn("001|05|Titulo OCR")

        robot.interact { btnOcr.fire() }

        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        // Aguarda a execução da Task de OCR com mais paciência
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS) {
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
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnCapitulos = tabContent.lookup("#btnCapitulos") as JFXButton

        robot.interact { btnCapitulos.fire() }

        // Como o PopupCapitulos não está mais mockado estaticamente, ele deve abrir.
        // Vamos apenas verificar se abriu verificando se o rootTab (nodeBlur) está com efeito.
        WaitForAsyncUtils.waitForFxEvents()
        assertNotNull(
                comicinfoController.controllerPai.rootTab.effect,
                "O efeito de blur deveria estar aplicado ao rootTab ao abrir o popup."
        )

        // Simular fechar o dialog (o JFXDialog é adicionado ao rootStack)
        robot.interact {
            mainController.rootStack.children.filterIsInstance<com.jfoenix.controls.JFXDialog>()
                    .forEach { it.close() }
        }
    }
}
