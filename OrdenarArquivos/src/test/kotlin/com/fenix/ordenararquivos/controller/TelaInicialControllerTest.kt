package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.database.DataBase
import com.jfoenix.controls.JFXListView
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(ApplicationExtension::class)
class TelaInicialControllerTest {

    private val mLOG = LoggerFactory.getLogger(TelaInicialControllerTest::class.java)

    private lateinit var scene: Scene
    private lateinit var stage: Stage
    private lateinit var controller: TelaInicialController

    private val PASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private val ORIGEM_TEMPORARIA = File(System.getProperty("user.dir"), "temp/origem/")
    private val DESTINO_TEMPORARIA = File(System.getProperty("user.dir"), "temp/destino/")

    @Start
    private fun start(stage: Stage) {

        mLOG.info("Preparando a base de teste...")
        DataBase.isTeste = true
        val db = File(System.getProperty("user.dir"), DataBase.mDATABASE_TEST)
        if (db.exists())
            db.delete()

        mLOG.info("Iniciando a tela...")
        this.stage = stage

        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val panel = loader.load<AnchorPane>()
        scene = Scene(panel)
        scene.fill = Color.BLACK
        controller = loader.getController()
        controller.configurarAtalhos(scene)

        stage.scene = scene
        stage.title = "Testando Ordena Arquivos"
        stage.icons.add(Image(TelaInicialController::class.java.getResourceAsStream(TelaInicialController.iconLocate)))
        stage.initStyle(StageStyle.DECORATED)
        stage.minWidth = 700.0
        stage.minHeight = 600.0

        stage.onCloseRequest = EventHandler {
            DataBase.closeConnection()
            exitProcess(0)
        }

        stage.show()
    }

    @BeforeAll
    fun preparaTeste(robot: FxRobot) {
        if (!PASTA_TEMPORARIA.exists())
            PASTA_TEMPORARIA.mkdir()
        else {
            for (item in PASTA_TEMPORARIA.listFiles())
                item.delete()
        }

        if (!ORIGEM_TEMPORARIA.exists())
            ORIGEM_TEMPORARIA.mkdir()

        if (!DESTINO_TEMPORARIA.exists())
            DESTINO_TEMPORARIA.mkdir()

        copiaItem(Paths.get("src", "test", "resources", "01 - Frente.jpg").toFile(), ORIGEM_TEMPORARIA)
        copiaItem(Paths.get("src", "test", "resources", "02 - Tras.jpg").toFile(), ORIGEM_TEMPORARIA)
        copiaItem(Paths.get("src", "test", "resources", "03 - Tudo.png").toFile(), ORIGEM_TEMPORARIA)
        copiaItem(Paths.get("src", "test", "resources", "04 - Sumario.jpg").toFile(), ORIGEM_TEMPORARIA)
    }

    private fun copiaItem(arquivo: File, destino: File, nome: String = arquivo.name): Path {
        val arquivoDestino = Paths.get(destino.toPath().toString() + "/" + nome)
        Files.copy(arquivo.toPath(), arquivoDestino, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        return arquivoDestino
    }

    @AfterAll
    fun limpaTeste() {
        if (ORIGEM_TEMPORARIA.exists()) {
            for (item in ORIGEM_TEMPORARIA.listFiles())
                item.delete()
            ORIGEM_TEMPORARIA.delete()
        }

        if (DESTINO_TEMPORARIA.exists()) {
            for (item in DESTINO_TEMPORARIA.listFiles())
                item.delete()
            DESTINO_TEMPORARIA.delete()
        }

        if (PASTA_TEMPORARIA.exists()) {
            for (item in PASTA_TEMPORARIA.listFiles())
                item.delete()
        }
    }

    @Test
    @Order(0)
    fun preparaCampos(robot: FxRobot) {
        robot.lookup("txtNomePastaManga").queryAs(JFXTextField::class.java).text = "[JPN] Teste da tela inicial - "
        robot.lookup("txtPastaOrigem").queryAs(JFXTextField::class.java).text = ORIGEM_TEMPORARIA.path
        robot.lookup("txtPastaDestino").queryAs(JFXTextField::class.java).text = DESTINO_TEMPORARIA.path
    }

    @Test
    @Order(1)
    fun testeGerarCapitulos(robot: FxRobot) {
        val txtInicio = robot.lookup("txtGerarInicio").queryAs(JFXTextField::class.java)
        val txtFim = robot.lookup("txtGerarFim").queryAs(JFXTextField::class.java)
        txtInicio.text = "1"
        txtFim.text = "10"
        robot.clickOn("btnGerar")

        val txtAImportar = robot.lookup("txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertEquals(txtAImportar.text, "001-\n002-\n003-\n004-\n005-\n006-\n007-\n008-\n009-\n010-")
    }

    @Test
    @Order(2)
    fun testeIncrementaVolume(robot: FxRobot) {
        val txtInicio = robot.lookup("txtVolume").queryAs(JFXTextField::class.java)
        val txtFim = robot.lookup("txtGerarFim").queryAs(JFXTextField::class.java)

        robot.clickOn("btnVolumeMais")

        assertEquals(txtInicio.text, "11")
        assertEquals(txtFim.text, "20")

        val txtAImportar = robot.lookup("txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertEquals(txtAImportar.text, "011-\n012-\n013-\n014-\n015-\n016-\n017-\n018-\n019-\n020-")
    }

    @Test
    @Order(3)
    fun testeDecrementaVolume(robot: FxRobot) {
        val txtInicio = robot.lookup("txtVolume").queryAs(JFXTextField::class.java)
        val txtFim = robot.lookup("txtGerarFim").queryAs(JFXTextField::class.java)

        robot.clickOn("btnVolumeMenos")

        assertEquals(txtInicio.text, "1")
        assertEquals(txtFim.text, "10")

        val txtAImportar = robot.lookup("txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertEquals(txtAImportar.text, "001-\n002-\n003-\n004-\n005-\n006-\n007-\n008-\n009-\n010-")
    }

    @Test
    @Order(4)
    fun testeGerarSequencia(robot: FxRobot) {
        val txtInicio = robot.lookup("txtVolume").queryAs(JFXTextField::class.java)
        val txtFim = robot.lookup("txtGerarFim").queryAs(JFXTextField::class.java)

        robot.clickOn("btnVolumeMenos")

        assertEquals(txtInicio.text, "1")
        assertEquals(txtFim.text, "10")

        val txtAImportar = robot.lookup("txtAreaImportar").queryAs(JFXTextArea::class.java)
        assertEquals(txtAImportar.text, "001-\n002-\n003-\n004-\n005-\n006-\n007-\n008-\n009-\n010-")
    }

    @Test
    @Order(4)
    fun testeItens(robot: FxRobot) {
        val lsVwImagens = robot.lookup("lsVwListaImagens").queryAs(JFXListView::class.java) as JFXListView<String>
        robot.lookup("txtPastaOrigem").queryAs(JFXTextField::class.java).requestFocus()
        robot.lookup("txtPastaDestino").queryAs(JFXTextField::class.java).requestFocus()

        assertTrue(lsVwImagens.items.isNotEmpty())
    }
}