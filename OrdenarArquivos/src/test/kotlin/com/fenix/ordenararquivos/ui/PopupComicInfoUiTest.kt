package com.fenix.ordenararquivos.ui

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.PopupComicInfoController
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.util.*
import java.util.concurrent.TimeUnit

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PopupComicInfoUiTest : BaseTest() {

    private lateinit var controller: PopupComicInfoController
    private val mockComicInfoService = mock<ComicInfoServices>()
    
    private fun createSampleComicInfo() = ComicInfo().apply {
        id = UUID.randomUUID()
        title = "Sample Title"
        series = "Sample Series"
        publisher = "Sample Publisher"
        genre = "Action; Adventure"
        languageISO = "en"
        ageRating = AgeRating.Teen
    }

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(PopupComicInfoController::class.java.getResource("/view/PopupComicInfo.fxml"))
        loader.setControllerFactory { controllerClass ->
            if (controllerClass == PopupComicInfoController::class.java) {
                PopupComicInfoController().apply {
                    // Injeção de dependências via reflection
                    try {
                        val field = PopupComicInfoController::class.java.getDeclaredField("mServiceComicInfo")
                        field.isAccessible = true
                        field.set(this, mockComicInfoService)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    controller = this
                }
            } else {
                controllerClass.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<AnchorPane>()
        stage.scene = Scene(root)
        applyJFoenixFix(stage.scene)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        Mockito.reset(mockComicInfoService)
        robot.interact {
            controller.setComicInfo(createSampleComicInfo())
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(1)
    fun testInitialLoad(robot: FxRobot) {
        val txtTitle = robot.lookup("#txtTitle").queryAs(JFXTextField::class.java)
        val txtSeries = robot.lookup("#txtSeries").queryAs(JFXTextField::class.java)
        val cbAgeRating = robot.lookup("#cbAgeRating").queryAs(JFXComboBox::class.java)
        
        assertEquals("Sample Title", txtTitle.text)
        assertEquals("Sample Series", txtSeries.text)
        assertEquals(AgeRating.Teen, cbAgeRating.value)
    }

    @Test
    @Order(2)
    fun testMalSearch(robot: FxRobot) {
        val txtMalNome = robot.lookup("#txtMalNome").queryAs(JFXTextField::class.java)
        val btnMalConsultar = robot.lookup("#btnMalConsultar").queryAs(JFXButton::class.java)
        val tbViewMal = robot.lookup("#tbViewMal").queryAs(TableView::class.java)

        val malResult = Mal(123L, "MAL Title", "Alt Title", null, null, mock())
        whenever(mockComicInfoService.getMal(anyOrNull(), anyString())).thenReturn(listOf(malResult))

        robot.interact {
            txtMalNome.text = "Naruto"
        }
        robot.clickOn(btnMalConsultar as Node)

        // Aguarda a Task completar (popula a lista)
        WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS) {
            tbViewMal.items.isNotEmpty()
        }
        
        assertEquals(1, tbViewMal.items.size)
        val item = tbViewMal.items[0] as Mal
        assertEquals(123L, item.id)
    }

    @Test
    @Order(3)
    fun testApplyMal(robot: FxRobot) {
        testMalSearch(robot) // Primeiro pesquisa para popular a tabela

        val tbViewMal = robot.lookup("#tbViewMal").queryAs(TableView::class.java)
        val btnMalAplicar = robot.lookup("#btnMalAplicar").queryAs(JFXButton::class.java)
        val txtTitle = robot.lookup("#txtTitle").queryAs(JFXTextField::class.java)

        // Mock updateMal para mudar o título no objeto
        doAnswer {
            val ci = it.getArgument<ComicInfo>(0)
            ci.title = "Updated Title from MAL"
            null
        }.whenever(mockComicInfoService).updateMal(any(), any(), any())

        robot.interact {
            tbViewMal.selectionModel.select(0)
        }
        robot.clickOn(btnMalAplicar as Node)
        WaitForAsyncUtils.waitForFxEvents()

        assertEquals("Updated Title from MAL", txtTitle.text)
    }

    @Test
    @Order(4)
    fun testSave(robot: FxRobot) {
        val txtTitle = robot.lookup("#txtTitle").queryAs(JFXTextField::class.java)
        val btnConfirmar = robot.lookup("#btnConfirmar").queryAs(JFXButton::class.java)

        robot.interact {
            txtTitle.text = "Manually Changed Title"
        }
        
        robot.clickOn(btnConfirmar as Node)
        WaitForAsyncUtils.waitForFxEvents()

        // Verifica se o serviço de save foi chamado com o título alterado
        // save(comic, isSendCloud, isReceiveCloud, sincronizacao)
        verify(mockComicInfoService).save(argThat {
            this.title == "Manually Changed Title"
        }, any(), any(), any())
    }

    @Test
    @Order(5)
    fun testCancel(robot: FxRobot) {
        val btnCancelar = robot.lookup("#btnCancelar").queryAs(JFXButton::class.java)
        
        var closed = false
        robot.interact {
            controller.onClose = { closed = true }
        }
        
        robot.clickOn(btnCancelar as Node)
        WaitForAsyncUtils.waitForFxEvents()

        assertTrue(closed)
        verify(mockComicInfoService, never()).save(any(), any(), any(), any())
    }
}
