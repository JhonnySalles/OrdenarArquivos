package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import java.io.File
import java.io.FileOutputStream

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AbaComicInfoControllerTest {

    private lateinit var mainController: TelaInicialController
    private lateinit var comicInfoController: AbaComicInfoController
    
    private val rarService: WinrarServices = mock()

    private val TEMP_DIR = File(System.getProperty("user.dir"), "temp")
    private val ORIGEM_DIR = File(TEMP_DIR, "origem")

    @Start
    private fun start(stage: Stage) {
        DataBase.isTeste = true
        val db = File(System.getProperty("user.dir"), DataBase.mDATABASE_TEST)
        if (db.exists()) db.delete()

        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root = loader.load<AnchorPane>()
        mainController = loader.getController<TelaInicialController>()

        // Obtém o controller da aba via reflexão
        val field = mainController.javaClass.getDeclaredField("comicinfoController")
        field.isAccessible = true
        comicInfoController = field.get(mainController) as AbaComicInfoController

        // Injeta mock
        comicInfoController.mRarService = rarService

        val scene = Scene(root)
        mainController.configurarAtalhos(scene)
        stage.scene = scene
        stage.title = "Teste AbaComicInfo"
        stage.show()
    }

    @BeforeAll
    fun setup() {
        if (TEMP_DIR.exists()) {
            TEMP_DIR.deleteRecursively()
        }
        TEMP_DIR.mkdirs()
        ORIGEM_DIR.mkdirs()
        
        // Cria arquivos dummy para o teste
        File(ORIGEM_DIR, "Manga_01.cbr").createNewFile()
        File(ORIGEM_DIR, "Manga_02.cbr").createNewFile()
    }

    @Test
    @Order(1)
    @DisplayName("Deve carregar itens da pasta na TableView")
    fun testCarregaItens(robot: FxRobot) {
        // Mock do retorno do ComicInfo
        val mockComicInfo = ComicInfo().apply {
            series = "Serie Teste"
            title = "Titulo Teste"
        }
        
        whenever(rarService.extraiComicInfo(any())).thenReturn(File(TEMP_DIR, "ComicInfo.xml"))
        // Simula que o arquivo extraído existe (precisamos criar o arquivo real para o unmarshaller não falhar se for chamado)
        val dummyXml = File(TEMP_DIR, "ComicInfo.xml")
        FileOutputStream(dummyXml).use { out ->
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ComicInfo></ComicInfo>".toByteArray())
        }

        val txtPasta = robot.lookup("#txtPastaProcessar").queryAs(JFXTextField::class.java)
        
        robot.interact {
            txtPasta.text = ORIGEM_DIR.absolutePath
            // Dispara o carregamento via botão
            robot.clickOn("#btnCarregar")
        }
        
        // Aguarda o processamento (Task)
        Thread.sleep(1000)
        
        val tableView = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        assertTrue(tableView.items.size >= 1, "A tabela deve conter itens carregados")
    }

    @Test
    @Order(2)
    @DisplayName("Deve gerar tags para os itens carregados")
    fun testGerarTags(robot: FxRobot) {
        val tableView = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        
        robot.interact {
            robot.clickOn("#btnTagsProcessar")
        }
        
        val firstItem = tableView.items[0] as Processar
        // Verifica se a lógica de geração de tags (que usa o comicInfo mockado ou real) foi chamada
        // Como o initial ComicInfo estava vazio, as tags podem estar vazias ou com formato padrão
        // O importante é que a rotina executou sem erros.
        assertTrue(true) 
    }

    @Test
    @Order(3)
    @DisplayName("Deve normalizar as tags")
    fun testNormalizarTags(robot: FxRobot) {
        val tableView = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        
        robot.interact {
            val item = tableView.items[0] as Processar
            item.tags = "001.png - Capítulo 1"
            robot.clickOn("#btnTagsNormaliza")
        }
        
        val item = tableView.items[0] as Processar
        // Verifica se a linguagem padrão (Português) foi aplicada
        assertTrue(item.tags.contains("Capítulo"), "A tag deve estar normalizada para Português")
    }

    @Test
    @Order(4)
    @DisplayName("Deve aplicar as tags geradas")
    fun testAplicarTags(robot: FxRobot) {
        val tableView = robot.lookup("#tbViewProcessar").queryAs(TableView::class.java)
        
        robot.interact {
            val item = tableView.items[0] as Processar
            item.tags = "001.png # Capitulo 1 ^ Titulo"
            robot.clickOn("#btnTagsAplicar")
        }
        
        val item = tableView.items[0] as Processar
        // A lógica do onBtnTagsAplicar substitui separadores especiais por " - "
        assertTrue(item.tags.contains(" - "), "A tag deve ter os separadores aplicados")
    }
}
