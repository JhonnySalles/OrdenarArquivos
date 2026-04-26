package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@Tag("UI")
@ExtendWith(ApplicationExtension::class, MockitoExtension::class)
class AbaPastasControllerTest {

    private lateinit var controller: AbaPastasController

    @Mock
    lateinit var mockServiceWinrar: WinrarServices

    @Mock
    lateinit var mockServiceManga: MangaServices

    @Mock
    lateinit var mockServiceComicInfo: ComicInfoServices

    @Mock
    lateinit var mockTelaInicial: TelaInicialController

    private val tempDir = File("temp")
    private val origemDir = File(tempDir, "origem")
    private val destinoDir = File(tempDir, "destino")

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupHeadless() {
            if (System.getProperty("os.name").contains("Windows").not()) {
                System.setProperty("testfx.robot", "glass")
                System.setProperty("testfx.headless", "true")
                System.setProperty("prism.order", "sw")
                System.setProperty("glass.platform", "Monocle")
                System.setProperty("monocle.platform", "Headless")
            }
        }
    }

    @Start
    fun start(stage: Stage) {
        val fxmlUrl = javaClass.getResource("/view/AbaPastas.fxml")
        if (fxmlUrl == null) {
            throw IllegalStateException("FXML file not found: /view/AbaPastas.fxml")
        }
        val loader = FXMLLoader(fxmlUrl)
        val root: AnchorPane = loader.load()
        controller = loader.getController()

        // Mock UI elements of parent controller
        val pb = ProgressBar()
        val lbl = Label()
        `when`(mockTelaInicial.rootProgress).thenReturn(pb)
        `when`(mockTelaInicial.rootMessage).thenReturn(lbl)

        // Inject mocks
        controller.mServiceWinrar = mockServiceWinrar
        controller.mServiceManga = mockServiceManga
        controller.mServiceComicInfo = mockServiceComicInfo
        controller.controllerPai = mockTelaInicial

        val scene = Scene(root)
        stage.scene = scene
        stage.show()
    }

    @BeforeEach
    fun setUp() {
        // Clear temp directory
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
        tempDir.mkdirs()
        origemDir.mkdirs()
        destinoDir.mkdirs()

        // Extract test.zip
        val zipStream = javaClass.getResourceAsStream("/test.zip")
        if (zipStream != null) {
            ZipInputStream(zipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(origemDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    @Test
    fun testCarregarPastas(robot: FxRobot) {
        // Click on search button
        robot.clickOn("#btnPesquisarPasta")

        // Wait for FX events
        WaitForAsyncUtils.waitForFxEvents()

        // Check if path is set in text field
        val txtPasta = robot.lookup("#txtPasta").queryAs(javafx.scene.control.TextField::class.java)
        assertEquals(origemDir.absolutePath, txtPasta.text)
        
        // Click on Load button
        robot.clickOn("#btnCarregar")
        
        // Wait for task completion
        WaitForAsyncUtils.waitForFxEvents()
        
        // We might need to wait for the background task to finish.
        Thread.sleep(1000) 
        WaitForAsyncUtils.waitForFxEvents()

        // Verify table has items
        val tbView = robot.lookup("#tbViewProcessar").queryAs(javafx.scene.control.TableView::class.java)
        assertTrue(tbView.items.size > 0, "Table should have at least one item")
    }

    @Test
    fun testProcessaArquivosRar(robot: FxRobot) {
        val method = controller.javaClass.getDeclaredMethod("processaArquivosRar", List::class.java)
        method.isAccessible = true

        val cbManga = robot.lookup("#cbManga").queryAs(com.jfoenix.controls.JFXComboBox::class.java)

        val cenarios = listOf(
            "Manga Teste Volume 01.rar" to "Manga Teste",
            "MANGA TESTE VOLUME 01.rar" to "MANGA TESTE",
            "Outro Manga Capítulo 05.rar" to "Outro Manga",
            "Manga Chapter 10.rar" to "Manga",
            "Manga capitulo 15.rar" to "Manga",
            "Manga - 01.rar" to "Manga"
        )

        cenarios.forEach { (nomeArquivo, esperado) ->
            robot.interact {
                method.invoke(controller, listOf(File(nomeArquivo)))
            }
            // Espera a Task terminar
            Thread.sleep(500)
            WaitForAsyncUtils.waitForFxEvents()
            
            assertEquals(esperado, cbManga.editor.text, "Erro no nome do mangá para: $nomeArquivo")
        }
    }

    @Test
    fun testCarregarPastasComComicInfo(robot: FxRobot) {
        val pastaManga = File(origemDir, "Manga Teste")
        pastaManga.mkdirs()
        val pastaCapitulo = File(pastaManga, "Chapter 65.2")
        pastaCapitulo.mkdirs()

        // Criar ComicInfo.xml
        val comicInfo = ComicInfo().apply {
            title = "Chapter 65.2: Vou tirá-lo do meu caminho (parte 2)"
            number = 65.2f
            volume = 1
        }
        val comicFile = File(pastaCapitulo, "ComicInfo.xml")
        val context = JAXBContext.newInstance(ComicInfo::class.java)
        val marshaller = context.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        FileOutputStream(comicFile).use { fos ->
            marshaller.marshal(comicInfo, fos)
        }

        robot.interact {
            robot.lookup("#txtPasta").queryAs(javafx.scene.control.TextField::class.java).text = pastaManga.absolutePath
        }

        robot.clickOn("#btnCarregar")
        
        // Esperar carregamento
        Thread.sleep(1500) 
        WaitForAsyncUtils.waitForFxEvents()

        val tbView = robot.lookup("#tbViewProcessar").queryAs(javafx.scene.control.TableView::class.java)
        val items = tbView.items
        assertTrue(items.size > 0, "Deveria ter carregado o item")
        
        val item = items[0] as com.fenix.ordenararquivos.model.entities.Pasta
        assertEquals(65.2f, item.capitulo, "Capítulo deveria vir do XML")
        assertEquals(1f, item.volume, "Volume deveria vir do XML")
        assertEquals("Vou tirá-lo do meu caminho (parte 2)", item.titulo, "Título deveria vir do XML sem o prefixo")
    }

    @Test
    fun testProgressBarUpdates(robot: FxRobot) {
        val method = controller.javaClass.getDeclaredMethod("processaArquivosRar", List::class.java)
        method.isAccessible = true

        val rarFile = File(origemDir, "teste.rar")
        rarFile.createNewFile()

        robot.interact {
            method.invoke(controller, listOf(rarFile))
        }

        // Verifica estados intermediários (aproximadamente)
        // Como o processamento é rápido no mock, talvez precisemos de um delay no mock se quisermos pegar 0.5
        // Mas podemos validar o início e o fim.
        val progress = mockTelaInicial.rootProgress.progress
        assertTrue(progress >= 0.0, "Progresso deve começar em >= 0")
        
        Thread.sleep(1000)
        WaitForAsyncUtils.waitForFxEvents()
        
        assertEquals(1.0, mockTelaInicial.rootProgress.progress, 0.01, "Progresso deve terminar em 1.0")
    }

    @Test
    fun testProcessaArquivoXml(robot: FxRobot) {
        val method = controller.javaClass.getDeclaredMethod("processaArquivoXml", File::class.java)
        method.isAccessible = true

        val comicInfo = ComicInfo().apply {
            series = "Serie XML"
            volume = 10
        }
        val xmlFile = File(tempDir, "ComicInfo.xml")
        val context = JAXBContext.newInstance(ComicInfo::class.java)
        val marshaller = context.createMarshaller()
        marshaller.marshal(comicInfo, xmlFile)

        robot.interact {
            method.invoke(controller, xmlFile)
        }

        val cbManga = robot.lookup("#cbManga").queryAs(com.jfoenix.controls.JFXComboBox::class.java)
        assertEquals("Serie XML", cbManga.editor.text)
        
        val txtVolume = robot.lookup("#txtVolume").queryAs(com.jfoenix.controls.JFXTextField::class.java) // Na verdade o campo de volume fica em outro lugar?
        // Na AbaPastas, volume/capitulo vêm do ComicInfo carregado
        assertEquals(10, controller.mComicInfo.volume)
    }

    @Test
    fun testCbMangaChangeUpdatesList(robot: FxRobot) {
        // Prepare some items in the list
        robot.interact {
            controller.mObsListaProcessar.add(com.fenix.ordenararquivos.model.entities.Pasta(pasta = File("pasta1")).apply { arquivo = "arquivo1" })
            controller.mObsListaProcessar.add(com.fenix.ordenararquivos.model.entities.Pasta(pasta = File("pasta2")).apply { arquivo = "arquivo2" })
        }

        val cbManga = robot.lookup("#cbManga").queryAs(com.jfoenix.controls.JFXComboBox::class.java)
        
        // Simular foco e alteração de texto
        robot.clickOn(cbManga.editor)
        robot.write("Novo Nome Manga")
        
        // Tirar o foco para disparar o listener
        robot.clickOn("#btnCarregar") 
        
        WaitForAsyncUtils.waitForFxEvents()
        
        // Verificar se os itens na lista foram atualizados
        controller.mObsListaProcessar.forEach { item ->
            assertEquals("Novo Nome Manga", item.nome, "Item ${item.arquivo.name} não foi atualizado")
        }
    }
}

