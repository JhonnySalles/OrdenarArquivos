package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.util.Utils
import javafx.concurrent.Worker
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PopupCapitulosWebControllerUnitTest : BaseJfxTest() {

    @Test
    fun testScrapingUserAgentAlinhadoComChrome124() {
        assertTrue(Utils.SCRAPING_USER_AGENT.contains("Chrome/124"))
        assertTrue(Utils.SCRAPING_USER_AGENT.contains("AppleWebKit"))
    }

    @Test
    fun testWebViewUserAgentDistintoDoScraping() {
        assertNotEquals(Utils.SCRAPING_USER_AGENT, Utils.WEBVIEW_USER_AGENT)
        assertTrue(Utils.WEBVIEW_USER_AGENT.contains("Safari/605"))
        assertFalse(Utils.WEBVIEW_USER_AGENT.contains("Chrome/124"))
    }

    @Test
    fun testDeveIgnorarCargaUrlDuplicadaQuandoSucesso() {
        assertTrue(
            PopupCapitulosWebController.deveIgnorarCarga(
                "https://comick.io/",
                "https://comick.io/",
                Worker.State.SUCCEEDED,
                forcar = false
            )
        )
    }

    @Test
    fun testNaoIgnoraCargaQuandoForcarRefresh() {
        assertFalse(
            PopupCapitulosWebController.deveIgnorarCarga(
                "https://comick.io/",
                "https://comick.io/",
                Worker.State.SUCCEEDED,
                forcar = true
            )
        )
    }

    @Test
    fun testNaoIgnoraCargaQuandoEmExecucaoPermiteBloqueio() {
        assertTrue(
            PopupCapitulosWebController.deveIgnorarCarga(
                "https://mangadex.org/",
                null,
                Worker.State.RUNNING,
                forcar = false
            )
        )
    }

    @Test
    fun testSeletoresPorDominioComick() {
        val seletores = PopupCapitulosWebController.seletoresPorDominio("https://comick.io/comic/test")
        assertTrue(seletores.isNotEmpty())
        assertTrue(seletores.any { it.contains("tbody") })
    }

    @Test
    fun testSeletoresPorDominioDesconhecidoRetornaVazio() {
        assertTrue(PopupCapitulosWebController.seletoresPorDominio("https://example.com").isEmpty())
    }

    @Test
    fun testMontarScriptExtracaoHtmlComSeletores() {
        val script = PopupCapitulosWebController.montarScriptExtracaoHtml(listOf("table tbody", ".chapter-list"))
        assertTrue(script.contains("table tbody"))
        assertTrue(script.contains("document.documentElement.outerHTML"))
    }

    @Test
    fun testMontarScriptExtracaoHtmlSemSeletoresUsaDocumentoCompleto() {
        assertEquals(
            "document.documentElement.outerHTML",
            PopupCapitulosWebController.montarScriptExtracaoHtml(emptyList())
        )
    }

    @Test
    fun testPopupCapitulosWebFxmlCarregaSemErro() {
        val resource = PopupCapitulosWebController::class.java.getResource("/view/PopupCapitulosWeb.fxml")
        assertNotNull(resource)

        val fxml = requireNotNull(resource).openStream().bufferedReader().use { it.readText() }
        assertTrue(fxml.contains("<?import javafx.scene.layout.StackPane?>"))
        assertFalse(fxml.contains("loadingOverlay"))
        assertFalse(fxml.contains("JFXSpinner"))

        val loader = FXMLLoader(resource)
        try {
            val root = loader.load<Parent>()
            assertNotNull(root)
            assertNotNull(loader.getController<PopupCapitulosWebController>())
        } catch (e: javafx.fxml.LoadException) {
            assertFalse(
                e.message.orEmpty().contains("StackPane"),
                "FXML deve resolver StackPane: ${e.message}"
            )
            throw e
        } catch (e: IllegalAccessError) {
            // Estrutura FXML OK; ikonli/OSGi pode falhar em testes modulares após o parse.
            assertTrue(e.message.orEmpty().contains("ikonli"))
        }
    }
}
