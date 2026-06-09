package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Capitulo
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.collections.FXCollections
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Field

class PopupCapitulosControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupCapitulosController

    @BeforeEach
    fun setUp() {
        controller = PopupCapitulosController()

        setField("txtEndereco", JFXTextField())
        setField("cbLinguagem", JFXComboBox<Linguagem>())
        setField("cbMarcarTodos", JFXCheckBox())
        setField("tbViewTabela", TableView<Volume>())

        (getField("cbLinguagem") as JFXComboBox<Linguagem>).items.setAll(*Linguagem.values())
        (getField("cbLinguagem") as JFXComboBox<Linguagem>).selectionModel.select(Linguagem.PORTUGUESE)
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

    private fun invokePreparar(volumes: List<Volume>) {
        controller.preparar(volumes)
    }

    private fun setupProcessar(tags: String, volume: Int = 1, arquivo: String = "vol1.cbz") {
        val processar = listOf(
            Processar(
                arquivo = arquivo,
                tags = tags,
                comicInfo = ComicInfo(volume = volume)
            )
        )
        setField("mProcessar", processar)
        setField("isImportado", true)
    }

    @Test
    fun testExtractManualText() {
        val text = """
            Volume 1
            Chapter 1: The Beginning
            Chapter 2: The End
            
            Volume 2
            Chapter 3: More
        """.trimIndent()

        val method = controller.javaClass.getDeclaredMethod("extractManualText", String::class.java)
        method.isAccessible = true
        method.invoke(controller, text)

        val lista = getField("mLista") as List<Volume>
        val titulos = lista.flatMap { it.capitulos }.map { it.ingles }
        assertTrue(titulos.contains("The Beginning"))
        assertTrue(titulos.contains("The End"))
        assertTrue(titulos.contains("More"))
    }

    @Test
    fun testPrepararSemProcessar() {
        setField("isImportado", true)
        val volumes = listOf(
            Volume(volume = 1.0).apply {
                capitulos.add(Capitulo(capitulo = 1.0, ingles = "Title 1", japones = ""))
            }
        )

        invokePreparar(volumes)

        val lista = getField("mLista") as List<Volume>
        assertEquals(1, lista.size)
        assertTrue(lista[0].tags.contains("Title 1"))
        assertTrue(lista[0].tags.startsWith("-1${Utils.SEPARADOR_IMAGEM}"))
    }

    @Test
    fun testParseTagCapituloComImportacao() {
        val tag = "12;Capítulo 01${Utils.SEPARADOR_IMPORTACAO}01.00|Título Importado"
        val ref = controller.parseTagCapitulo(tag)

        assertNotNull(ref)
        assertEquals("12", ref!!.pagina)
        assertEquals(1.0, ref.capitulo)
        assertEquals("Capítulo 01", ref.bookmark)
        assertEquals("Título Importado", ref.tituloImportado)
    }

    @Test
    fun testPrepararPreservaPaginaOriginalNoMatch() {
        val tagsOriginais = "12;Capítulo 01${Utils.SEPARADOR_IMPORTACAO}01.00|Antigo"
        setupProcessar(tagsOriginais)

        val importPool = listOf(
            Volume(volume = 1.0, capitulos = mutableListOf(
                Capitulo(capitulo = 1.0, ingles = "Novo Título", japones = "")
            ))
        )

        invokePreparar(importPool)

        val lista = getField("mLista") as List<Volume>
        assertEquals(1, lista.size)
        assertEquals(tagsOriginais, lista[0].descricoes)
        assertTrue(lista[0].tags.startsWith("12${Utils.SEPARADOR_IMAGEM}"))
        assertFalse(lista[0].tags.startsWith("-1${Utils.SEPARADOR_IMAGEM}"))
        assertTrue(lista[0].tags.contains("Novo Título"))
    }

    @Test
    fun testPrepararCapituloSemTagCorrespondenteUsaMenosUm() {
        setupProcessar("5;Capítulo 01${Utils.SEPARADOR_IMPORTACAO}01.00|Existente")

        val importPool = listOf(
            Volume(volume = 1.0, capitulos = mutableListOf(
                Capitulo(capitulo = 1.0, ingles = "Cap 1", japones = ""),
                Capitulo(capitulo = 99.0, ingles = "Cap Extra", japones = "")
            ))
        )

        invokePreparar(importPool)

        val lista = getField("mLista") as List<Volume>
        assertEquals(2, lista.size)
        assertTrue(lista[0].tags.startsWith("5${Utils.SEPARADOR_IMAGEM}"))

        val naoLocalizados = lista.find { it.arquivo == "Não Localizados" }
        assertNotNull(naoLocalizados)
        assertTrue(naoLocalizados!!.tags.startsWith("-1${Utils.SEPARADOR_IMAGEM}"))
        assertTrue(naoLocalizados.tags.contains("Cap Extra"))
    }

    @Test
    fun testDeduplicarImportacaoPriorizaTitulo() {
        setField("isImportado", true)
        setField("mImportedChapters", FXCollections.observableArrayList(
            Volume(volume = 1.0, capitulos = mutableListOf(
                Capitulo(capitulo = 1.0, ingles = "", japones = "")
            ))
        ))

        val novos = listOf(
            Volume(volume = 1.0, capitulos = mutableListOf(
                Capitulo(capitulo = 1.0, ingles = "Título Completo", japones = "")
            ))
        )

        val method = controller.javaClass.getDeclaredMethod("mergeImportedChapters", List::class.java)
        method.isAccessible = true
        method.invoke(controller, novos)

        val imported = getField("mImportedChapters") as List<Volume>
        assertEquals("Título Completo", imported[0].capitulos[0].ingles)
    }

    @Test
    fun testPrepararDeduplicaPoolComTitulo() {
        setupProcessar("3;Capítulo 01${Utils.SEPARADOR_IMPORTACAO}01.00|Tag")

        val importPool = listOf(
            Volume(volume = 1.0, capitulos = mutableListOf(
                Capitulo(capitulo = 1.0, ingles = "", japones = ""),
                Capitulo(capitulo = 1.0, ingles = "Melhor Título", japones = "")
            ))
        )

        invokePreparar(importPool)

        val lista = getField("mLista") as List<Volume>
        assertEquals(1, lista.size)
        assertEquals(1, lista[0].capitulos.size)
        assertTrue(lista[0].tags.contains("Melhor Título"))
    }

    @Test
    fun testDescricoesIgualTagsOriginais() {
        val tags = "7;Capítulo 02${Utils.SEPARADOR_IMPORTACAO}02.00|Original\n8;Capítulo 03${Utils.SEPARADOR_IMPORTACAO}03.00|Outro"
        setupProcessar(tags)

        invokePreparar(listOf(
            Volume(volume = 1.0, capitulos = mutableListOf(
                Capitulo(capitulo = 2.0, ingles = "Imp 2", japones = ""),
                Capitulo(capitulo = 3.0, ingles = "Imp 3", japones = "")
            ))
        ))

        val lista = getField("mLista") as List<Volume>
        assertEquals(tags, lista[0].descricoes)
    }

    @Test
    fun testProcessarHtmlMangaPlanet() {
        val htmlFile = File("src/test/resources/fixtures/mangaplanet.html")
        val html = htmlFile.readText(Charsets.UTF_8)

        controller.processarHtml("https://mangaplanet.com", html)

        val imported = getField("mImportedChapters") as List<Volume>
        assertEquals(1, imported.size)
        assertEquals(1.0, imported[0].volume)
        assertEquals(1, imported[0].capitulos.size)
        assertEquals("The Beginning", imported[0].capitulos[0].ingles)

        val lista = getField("mLista") as List<Volume>
        assertTrue(lista.isNotEmpty())
    }

    @Test
    fun testProcessarUrlBlankShowsAlert() {
        AlertasModal.isTeste = true
        AlertasModal.lastAlertText = null

        controller.processarUrl("")

        assertEquals("Informe uma URL ou arquivo HTML antes de executar.", AlertasModal.lastAlertText)
    }

    @Test
    fun testNormalizarEnderecoAdicionaHttps() {
        assertEquals("https://www.mangadex.org/", controller.normalizarEndereco("www.mangadex.org/"))
        assertEquals("https://mangadex.org", controller.normalizarEndereco("mangadex.org"))
    }

    @Test
    fun testNormalizarEnderecoPreservaArquivoLocal() {
        val file = File("src/test/resources/fixtures/mangaplanet.html")
        assertEquals(file.absolutePath, controller.normalizarEndereco(file.absolutePath))
    }

    @Test
    fun testRestaurarEstadoDialogHabilitaNode() {
        val pane = AnchorPane()
        pane.isDisable = true

        PopupCapitulosWebController.restaurarEstadoDialog(pane)

        assertFalse(pane.isDisable)
        assertNull(pane.effect)
    }

    @Test
    fun testIsComickComicUrl() {
        assertTrue(controller.isComickComicUrl("https://comick.dev/comic/foo-bar"))
        assertTrue(controller.isComickComicUrl("https://comick.io/comic/example"))
        assertFalse(controller.isComickComicUrl("https://comick.dev/search"))
    }

    @Test
    fun testExtrairSlugComick() {
        assertEquals(
            "apocalypse-bringer",
            controller.extrairSlugComick(
                "https://comick.dev/comic/apocalypse-bringer?lang=pt-br&page=1#chapter-header"
            )
        )
    }

    @Test
    fun testProcessarHtmlComickDev() {
        val htmlFile = File("src/test/resources/fixtures/comick-dev.html")
        controller.processarHtml("https://comick.dev/comic/test-comic", htmlFile.readText(Charsets.UTF_8))

        val imported = getField("mImportedChapters") as List<Volume>
        assertTrue(imported.any { it.capitulos.size >= 2 })
    }

    @Test
    fun testProcessarUrlComickArquivoLocalUsaApi() {
        setField("mImportedChapters", mutableListOf<Volume>())
        controller.comickFetcher = { url ->
            when {
                url.endsWith("/comic/test-comic") ->
                    """{"comic":{"hid":"hid123"}}"""
                url.contains("/comic/hid123/chapters") ->
                    """{"total":1,"chapters":[{"chap":"1","vol":null,"title":"API Chapter"}]}"""
                else -> null
            }
        }

        val htmlFile = File("src/test/resources/fixtures/comick-local.html")
        controller.processarUrl(htmlFile.absolutePath)

        val imported = getField("mImportedChapters") as List<Volume>
        assertEquals(1, imported.sumOf { it.capitulos.size })
        assertEquals("API Chapter", imported.flatMap { it.capitulos }.first().ingles)
        controller.comickFetcher = null
    }
}
