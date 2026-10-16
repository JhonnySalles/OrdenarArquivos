package com.fenix.ordenararquivos.model.entities.comicinfo

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.enums.TrackerType
import com.jfoenix.controls.JFXButton
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TrackerResultTest : BaseJfxTest() {

    @Test
    fun testSiteBoxVerticalOrientation() {
        val metadata = TrackerMetadata(
            id = 101L,
            title = "Test Novel",
            alternativeTitles = emptyList(),
            genres = emptyList(),
            authors = emptyList(),
            serialization = emptyList(),
            url = "https://anilist.co/manga/101",
            type = "novel"
        )

        val btn = JFXButton("Site")
        val trackerResult = TrackerResult(
            tracker = TrackerType.ANILIST,
            id = 101L,
            nome = "Test Novel",
            descricao = "Desc",
            site = btn,
            imagem = null,
            dados = metadata
        )

        val siteBox = trackerResult.siteBox
        assertNotNull(siteBox)
        assertTrue(siteBox is VBox)

        // Deve conter exatamente 3 elementos diretamente no VBox: botão site, badge de tipo e badge do tracker
        assertEquals(3, siteBox.children.size)
        assertSame(btn, siteBox.children[0])

        val badgeTipo = siteBox.children[1] as Label
        assertEquals("NOVEL", badgeTipo.text)

        val badgeTracker = siteBox.children[2] as Label
        assertEquals("AniList", badgeTracker.text)

        // Nenhum dos filhos pode ser um HBox (para evitar corte horizontal)
        assertFalse(siteBox.children.any { it is HBox }, "Nenhum elemento filho deve ser HBox no siteBox")
    }

    @Test
    fun testSiteBoxWithoutSiteButton() {
        val metadata = TrackerMetadata(
            id = 202L,
            title = "Test Manga",
            alternativeTitles = emptyList(),
            genres = emptyList(),
            authors = emptyList(),
            serialization = emptyList(),
            url = "https://myanimelist.net/manga/202",
            type = "manga"
        )

        val trackerResult = TrackerResult(
            tracker = TrackerType.MYANIMELIST,
            id = 202L,
            nome = "Test Manga",
            descricao = "Desc",
            site = null,
            imagem = null,
            dados = metadata
        )

        val siteBox = trackerResult.siteBox
        assertEquals(2, siteBox.children.size)

        val badgeTipo = siteBox.children[0] as Label
        assertEquals("MANGA", badgeTipo.text)

        val badgeTracker = siteBox.children[1] as Label
        assertEquals("MyAnimeList", badgeTracker.text)
    }
}
