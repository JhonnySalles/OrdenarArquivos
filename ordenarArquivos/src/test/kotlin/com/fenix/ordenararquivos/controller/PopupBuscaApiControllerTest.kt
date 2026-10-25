package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.api.ApiComicInfoResumo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

class PopupBuscaApiControllerTest : BaseJfxTest() {

    private lateinit var controller: PopupBuscaApiController

    @BeforeEach
    fun setUp() {
        controller = PopupBuscaApiController()

        // Injetar campos FXML
        setField("apRoot", AnchorPane())
        setField("txtPesquisa", JFXTextField())
        setField("cbLinguagem", JFXComboBox<Linguagem>())
        setField("btnPesquisar", JFXButton())
        setField("lblStatus", Label())
        setField("pbCarregando", ProgressBar())

        setField("tbViewResultados", TableView<ApiComicInfoResumo>())
        setField("clVolume", TableColumn<ApiComicInfoResumo, String>())
        setField("clNumero", TableColumn<ApiComicInfoResumo, String>())
        setField("clSerie", TableColumn<ApiComicInfoResumo, String>())
        setField("clTitulo", TableColumn<ApiComicInfoResumo, String>())
        setField("clIdioma", TableColumn<ApiComicInfoResumo, String>())
        setField("clEditora", TableColumn<ApiComicInfoResumo, String>())

        controller.initialize(null, null)
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getField(name: String): T {
        val field: Field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(controller) as T
    }

    private fun invokeMethod(name: String, vararg args: Any?) {
        val method: Method = controller.javaClass.getDeclaredMethod(name)
        method.isAccessible = true
        method.invoke(controller, *args)
    }

    @Test
    fun testInicializacaoComboLinguagem() {
        assertNotNull(controller.cbLinguagem.items)
        assertTrue(controller.cbLinguagem.items.contains(Linguagem.TODOS))
        assertTrue(controller.cbLinguagem.items.contains(Linguagem.PORTUGUESE))
        assertTrue(controller.cbLinguagem.items.contains(Linguagem.ENGLISH))
        assertTrue(controller.cbLinguagem.items.contains(Linguagem.JAPANESE))
        assertEquals(Linguagem.PORTUGUESE, controller.cbLinguagem.value)
    }

    @Test
    fun testFiltragemPorIdiomaEItensNulos() {
        val listaCompleta: MutableList<ApiComicInfoResumo> = getField("listaCompletaApi")
        val listaResultados: ObservableList<ApiComicInfoResumo> = getField("listaResultados")

        val itemPt = ApiComicInfoResumo(
            id = UUID.randomUUID(),
            series = "One Piece",
            title = "Romance Dawn",
            volume = 1f,
            number = 1f,
            languageISO = "pt",
            publisher = "Shueisha"
        )
        val itemEn = ApiComicInfoResumo(
            id = UUID.randomUUID(),
            series = "One Piece",
            title = "Romance Dawn",
            volume = 1f,
            number = 1f,
            languageISO = "en",
            publisher = "Shueisha"
        )
        val itemJa = ApiComicInfoResumo(
            id = UUID.randomUUID(),
            series = "One Piece",
            title = "Romance Dawn",
            volume = 1f,
            number = 1f,
            languageISO = "ja",
            publisher = "Shueisha"
        )
        val itemSemIdioma = ApiComicInfoResumo(
            id = UUID.randomUUID(),
            series = "One Piece",
            title = "Romance Dawn",
            volume = 1f,
            number = 1f,
            languageISO = null,
            publisher = "Shueisha"
        )

        listaCompleta.addAll(listOf(itemPt, itemEn, itemJa, itemSemIdioma))

        // Filtro em Português (padrão)
        controller.cbLinguagem.value = Linguagem.PORTUGUESE
        invokeMethod("aplicarFiltroIdioma")
        assertEquals(1, listaResultados.size)
        assertEquals("pt", listaResultados[0].languageISO)

        // Filtro em Inglês
        controller.cbLinguagem.value = Linguagem.ENGLISH
        invokeMethod("aplicarFiltroIdioma")
        assertEquals(1, listaResultados.size)
        assertEquals("en", listaResultados[0].languageISO)

        // Filtro em Japonês
        controller.cbLinguagem.value = Linguagem.JAPANESE
        invokeMethod("aplicarFiltroIdioma")
        assertEquals(1, listaResultados.size)
        assertEquals("ja", listaResultados[0].languageISO)

        // Filtro em Todos: deve incluir todos os 4, inclusive o nulo
        controller.cbLinguagem.value = Linguagem.TODOS
        invokeMethod("aplicarFiltroIdioma")
        assertEquals(4, listaResultados.size)
    }

    @Test
    fun testSetPesquisaInicialComLinguagem() {
        controller.setPesquisaInicial("Naruto", Linguagem.JAPANESE)
        assertEquals("Naruto", controller.txtPesquisa.text)
        assertEquals(Linguagem.JAPANESE, controller.cbLinguagem.value)
    }
}
