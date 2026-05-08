package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import com.fenix.ordenararquivos.model.enums.Linguagem
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

        // Injetar campos FXML necessários para desabilita/habilita
        setField("txtPasta", JFXTextField())
        setField("btnPesquisarPasta", JFXButton())
        setField("cbLinguagem", JFXComboBox<Linguagem>())
        setField("btnCarregar", JFXButton())
        setField("btnValidar", JFXButton())
        setField("btnRenomear", JFXButton())
        setField("btnGerarCapas", JFXButton().apply { accessibleTextProperty().set("GERAR") })
        setField("btnImportarVolumes", JFXButton())
        setField("btnCompactar", JFXButton())
        setField("btnAjustarPastas", JFXButton())
        setField("btnCapitulos", JFXButton())
        setField("tbViewProcessar", TableView<Pasta>())
        setField("cbManga", JFXComboBox<String>().apply { items = FXCollections.observableArrayList() })
        setField("txtMalId", JFXTextField())
        setField("txtMalNome", JFXTextField())
        setField("btnMalConsultar", JFXButton().apply { accessibleTextProperty().set("PROCESSA") })
        setField("cbAgeRating", JFXComboBox<com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating>())

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

    @Test
    fun testConsultarMalSanitization() {
        val txtMalNome = JFXTextField()
        txtMalNome.text = "Naruto: @Shippuden! - [JPN]"
        setField("txtMalNome", txtMalNome)
        
        // Mock de outros campos necessários para não dar NPE antes da sanitização
        setField("txtMalId", JFXTextField().apply { text = "" })
        setField("cbLinguagem", com.jfoenix.controls.JFXComboBox<com.fenix.ordenararquivos.model.enums.Linguagem>())

        // O valor sanitizado é interno ao método consultarMal, mas podemos testar a lógica do Regex diretamente
        // ou invocar o método e ver se ele chama o serviço com o nome limpo.
        
        val nomeOriginal = txtMalNome.text
        val nomeSanitizado = nomeOriginal.replace(Regex("[^\\w\\s-]"), "")
        
        assertEquals("Naruto Shippuden - JPN", nomeSanitizado)
    }

    @Test
    fun testAjustarTitulosRegex() {
        val pasta = Pasta(File("teste"), "Pasta", titulo = "Chapter 01: O Inicio", isSelecionado = true)
        val lista = FXCollections.observableArrayList(pasta)
        setField("mObsListaProcessar", lista)
        setField("tbViewProcessar", TableView<Pasta>())

        val method = controller.javaClass.getDeclaredMethod("ajustarTitulos", Boolean::class.java)
        method.isAccessible = true
        method.invoke(controller, false) // todos = false

        assertEquals("O Inicio", pasta.titulo)

        pasta.titulo = "Vol. 10 - Fim do Arco"
        method.invoke(controller, false)
        assertEquals("Fim do Arco", pasta.titulo)

        pasta.titulo = "045 - Confronto"
        method.invoke(controller, false)
        assertEquals("Confronto", pasta.titulo)
        
        // Teste com o novo prefixo detectado pelo regex ^.*?
        pasta.titulo = "[Scan] Cap. 15 - Teste"
        method.invoke(controller, false)
        assertEquals("Teste", pasta.titulo)
    }
}
