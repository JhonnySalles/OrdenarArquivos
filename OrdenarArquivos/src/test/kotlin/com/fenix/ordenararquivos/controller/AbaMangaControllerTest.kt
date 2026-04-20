package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXTextField
import javafx.scene.control.TableView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.lang.reflect.Field

class AbaMangaControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: AbaMangaController
    private val mangaService: MangaServices = mock()
    private val comicInfoService: ComicInfoServices = mock()
    private val telaInicialController: TelaInicialController = mock()

    @BeforeEach
    fun setUp() {
        controller = AbaMangaController()
        controller.controllerPai = telaInicialController

        // Injetar mocks nos campos de serviço via reflexão
        setField("mServiceManga", mangaService)
        setField("mServiceComicInfo", comicInfoService)

        // Injetar campos FXML necessários para evitar NPE
        val txtFiltro = JFXTextField()
        txtFiltro.text = ""
        setField("txtFiltro", txtFiltro)
        setField("tbViewManga", TableView<Manga>())
        
        // Mock das colunas se necessário, mas para testes de lógica básica vamos focar no estado
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    @Test
    fun testCarregarDadosChamadaServico() {
        whenever(mangaService.findAll(anyOrNull(), any(), any())).thenReturn(listOf(Manga(nome = "Test")))

        // Invocar carregarDados (é private, usamos reflexão ou chamamos via Initialize se possível)
        // Como initialize chama carregarDados, vamos tentar invocar o método carregarDados diretamente
        val method = controller.javaClass.getDeclaredMethod("carregarDados", Boolean::class.java)
        method.isAccessible = true
        method.invoke(controller, false)

        // Aguarda um pouco pois o carregarDados roda em uma Thread/Task (simulado no robot ou Thread.sleep curto)
        Thread.sleep(200) 

        verify(mangaService).findAll(eq(""), eq(1000), eq(0))
        
        val mangas = getField("mMangas") as List<Manga>
        assertEquals(1, mangas.size)
        assertEquals("Test", mangas[0].nome)
    }

    @Test
    fun testSalvarManga() {
        val manga = Manga(id = 1, nome = "Naruto", comic = "Ninja")
        
        val method = controller.javaClass.getDeclaredMethod("salvarManga", Manga::class.java)
        method.isAccessible = true
        
        // Mock do controller pai para evitar NPE ao acessar rootMessage
        val lblMessage = javafx.scene.control.Label()
        whenever(telaInicialController.rootMessage).thenReturn(lblMessage)

        method.invoke(controller, manga)

        // Usamos Mockito.mockingDetails para inspecionar as chamadas de ambos os serviços.
        // Isso evita erros de 'InvalidUseOfMatchers' causados por sobrecargas Kotlin e parâmetros default.
        
        val mangaInvocations = org.mockito.Mockito.mockingDetails(mangaService).invocations
        val mangaSaveCall = mangaInvocations.find { it.method.name == "save" }
        assertNotNull(mangaSaveCall, "O método mServiceManga.save deveria ter sido chamado.")
        assertEquals("Naruto", (mangaSaveCall!!.arguments[0] as Manga).nome)

        val comicInvocations = org.mockito.Mockito.mockingDetails(comicInfoService).invocations
        val comicSaveCall = comicInvocations.find { it.method.name == "save" }
        assertNotNull(comicSaveCall, "O método mServiceComicInfo.save deveria ter sido chamado.")
    }

    private fun getField(name: String): Any? {
        val field: Field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(controller)
    }
}
