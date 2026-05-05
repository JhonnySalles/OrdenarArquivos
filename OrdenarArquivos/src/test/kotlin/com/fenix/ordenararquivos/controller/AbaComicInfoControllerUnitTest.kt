package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import javafx.collections.FXCollections
import javafx.scene.control.TableView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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
        val item = Processar(arquivo = "test.rar", tags = "0 : Capítulo 001 # 001 - Título")
        val lista = FXCollections.observableArrayList(item)
        setField("mObsListaProcessar", lista)

        val method = controller.javaClass.getDeclaredMethod("onBtnTagsAplicar")
        method.isAccessible = true
        method.invoke(controller)

        // O separador de importação é ' # ' e o separador de capítulo é ' - ' (pelo que vi no código)
        // O método onBtnTagsAplicar deveria limpar a tag para o formato "imagem : capítulo - título"
        assertEquals("0 - Título", item.tags)
    }
}
