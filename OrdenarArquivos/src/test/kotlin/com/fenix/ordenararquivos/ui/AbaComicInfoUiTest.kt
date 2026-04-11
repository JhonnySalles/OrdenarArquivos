package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaComicInfoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXComboBox
import com.fenix.ordenararquivos.controller.PopupAmazon
import com.fenix.ordenararquivos.controller.PopupCapitulos
import com.fenix.ordenararquivos.notification.AlertasPopup
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.control.Tab
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.scene.input.KeyCode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.verification.VerificationMode
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.*
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaComicInfoUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var comicinfoController: AbaComicInfoController
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var mockWinrar: WinrarServices
    private lateinit var mockOcr: MockedStatic<Ocr>
    private lateinit var mockPopupAmazon: MockedStatic<PopupAmazon>
    private lateinit var mockPopupCapitulos: MockedStatic<PopupCapitulos>
    private lateinit var mockAlertas: MockedStatic<AlertasPopup>

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb_comicinfo?mode=memory&cache=shared")
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
        
        val scene = Scene(root, 1024.0, 768.0)
        
        // Workaround for JFoenix skins
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin" !important; }
                .jfx-password-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin" !important; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin" !important; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin" !important; }
                .jfx-button { -fx-skin: "javafx.scene.control.skin.ButtonSkin" !important; }
                .jfx-tab-pane { -fx-skin: "javafx.scene.control.skin.TabPaneSkin" !important; }
            """.trimIndent())
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
        Platform.runLater {
            tabPane.selectionModel.select(tab)
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @BeforeEach
    fun setUp() {
        if (::mockOcr.isInitialized) {
            try { mockOcr.close() } catch (e: Exception) {}
        }
        mockOcr = Mockito.mockStatic(Ocr::class.java)
        mockPopupAmazon = Mockito.mockStatic(PopupAmazon::class.java)
        mockPopupCapitulos = Mockito.mockStatic(PopupCapitulos::class.java)
        mockAlertas = Mockito.mockStatic(AlertasPopup::class.java)
        mockAlertas.`when`<Boolean>(MockedStatic.Verification { AlertasPopup.confirmacaoModal(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()) }).thenReturn(true)
        
        val winrarField = comicinfoController.javaClass.getDeclaredField("mRarService")
        winrarField.isAccessible = true
        winrarField.set(comicinfoController, mockWinrar)
        
        Mockito.reset(mockWinrar)
    }

    @AfterEach
    fun tearDown() {
        if (::mockOcr.isInitialized) mockOcr.close()
        if (::mockPopupAmazon.isInitialized) mockPopupAmazon.close()
        if (::mockPopupCapitulos.isInitialized) mockPopupCapitulos.close()
        if (::mockAlertas.isInitialized) mockAlertas.close()
    }

    private fun helperCarregarItens(robot: FxRobot) {
        val tempDirFile = tempDir.toFile()
        val dummyFile = File(tempDirFile, "test_manga_001.rar")
        dummyFile.createNewFile()
        
        val dummyComicInfoXml = File(tempDirFile, "ComicInfo.xml")
        dummyComicInfoXml.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ComicInfo><Series>Test Series</Series><Title>Test Title</Title><Pages><Page Image=\"0\" Bookmark=\"Test\"/></Pages></ComicInfo>")
        
        whenever(mockWinrar.extraiComicInfo(org.mockito.ArgumentMatchers.any())).thenReturn(dummyComicInfoXml)
        mockOcr.`when`<String>(MockedStatic.Verification { Ocr.process(org.mockito.ArgumentMatchers.any(java.io.File::class.java), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()) }).thenReturn("001-01")

        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tab = tabPane.selectionModel.selectedItem
        val tabContent = tab.content as AnchorPane

        val txtPasta = tabContent.lookup("#txtPastaProcessar") as JFXTextField
        val btnCarregar = tabContent.lookup("#btnCarregar") as JFXButton
        
        robot.interact {
            txtPasta.text = tempDirFile.absolutePath
            btnCarregar.fire()
        }
        
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS) {
            val table = tabContent.lookup("#tbViewProcessar") as TableView<*>
            (table.items?.size ?: 0) > 0
        }
    }

    @Test
    fun testCarregarItens(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        assertEquals(1, table.items.size)
    }

    @Test
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
        assertTrue(item.tags.contains("Capítulo 001"), "A tag deveria ser normalizada para 'Capítulo 001', atual: '${item.tags}'")
    }

    @Test
    fun testSalvarTodos(robot: FxRobot) {
        helperCarregarItens(robot)
        whenever(mockWinrar.insereComicInfo(any(), any())).thenReturn(true)
        
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnSalvar = tabContent.lookup("#btnSalvarTodos") as JFXButton
        
        robot.interact {
            btnSalvar.fire()
        }
        
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS) {
            !btnSalvar.isDisable
        }
        Mockito.verify(mockWinrar, Mockito.atLeastOnce()).insereComicInfo(any(), any())
    }

    @Test
    fun testGerarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnGerar = tabContent.lookup("#btnTagsProcessar") as JFXButton
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        val item = table.items[0]

        // Inicialmente as tags podem vir do helperCarregarItens. No teste normalizamos para garantir.
        robot.interact {
            btnGerar.fire()
        }
        
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(item.tags.isNotEmpty())
    }

    @Test
    fun testAplicarTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        val item = table.items[0]

        // Definir uma tag no formato "imagem|Capítulo 001 # Titulo" para testar o Split de aplicação
        robot.interact {
            item.tags = "0${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMAGEM} Capítulo 001 ${com.fenix.ordenararquivos.util.Utils.SEPARADOR_IMPORTACAO} Meu Titulo"
        }

        val btnAplicar = tabContent.lookup("#btnTagsAplicar") as JFXButton
        robot.interact {
            btnAplicar.fire()
        }

        WaitForAsyncUtils.waitForFxEvents()
        // Validar se as tags foram de fato processadas (conter o capitulo e titulo)
        assertTrue(item.tags.contains("Capítulo 001"), "Tags deveriam conter o capítulo. Atual: ${item.tags}")
        assertTrue(item.tags.contains("Meu Titulo"), "Tags deveriam conter o título. Atual: ${item.tags}")
    }

    @Test
    fun testMenuContextoRemover(robot: FxRobot) {
        helperCarregarItens(robot)
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        assertEquals(1, table.items.size)

        robot.interact {
            table.selectionModel.select(0)
        }

        // Clicar com o botão direito na primeira célula do arquivo para abrir o menu
        val firstCell = robot.lookup(".table-cell").nth(1).queryAs(Node::class.java)
        robot.rightClickOn(firstCell as Node)
        robot.clickOn("Remover registro")
        
        // Pode abrir popup de confirmação (se o AlertasPopup.confirmacaoModal estiver habilitado)
        // Como AlertasPopup é estático, se ele abrir um JFXDialog real, precisamos lidar com ele.
        // No momento o teste pode travar se o dialog abrir. Idealmente AlertasPopup deveria ser mockado
        // mas é um singleton estático. 
        
        // Verificamos se houve tentativa de remoção
        WaitForAsyncUtils.waitForFxEvents()
        // Se o dialog estiver aberto, clicamos no botão de confirmação
        try {
            robot.clickOn("Sim")
        } catch (e: Exception) {}

        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(0, table.items.size)
    }

    @Test
    fun testOcrProcessarTask(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnOcr = tabContent.lookup("#btnOcrProcessar") as JFXButton
        
        // Mock do processo de OCR pra retornar algo conhecido
        mockOcr.`when`<String> { Ocr.process(any(), any(), any()) }.thenReturn("001 # Titulo OCR")

        robot.interact {
            btnOcr.fire()
        }

        // Aguarda a execução da Task de OCR
        WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS) {
            !btnOcr.isDisable
        }

        val table = tabContent.lookup("#tbViewProcessar") as TableView<Processar>
        assertTrue(table.items[0].tags.contains("Titulo OCR"), "Tags não contém o texto vindo do OCR. Atual: ${table.items[0].tags}")
    }

    @Test
    fun testBotaoCapitulos(robot: FxRobot) {
        helperCarregarItens(robot)
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        val tabContent = tabPane.selectionModel.selectedItem.content as AnchorPane
        val btnCapitulos = tabContent.lookup("#btnCapitulos") as JFXButton

        robot.interact {
            btnCapitulos.fire()
        }

        mockPopupCapitulos.verify(MockedStatic.Verification {
            PopupCapitulos.abreTelaCapitulos(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())
        }, times(1))
    }

    @Test
    fun testBotaoAmazon(robot: FxRobot) {
        helperCarregarItens(robot)
        
        // Buscar a célula na coluna clProcessarAmazon (índice 9)
        val btnAmazon = robot.lookup(".table-cell").nth(9).lookup(".button").queryAs(JFXButton::class.java)

        robot.interact {
            btnAmazon.fire()
        }

        mockPopupAmazon.verify(MockedStatic.Verification { 
            PopupAmazon.abreTelaAmazon(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())
        }, times(1))
    }

    @Test
    fun testAtalhosColunaTags(robot: FxRobot) {
        helperCarregarItens(robot)
        val table = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        
        // Localizar especificamente a célula na coluna Tags (índice 7)
        val cellTags = robot.lookup(".table-cell").nth(7).queryAs(Node::class.java)
        
        robot.interact {
            table.selectionModel.select(0)
            cellTags.requestFocus()
        }
        
        // Simular o foco e pressionar atalho Shift+Alt+Enter
        robot.clickOn(cellTags as Node)
        robot.press(KeyCode.SHIFT, KeyCode.ALT).type(KeyCode.ENTER).release(KeyCode.SHIFT, KeyCode.ALT)
        
        WaitForAsyncUtils.waitForFxEvents()
        
        val item = table.items[0] as Processar
        // O atalho deve disparar a formatação (substituindo o separador interno por " - ")
        assertTrue(item.tags.contains(" - "), "O atalho Shift+Alt+Enter não aplicou as tags corretamente. Atual: ${item.tags}")
    }
}
