package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaPastasController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.assertNotNull

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaPastasUiTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private lateinit var pastasController: AbaPastasController
    private lateinit var mockMangaService: MangaServices
    private lateinit var mockComicInfoService: ComicInfoServices

    @TempDir
    lateinit var tempDir: Path

    companion object {
        private var staticKeepAlive: java.sql.Connection? = null

        @BeforeAll
        @JvmStatic
        fun globalSetUp() {
            DataBase.isTeste = true
            DataBase.closeConnection()
            staticKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:pastas_testdb?mode=memory&cache=shared")
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
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController()
        
        val field = mainController.javaClass.getDeclaredField("pastasController")
        field.isAccessible = true
        pastasController = field.get(mainController) as AbaPastasController

        // Injetando mocks via Reflection
        mockMangaService = mock()
        mockComicInfoService = mock()
        
        val mangaServiceField = AbaPastasController::class.java.getDeclaredField("mServiceManga")
        mangaServiceField.isAccessible = true
        mangaServiceField.set(pastasController, mockMangaService)

        val comicInfoServiceField = AbaPastasController::class.java.getDeclaredField("mServiceComicInfo")
        comicInfoServiceField.isAccessible = true
        comicInfoServiceField.set(pastasController, mockComicInfoService)

        val scene = Scene(root, 1024.0, 768.0)
        
        try {
            val cssFile = File.createTempFile("jfoenix_skin_fix_pastas", ".css")
            cssFile.writeText("""
                .jfx-text-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-password-field { -fx-skin: "javafx.scene.control.skin.TextFieldSkin"; }
                .jfx-text-area { -fx-skin: "javafx.scene.control.skin.TextAreaSkin"; }
                .jfx-combo-box { -fx-skin: "javafx.scene.control.skin.ComboBoxListViewSkin"; }
            """.trimIndent())
            scene.stylesheets.add(cssFile.toURI().toURL().toExternalForm())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        robot.clickOn("Pastas")
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testCamposDefault(robot: FxRobot) {
        WaitForAsyncUtils.waitForFxEvents()
        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        assertEquals("", txtPasta.getText())
    }

    @Test
    fun testEdicaoColunasGrid(robot: FxRobot) {
        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        
        val pastaTest = Pasta(
            pasta = File("caminho/teste"),
            arquivo = "Pasta 01",
            nome = "Manga Teste",
            volume = 1f,
            capitulo = 1f,
            scan = "Scan Original",
            titulo = "Titulo Original"
        )
        
        robot.interact {
            tbViewProcessar.items.add(pastaTest)
        }

        // Editar Scan
        robot.doubleClickOn("Scan Original")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("Nova Scan").push(KeyCode.ENTER)
        
        // Editar Titulo
        robot.doubleClickOn("Titulo Original")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("Novo Titulo").push(KeyCode.ENTER)

        // Editar Volume (formatado como 01)
        robot.doubleClickOn("01")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("2").push(KeyCode.ENTER)

        // Editar Capítulo (formatado como 001)
        robot.doubleClickOn("001")
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE)
        robot.write("5").push(KeyCode.ENTER)

        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals("Nova Scan", pastaTest.scan, "Scan com valor inesperado. Valor atual: ${pastaTest.scan}")
        assertEquals("Novo Titulo", pastaTest.titulo, "Titulo com valor inesperado. Valor atual: ${pastaTest.titulo}")
        assertEquals(2f, pastaTest.volume, "Volume com valor inesperado. Valor atual: ${pastaTest.volume}")
        assertEquals(5f, pastaTest.capitulo, "Capitulo com valor inesperado. Valor atual: ${pastaTest.capitulo}")
        
        // Validar coluna formatada
        val formatado = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java).columns.last()
            .getCellData(0) as String
        assertTrue(formatado.contains("[Nova Scan]"), "Texto formatado com scan inesperado. Valor atual: ${formatado}")
        assertTrue(formatado.contains("Volume 02"), "Texto formatado com volume inesperado. Valor atual: ${formatado}")
        assertTrue(formatado.contains("Capítulo 005"), "Texto formatado com capitulo inesperado. Valor atual: ${formatado}")
    }

    @Test
    fun testCicloCompletoPastas(robot: FxRobot) {
        // Criar estrutura de pastas temporárias com scan no início para correta extração
        val sub1 = tempDir.resolve("[Scan A] Manga Vol 01 Cap 01").toFile().apply { mkdirs() }
        val sub2 = tempDir.resolve("[Scan A] Manga Vol 02 Cap 02").toFile().apply { mkdirs() }
        val sub3 = tempDir.resolve("[Scan A] Manga Vol 03 Cap 03").toFile().apply { mkdirs() }
        
        File(sub1, "pag01.jpg").createNewFile()
        File(sub2, "pag01.jpg").createNewFile()
        File(sub3, "pag01.jpg").createNewFile()

        val txtPasta = robot.lookup("#txtPasta").queryAs(JFXTextField::class.java)
        robot.interact {
            txtPasta.text = tempDir.toAbsolutePath().toString()
        }
        
        robot.clickOn("#btnCarregar")
        WaitForAsyncUtils.waitForFxEvents()
        
        val tbViewProcessar = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java) as TableView<Pasta>
        assertEquals(3, tbViewProcessar.items.size, "Quantidade de pastas inesperada. Valor atual: ${tbViewProcessar.items.size}")

        // Validar extração via Regex desas pastas
        assertEquals("Scan A", tbViewProcessar.items[0].scan, "Scan com valor inesperado. Valor atual: ${tbViewProcessar.items[0].scan}")
        assertEquals(1f, tbViewProcessar.items[0].volume, "Volume com valor inesperado. Valor atual: ${tbViewProcessar.items[0].volume}")
        assertEquals(1f, tbViewProcessar.items[0].capitulo, "Capitulo com valor inesperado. Valor atual: ${tbViewProcessar.items[0].capitulo}")

        // Simular preenchimento do manga
        robot.clickOn("#cbManga").write("Manga Teste").push(KeyCode.ENTER)
        
        // Simular perda de foco para atualizar os nomes na lista (conforme listener no controller)
        robot.clickOn("#txtPasta")
        WaitForAsyncUtils.waitForFxEvents()

        // Validar coluna formatada antes de aplicar
        val clFormatado = tbViewProcessar.columns.last()
        val formatado1 = clFormatado.getCellData(0) as String
        assertEquals("[Scan A] Manga Teste - Volume 01 Capítulo 001", formatado1, "Texto formatado com valor inesperado. Valor atual: ${formatado1}")

        // Aplicar renomeio
        robot.clickOn("#btnAplicar")
        
        // Aguarda processamento Task
        Thread.sleep(2000) 
        WaitForAsyncUtils.waitForFxEvents()

        // Validar que pastas originais foram renomeadas conforme o formato sugerido
        val files = tempDir.toFile().listFiles()
        assertTrue(files?.any { it.name == "[Scan A] Manga Teste - Volume 01 Capítulo 001" } == true, "Pasta com nome inesperado. Valor atual: ${files}")
        assertTrue(files?.any { it.name == "[Scan A] Manga Teste - Volume 02 Capítulo 002" } == true, "Pasta com nome inesperado. Valor atual: ${files}")
        assertTrue(files?.any { it.name == "[Scan A] Manga Teste - Volume 03 Capítulo 003" } == true, "Pasta com nome inesperado. Valor atual: ${files}")
    }

    @Test
    fun testCarregamentoComicInfo(robot: FxRobot) {
        // Mock do ComicInfo que deve ser retornado pelo banco
        val comicInfoFake = ComicInfo(
            java.util.UUID.randomUUID(), 456L, "Capa Naruto", "Naruto Shippuden", "Naruto", 
            "Editora Panini", "Alternate", "Arc 1", "Group 1", "Imprint 1", 
            "Ação; Aventura", "pt", AgeRating.Teen
        )
        
        whenever(mockComicInfoService.find(eq("Naruto"), any())).thenReturn(comicInfoFake)

        // Ir para a aba ComicInfo (selecionando programaticamente para evitar ambiguidade de cliques)
        val tabRoot = robot.lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java)
        robot.interact { tabRoot.selectionModel.select(1) } // 1 = aba ComicInfo
        WaitForAsyncUtils.waitForFxEvents()

        // Selecionar o manga no ComboBox
        robot.clickOn("#cbManga").write("Naruto")
        
        // Clicar em outro campo para disparar o listener de perda de foco (onde ocorre a busca no banco)
        robot.clickOn("#txtTitle")
        
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(1000)

        // Verificar se os campos foram preenchidos com os dados do mock
        assertEquals("Naruto Shippuden", robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text, "Titulo com valor inesperado. Valor atual: ${robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text}")
        assertEquals("Naruto", robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text, "Serie com valor inesperado. Valor atual: ${robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text}")
        assertEquals("Editora Panini", robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text, "Editora com valor inesperado. Valor atual: ${robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text}")
        assertEquals("Alternate", robot.lookup("#txtAlternateSeries").queryAs(JFXTextField::class.java).text, "Serie alternativa com valor inesperado. Valor atual: ${robot.lookup("#txtAlternateSeries").queryAs(JFXTextField::class.java).text}")
        
        val cbAgeRating = robot.lookup("#cbAgeRating").queryAs(JFXComboBox::class.java) as JFXComboBox<AgeRating>
        assertEquals(AgeRating.Teen, cbAgeRating.value, "Classificação etária com valor inesperado. Valor atual: ${cbAgeRating.value}")
    }

    @Test
    fun testMockMangaDatabaseSuggestion(robot: FxRobot) {
        // Garantir que os itens estão populados (simulando a carga do listar() que ocorre no init)
        val cbManga = robot.lookup("#cbManga").queryAs(JFXComboBox::class.java) as JFXComboBox<String>
        robot.interact {
            cbManga.items.setAll("Naruto", "One Piece")
        }
        
        // Sugestão ao digitar
        robot.clickOn("#cbManga").write("Nar")
        WaitForAsyncUtils.waitForFxEvents()
        
        // Não verificamos o mockMangaService.listar() aqui porque ele é chamado no initialize antes da injeção do mock
    }

    @Test
    fun testMockMalRequest(robot: FxRobot) {
        // Navegar para a aba ComicInfo (selecionando programaticamente para evitar ambiguidade de cliques)
        val tabRoot = robot.lookup("#tbTabRootPastas").queryAs(JFXTabPane::class.java)
        robot.interact { tabRoot.selectionModel.select(1) }
        WaitForAsyncUtils.waitForFxEvents()
        
        // 1. Mock de dois objetos Manga do Mal4J para resultados diferentes
        val devMalManga1 = mock<dev.katsute.mal4j.manga.Manga>()
        val malClassic = Mal(121L, "Naruto Classic", "Desc Classic", null, null, devMalManga1)
        
        val devMalManga2 = mock<dev.katsute.mal4j.manga.Manga>()
        val malShippuden = Mal(122L, "Naruto Shippuden", "Desc Shippuden", null, null, devMalManga2)
        
        whenever(mockComicInfoService.getMal(anyOrNull(), any())).thenReturn(listOf(malClassic, malShippuden))

        // Configurar updateMal para preencher o ComicInfo com dados do Mal selecionado dinamicamente
        whenever(mockComicInfoService.updateMal(any(), any(), any())).thenAnswer { invocation ->
            val comic = invocation.getArgument<ComicInfo>(0)
            val mal = invocation.getArgument<Mal>(1)
            comic.title = mal.nome
            comic.series = mal.nome + " Series"
            comic.publisher = "Editora " + mal.nome
            null
        }

        // Realizar consulta
        robot.clickOn("#txtMalNome").write("Naruto")
        robot.clickOn("#btnMalConsultar")
        
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(1000) // Espera task thread

        val tbViewMal = robot.lookup("#tbViewMal").queryAs(TableView::class.java) as TableView<Mal>
        assertEquals(2, tbViewMal.items.size, "Quantidade de itens inesperada. Valor atual: ${tbViewMal.items.size}")

        // 2. Fluxo: Duplo Clique no Primeiro Item (Naruto Classic)
        robot.doubleClickOn("Naruto Classic")
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)

        // Validar fiels após double click
        assertEquals("Naruto Classic", robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text, "Título não preenchido corretamente após duplo clique. Valor atual: ${robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text}")
        assertEquals("Naruto Classic Series", robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text, "Série não preenchida corretamente após duplo clique. Valor atual: ${robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text}")
        assertEquals("Editora Naruto Classic", robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text, "Editora não preenchida corretamente após duplo clique. Valor atual: ${robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text}")

        // 3. Fluxo: Selecionar Segundo Item (Naruto Shippuden) e Botão Aplicar
        robot.clickOn("Naruto Shippuden")
        robot.clickOn("#btnMalAplicar")
        
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(500)

        // Validar fields após botão aplicar
        assertEquals("Naruto Shippuden", robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text, "Título não atualizado corretamente após botão aplicar. Valor atual: ${robot.lookup("#txtTitle").queryAs(JFXTextField::class.java).text}")
        assertEquals("Naruto Shippuden Series", robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text, "Série não atualizada corretamente após botão aplicar. Valor atual: ${robot.lookup("#txtSeries").queryAs(JFXTextField::class.java).text}")
        assertEquals("Editora Naruto Shippuden", robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text, "Editora não atualizada corretamente após botão aplicar. Valor atual: ${robot.lookup("#txtPublisher").queryAs(JFXTextField::class.java).text}")
        
        verify(mockComicInfoService, atLeastOnce()).updateMal(any(), any(), any())
    }
}
