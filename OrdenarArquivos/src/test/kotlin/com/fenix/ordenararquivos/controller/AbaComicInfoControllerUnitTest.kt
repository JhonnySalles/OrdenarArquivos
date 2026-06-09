package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.fenix.ordenararquivos.util.Utils
import javafx.collections.FXCollections
import javafx.scene.control.TableView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.File
import java.lang.reflect.Field

class AbaComicInfoControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: AbaComicInfoController
    private val rarService: WinrarServices = mock()
    private val ocrService: OcrServices = mock()
    private val telaInicialController: TelaInicialController = mock()

    @BeforeEach
    fun setUp() {
        controller = AbaComicInfoController()
        controller.controllerPai = telaInicialController

        setField("mRarService", rarService)
        setField("mOcrService", ocrService)
        setField("tbViewProcessar", TableView<Processar>())
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = findField(controller.javaClass, name)
        field.isAccessible = true
        field.set(controller, value)
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
    fun testGerarTagItemJapanese() {
        val pages = listOf(
            Pages(image = 0, bookmark = "Capítulo 001")
        )
        val comic = ComicInfo(pages = pages)
        val item = Processar(arquivo = "test.rar", comicInfo = comic)

        val method = controller.javaClass.getDeclaredMethod("gerarTagItem", Processar::class.java, Linguagem::class.java, Boolean::class.java)
        method.isAccessible = true
        method.invoke(controller, item, Linguagem.JAPANESE, false)

        // O prefixo japonês para "Capítulo" é "第...話"
        assertTrue(item.tags.contains("第"), "Deveria conter caractere japonês 第. Tags: ${item.tags}")
        assertTrue(item.tags.contains("話"), "Deveria conter caractere japonês 話. Tags: ${item.tags}")
    }

    @Test
    fun testOnBtnTagsAplicar() {
        val item = Processar(
            arquivo = "test.rar",
            tags = "0${Utils.SEPARADOR_IMAGEM} Capítulo 001 ${Utils.SEPARADOR_IMPORTACAO} 001${Utils.SEPARADOR_CAPITULO}Título\n" +
                "1${Utils.SEPARADOR_IMAGEM} Capítulo 002 ${Utils.SEPARADOR_IMPORTACAO} 002${Utils.SEPARADOR_CAPITULO}Outro"
        )
        val lista = FXCollections.observableArrayList(item)
        setField("mObsListaProcessar", lista)

        val method = controller.javaClass.getDeclaredMethod("onBtnTagsAplicar")
        method.isAccessible = true
        method.invoke(controller)

        assertEquals(
            "0${Utils.SEPARADOR_IMAGEM} Capítulo 001 - Título\n1${Utils.SEPARADOR_IMAGEM} Capítulo 002 - Outro",
            item.tags
        )
    }

    @Test
    fun testCriarComicInfoBasicoDerivaTituloDoArquivo() {
        val arquivo = File("Serie - Volume 01.cbz")
        val comic = AbaComicInfoController.criarComicInfoBasico(arquivo, Linguagem.PORTUGUESE)

        assertEquals("Serie", comic.comic)
        assertEquals("Serie", comic.title)
        assertEquals("Serie", comic.series)
        assertEquals(1, comic.volume)
        assertEquals(Linguagem.PORTUGUESE.sigla, comic.languageISO)
    }

    @Test
    fun testArquivoPossuiComicInfoDetectaXml() {
        assertTrue(
            AbaComicInfoController.arquivoPossuiComicInfoNaListagem(listOf("ComicInfo.xml", "cap001.jpg"))
        )
        assertTrue(
            AbaComicInfoController.arquivoPossuiComicInfoNaListagem(listOf("folder/ComicInfo.xml"))
        )
        assertFalse(
            AbaComicInfoController.arquivoPossuiComicInfoNaListagem(listOf("cap001.jpg", "CoMet.xml"))
        )
    }

    @Test
    fun testTagsFromComicRefleteBookmarks() {
        val comic = ComicInfo(
            pages = listOf(
                Pages(image = 0, bookmark = "Capítulo 001"),
                Pages(image = 5, bookmark = "Capítulo 002")
            )
        )

        val tags = AbaComicInfoController.tagsFromComic(comic)

        assertTrue(tags.contains("0${Utils.SEPARADOR_IMAGEM}Capítulo 001"))
        assertTrue(tags.contains("5${Utils.SEPARADOR_IMAGEM}Capítulo 002"))
    }

    @Test
    fun testRecarregarItemAtualizaTagsSemRecarregarLista() {
        val comic = ComicInfo(
            pages = listOf(Pages(image = 3, bookmark = "Capítulo 010"))
        )
        val arquivo = File("temp_test_comic.rar")
        val item = Processar(arquivo = arquivo.name, file = arquivo, comicInfo = ComicInfo(), tags = "")

        whenever(rarService.listarConteudo(arquivo)).thenReturn(listOf("ComicInfo.xml"))
        val xmlTemp = File.createTempFile("comicinfo_reload", ".xml")
        xmlTemp.deleteOnExit()
        whenever(rarService.extraiComicInfo(eq(arquivo), any())).thenReturn(xmlTemp)

        val jaxb = jakarta.xml.bind.JAXBContext.newInstance(ComicInfo::class.java)
        jaxb.createMarshaller().marshal(comic, xmlTemp)

        val method = controller.javaClass.getDeclaredMethod("recarregarComicInfoItem", Processar::class.java)
        method.isAccessible = true
        method.invoke(controller, item)

        assertFalse(item.semComicInfo)
        assertTrue(item.tags.contains("3${Utils.SEPARADOR_IMAGEM}Capítulo 010"))
        assertEquals("Capítulo 010", item.comicInfo?.pages?.first()?.bookmark)
    }
}
