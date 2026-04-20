package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.scene.control.TableView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.lang.reflect.Field

class PopupComicInfoControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupComicInfoController
    private val comicInfoService: ComicInfoServices = mock()

    @BeforeEach
    fun setUp() {
        controller = PopupComicInfoController()

        // Injetar mocks
        setField("mServiceComicInfo", comicInfoService)

        // Injetar campos FXML
        setField("txtIdMal", JFXTextField())
        setField("cbAgeRating", JFXComboBox<AgeRating>())
        setField("cbLinguagem", JFXComboBox<Linguagem>())
        setField("txtTitle", JFXTextField())
        setField("txtSeries", JFXTextField())
        setField("txtComic", JFXTextField())
        setField("txtPublisher", JFXTextField())
        setField("txtAlternateSeries", JFXTextField())
        setField("txtSeriesGroup", JFXTextField())
        setField("txtStoryArc", JFXTextField())
        setField("txtGenre", JFXTextField())
        setField("txtImprint", JFXTextField())
        setField("txtNotes", JFXTextArea())

        setField("txtMalId", JFXTextField())
        setField("txtMalNome", JFXTextField())
        setField("btnMalConsultar", JFXButton())
        setField("btnConfirmar", JFXButton())
        setField("tbViewMal", TableView<Mal>())
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    private fun getField(name: String): Any? {
        val field: Field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(controller)
    }

    @Test
    fun testSetComicInfoPopulatesFields() {
        val ci =
                ComicInfo(
                        title = "One Piece",
                        series = "OP",
                        ageRating = AgeRating.Everyone,
                        languageISO = "en"
                )

        controller.setComicInfo(ci)

        val txtTitle = getField("txtTitle") as JFXTextField
        val cbAgeRating = getField("cbAgeRating") as JFXComboBox<AgeRating>

        assertEquals("One Piece", txtTitle.text)
        assertEquals(AgeRating.Everyone, cbAgeRating.value)
    }

    @Test
    fun testAtualizaObjetoFromFields() {
        val ci = ComicInfo()
        controller.setComicInfo(ci)

        // Simular edição nos campos
        (getField("txtTitle") as JFXTextField).text = "Naruto"
        (getField("cbAgeRating") as JFXComboBox<AgeRating>).value = AgeRating.Mature

        val method = controller.javaClass.getDeclaredMethod("atualizaObjeto")
        method.isAccessible = true
        method.invoke(controller)

        assertEquals("Naruto", ci.title)
        assertEquals(AgeRating.Mature, ci.ageRating)
    }

    @Test
    fun testOnBtnMalConsultar() {
        (getField("txtMalNome") as JFXTextField).text = "Bleach"

        whenever(comicInfoService.getMal(anyOrNull(), any())).thenReturn(listOf(mock()))

        val method = controller.javaClass.getDeclaredMethod("onBtnMalConsultar")
        method.isAccessible = true
        method.invoke(controller)

        // Como o método é assíncrono (Task), aguardamos a execução
        Thread.sleep(200)

        verify(comicInfoService).getMal(null, "Bleach")
    }
}
