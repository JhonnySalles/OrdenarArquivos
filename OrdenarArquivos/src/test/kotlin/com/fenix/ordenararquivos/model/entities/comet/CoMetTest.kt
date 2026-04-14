package com.fenix.ordenararquivos.model.entities.comet

import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Manga
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CoMetTest {

    @Test
    fun testToCoMetBasicConversion() {
        val comic = ComicInfo().apply {
            title = "One Piece"
            series = "One Piece Series"
            number = 100f
            publisher = "Shueisha"
            writer = "Eiichiro Oda"
            summary = "Epic adventure"
            manga = Manga.YesAndRightToLeft
            ageRating = AgeRating.Teen
        }

        val comet = CoMet(comic)
        
        assertEquals("One Piece", comet.title)
        assertEquals("One Piece Series", comet.series)
        assertEquals(100, comet.issue)
        assertEquals("Shueisha", comet.publisher)
        assertEquals("Eiichiro Oda", comet.writer?.first())
        assertEquals("Epic adventure", comet.description)
        assertEquals("rtl", comet.readingDirection)
        assertEquals("Teen", comet.rating)
    }

    @Test
    fun testToCoMetWithPaths() {
        val comic = ComicInfo().apply {
            title = "Test Manga"
            pages = mutableListOf(com.fenix.ordenararquivos.model.entities.comicinfo.Pages().apply { image = 0 })
        }
        val paths = listOf("page0.jpg", "page1.jpg", "cover.jpg")
        
        // Simular que a capa está no índice 2
        val comet = CoMet(comic, paths)
        
        // Como o constructor do CoMet pega a primeira página como capa por padrão se não houver definição de tipo
        assertEquals("page0.jpg", comet.coverImage)
    }

    @Test
    fun testGenreConversionWithSemicolon() {
        val comic = ComicInfo().apply {
            genre = "Action; Adventure; Comedy"
        }
        val comet = CoMet(comic)
        
        assertNotNull(comet.genre)
        assertEquals(3, comet.genre?.size)
        assertTrue(comet.genre!!.contains("Action"))
        assertTrue(comet.genre!!.contains("Adventure"))
        assertTrue(comet.genre!!.contains("Comedy"))
    }

    @Test
    fun testMangaDirectionMapping() {
        val comicLtr = ComicInfo().apply { manga = Manga.No }
        val cometLtr = CoMet(comicLtr)
        assertEquals("ltr", cometLtr.readingDirection)

        val comicRtl = ComicInfo().apply { manga = Manga.YesAndRightToLeft }
        val cometRtl = CoMet(comicRtl)
        assertEquals("rtl", cometRtl.readingDirection)
    }
}
