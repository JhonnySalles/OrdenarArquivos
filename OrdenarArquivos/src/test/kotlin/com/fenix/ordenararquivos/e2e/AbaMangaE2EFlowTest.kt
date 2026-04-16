package com.fenix.ordenararquivos.e2e

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaMangaController
import com.fenix.ordenararquivos.controller.PopupComicInfoController
import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTabPane
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.framework.junit5.Start
import org.testfx.util.WaitForAsyncUtils
import java.util.concurrent.TimeUnit

@Tag("E2E")
@Tag("UI")
@ExtendWith(ApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AbaMangaE2EFlowTest : BaseTest() {

    private lateinit var mainController: TelaInicialController
    private val mangaService = MangaServices()

    @Start
    fun start(stage: Stage) {
        val loader = FXMLLoader(TelaInicialController.fxmlLocate)
        val root: AnchorPane = loader.load()
        mainController = loader.getController()

        stage.scene = Scene(root, 1200.0, 800.0)
        stage.show()
    }

    @BeforeEach
    fun setUp(robot: FxRobot) {
        // Garantir banco limpo e populado com dados de teste
        // BaseTest configura o banco em memória (:memory:)
        mangaService.save(Manga(nome = "Naruto", volume = "01", capitulo = "01", arquivo = "Naruto 01.cbz"))
        mangaService.save(Manga(nome = "One Piece", volume = "01", capitulo = "01", arquivo = "One Piece 01.cbz"))

        // Navega para a aba Manga
        val tabPane = robot.lookup("#tpGlobal").queryAs(JFXTabPane::class.java)
        robot.interact {
            val tab = tabPane.tabs.find { it.text == "Manga" }
            tabPane.selectionModel.select(tab)
        }
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    @Order(1)
    fun testE2EMangaToPopupFlow(robot: FxRobot) {
        // 1. Verificar se a tabela carregou os dados do banco
        val tbViewManga = robot.lookup("#tbViewManga").queryAs(TableView::class.java) as TableView<Manga>
        
        // Timeout maior para o carregamento assíncrono inicial
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { tbViewManga.items.size >= 2 }
        
        // 2. Filtrar para isolar o mangá alvo
        robot.clickOn("#txtFiltro").write("Naruto")
        WaitForAsyncUtils.waitForFxEvents()
        
        // Aguarda o filtro assíncrono
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS) { tbViewManga.items.size == 1 }
        assertEquals("Naruto", tbViewManga.items[0].nome)

        // 3. Abrir o Popup via Double Click
        robot.doubleClickOn("Naruto")
        WaitForAsyncUtils.waitForFxEvents()
        
        // 4. Interagir com o PopupComicInfo
        // Procuramos o campo no popup que acabou de abrir
        val txtTitle = robot.lookup("#txtTitle").queryAs(JFXTextField::class.java)
        
        // Limpar e escrever novo título
        robot.clickOn(txtTitle)
        robot.interact { txtTitle.text = "" }
        robot.write("Naruto Shippuden")
        
        // 5. Confirmar alterações no popup
        robot.clickOn("#btnConfirmar")
        WaitForAsyncUtils.waitForFxEvents()
        
        // 6. Verificar se o popup fechou e o grid atualizou
        // O controller chama carregarDados() após fechar o popup.
        // Como 'Naruto' ainda está no filtro, ele deve aparecer com o novo comic (se mudamos comic)
        // No AbaMangaController, o ComicInfo é atualizado mas o Manga.comic no grid só muda se salvarmos no grid ou recarregarmos.
        // O PopupComicInfo salva o ComicInfo no banco. O grid do AbaManga mostra manga.comic.
        
        // Vamos verificar se no banco o mangá continua lá e se podemos ver a alteração se recarregarmos sem filtro
        robot.interact { robot.lookup("#txtFiltro").queryAs(JFXTextField::class.java).text = "" }
        robot.type(javafx.scene.input.KeyCode.ENTER) // Dispara onKeyFiltro se necessário
        
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS) { tbViewManga.items.size >= 2 }
        
        // 7. Verificar se o ComicInfo foi realmente salvo (via integração)
        // O PopupComicInfoController salva via ComicInfoServices
        val ci = com.fenix.ordenararquivos.service.ComicInfoServices().find("Naruto", "pt")
        assertTrue(ci != null && ci.title == "Naruto Shippuden", "O ComicInfo deveria estar salvo no banco com o novo título")
    }
}
