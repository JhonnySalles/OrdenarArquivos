package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import javafx.collections.FXCollections
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files

class AbaPastasControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: AbaPastasController
    private val mangaService: MangaServices = mock()
    private val comicInfoService: ComicInfoServices = mock()
    private val winrarService: WinrarServices = mock()
    private val telaInicialController: TelaInicialController = mock()

    @BeforeEach
    fun setUp() {
        controller = AbaPastasController()
        controller.controllerPai = telaInicialController

        // Injetar mocks nos campos de serviço via reflexão
        setField("mServiceManga", mangaService)
        setField("mServiceComicInfo", comicInfoService)
        setField("mRarService", winrarService)

        // Injetar campos FXML necessários
        setField("tbViewProcessar", TableView<Pasta>())
        setField("btnGerarCapas", JFXButton().apply { accessibleTextProperty().set("GERAR") })
        
        // Mock do controller pai para barras de progresso
        whenever(telaInicialController.rootProgress).thenReturn(ProgressBar())
        whenever(telaInicialController.rootMessage).thenReturn(Label())
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = findField(controller.javaClass, name)
        field.isAccessible = true
        field.set(controller, value)
    }

    private fun getField(name: String): Any? {
        val field: Field = findField(controller.javaClass, name)
        field.isAccessible = true
        return field.get(controller)
    }

    private fun findField(clazz: Class<*>, name: String): Field {
        return try {
            clazz.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            if (clazz.superclass != null) findField(clazz.superclass, name)
            else throw e
        }
    }

    @Test
    fun testOnBtnGerarCapasLogic() {
        // Criar uma pasta temporária para simular os arquivos
        val tempDir = Files.createTempDirectory("test_manga").toFile()
        val vol1Dir = File(tempDir, "[Scan] Manga - Volume 01").apply { mkdirs() }
        
        val pasta1 = Pasta(vol1Dir, vol1Dir.name, "Manga", 1.0f, scan = "Scan", isCapa = false)
        val lista = FXCollections.observableArrayList(pasta1)
        setField("mObsListaProcessar", lista)

        // Invocar onBtnGerarCapas via reflexão (é private)
        val method = controller.javaClass.getDeclaredMethod("onBtnGerarCapas")
        method.isAccessible = true
        method.invoke(controller)

        // Aguarda a execução da Task
        Thread.sleep(500)

        // Verificar se uma nova Pasta de Capa foi adicionada à lista
        val novaLista = getField("mObsListaProcessar") as List<Pasta>
        assertTrue(novaLista.any { it.isCapa && it.volume == 1.0f }, "Deveria ter gerado uma capa para o volume 1.0")
        
        val capa = novaLista.find { it.isCapa && it.volume == 1.0f }!!
        assertEquals("[Scan] Manga - Volume 01 Capa", capa.arquivo)
        assertTrue(capa.pasta.exists(), "O diretório da capa deveria ter sido criado fisicamente.")
        
        // Cleanup
        tempDir.deleteRecursively()
    }

    @Test
    fun testProcessaMoverRecursivo() {
        val tempDir = Files.createTempDirectory("test_mover").toFile()
        val raiz = File(tempDir, "raiz").apply { mkdirs() }
        val subPasta = File(raiz, "sub").apply { mkdirs() }
        val arquivo = File(subPasta, "test.txt").apply { writeText("hello") }

        val method = controller.javaClass.getDeclaredMethod("processaMoverRecursivo", File::class.java, File::class.java)
        method.isAccessible = true
        method.invoke(controller, raiz, raiz)

        assertTrue(File(raiz, "test.txt").exists(), "Arquivo deveria ter sido movido para a raiz")
        assertFalse(arquivo.exists(), "Arquivo original não deveria mais existir no subdiretório")
        
        tempDir.deleteRecursively()
    }
}
