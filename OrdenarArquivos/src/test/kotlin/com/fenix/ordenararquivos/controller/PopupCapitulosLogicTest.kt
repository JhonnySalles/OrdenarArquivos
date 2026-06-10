package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PopupCapitulosLogicTest : BaseJfxTest() {

    private val controller = PopupCapitulosController()

    @Test
    fun testExtractMangaPlanet() {
        val htmlFile = File("src/test/resources/fixtures/mangaplanet.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        
        val volumes = controller.extractMangaPlanet(doc)
        
        assertEquals(1, volumes.size)
        assertEquals(1.0, volumes[0].volume)
        assertEquals(1, volumes[0].capitulos.size)
        assertEquals(1.0, volumes[0].capitulos[0].capitulo)
        assertEquals("The Beginning", volumes[0].capitulos[0].ingles)
        assertEquals("はじまり", volumes[0].capitulos[0].japones)
    }

    @Test
    fun testExtractComick() {
        val htmlFile = File("src/test/resources/fixtures/comick.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")
        
        val volumes = controller.extractComick(doc)
        
        assertEquals(1, volumes.size)
        assertEquals(1.0, volumes[0].volume)
        assertEquals(1, volumes[0].capitulos.size)
        assertEquals(1.0, volumes[0].capitulos[0].capitulo)
        assertEquals("First Chapter", volumes[0].capitulos[0].ingles)
    }

    @Test
    fun testExtractComickDevLayout() {
        val htmlFile = File("src/test/resources/fixtures/comick-dev.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractComick(doc)

        assertEquals(1, volumes.size)
        assertEquals(2, volumes[0].capitulos.size)
        val titulos = volumes[0].capitulos.map { it.ingles }.toSet()
        assertTrue(titulos.contains("First Chapter"))
        assertTrue(titulos.contains("Second Chapter"))
    }

    @Test
    fun testExtractComickFromEmbeddedJson() {
        val html = """{"chap":"5","vol":"2","title":"Offline Chapter"}"""
        val entries = controller.extractComickFromEmbeddedJson(html)

        assertEquals(1, entries.size)
        assertEquals(5.0, entries[0].chap)
        assertEquals(2.0, entries[0].vol)
        assertEquals("Offline Chapter", entries[0].title)
    }

    @Test
    fun testExtractMangaTownDesktop() {
        val htmlFile = File("src/test/resources/fixtures/mangatown-desktop.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractMangaTown(doc)

        assertEquals(1, volumes.size)
        assertTrue(volumes[0].capitulos.any { it.capitulo == 71.0 })
    }

    @Test
    fun testExtractMangaTownMobile() {
        val htmlFile = File("src/test/resources/fixtures/mangatown-mobile.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractMangaTown(doc)

        assertEquals(1, volumes.size)
        val cap202 = volumes[0].capitulos.find { it.capitulo == 202.0 }
        assertTrue(cap202 != null)
        assertEquals("Umaru and The Fans", cap202?.ingles)
    }

    @Test
    fun testExtractMangaHere() {
        val htmlFile = File("src/test/resources/fixtures/mangahere.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractMangaHere(doc)

        assertTrue(volumes.isEmpty() || volumes.all { it.capitulos.isEmpty() })
    }

    @Test
    fun testExtractVyManga() {
        val htmlFile = File("src/test/resources/fixtures/vymanga.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractVyManga(doc)

        assertEquals(1, volumes.size)
        assertTrue(volumes[0].capitulos.any { it.capitulo == 72.0 })
    }

    @Test
    fun testExtractVyMangaWithVolume() {
        val parsed = controller.parseChapterFromText("Vol.3 Chapter 46 : The Hero'S Memoirs")

        assertEquals(3.0, parsed?.volume)
        assertEquals(46.0, parsed?.chapter)
        assertEquals("The Hero'S Memoirs", parsed?.title)
    }

    @Test
    fun testExtractKManga() {
        val htmlFile = File("src/test/resources/fixtures/kmanga-episode.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractKManga(doc)

        assertEquals(1, volumes.size)
        val capitulo142 = volumes[0].capitulos.find { it.capitulo == 142.0 }
        assertTrue(capitulo142 != null)
        assertEquals("The Hero Granville Rozzo", capitulo142?.ingles)
    }

    @Test
    fun testParseChapterFromTextPortuguese() {
        val parsed = controller.parseChapterFromText("Capítulo 12: Título em português")

        assertEquals(12.0, parsed?.chapter)
        assertEquals("Título em português", parsed?.title)
    }

    @Test
    fun testExtractMangaDexPrefersPortuguese() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/mangadex-multilang.html"), "UTF-8")
        val volumes = controller.extractMangaDex(doc)
        val caps = volumes.flatMap { it.capitulos }
        val cap153 = caps.find { it.capitulo == 153.0 }
        assertTrue(cap153 != null)
        assertEquals("Cura", cap153?.ingles)
    }

    @Test
    fun testExtractMangaDexEnglishFallback() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/mangadex-multilang.html"), "UTF-8")
        val volumes = controller.extractMangaDex(doc)
        val cap154 = volumes.flatMap { it.capitulos }.find { it.capitulo == 154.0 }
        assertTrue(cap154 != null)
        assertEquals("Início EN", cap154?.ingles)
    }

    @Test
    fun testExtractMangaDexSkipsEmptyAndUnsupported() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/mangadex-multilang.html"), "UTF-8")
        val caps = controller.extractMangaDex(doc).flatMap { it.capitulos }
        assertTrue(caps.none { it.capitulo == 155.0 })
        assertTrue(caps.none { it.capitulo == 156.0 })
        assertEquals(2, caps.size)
    }

    @Test
    fun testExtractMangaDexIgnoresLangCounts() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/mangadex-lang-counts.html"), "UTF-8")
        val volumes = controller.extractMangaDex(doc)
        val caps = volumes.flatMap { it.capitulos }

        val cap180 = caps.find { it.capitulo == 180.0 }
        assertTrue(cap180 != null)
        assertEquals("Poder Trovejante", cap180?.ingles)
        assertTrue(caps.none { it.capitulo == 18011.0 })

        val cap177 = caps.find { it.capitulo == 177.0 }
        assertTrue(cap177 != null)
        assertEquals("O Fardo que se Carrega", cap177?.ingles)
        assertTrue(caps.none { it.capitulo == 17721.0 })
    }

    @Test
    fun testExtractComickFan() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/comickfan.html"), "UTF-8")
        val volumes = controller.extractComickFan(doc)
        assertEquals(1, volumes.size)
        assertEquals(3, volumes[0].capitulos.size)
        val cap163 = volumes[0].capitulos.find { it.capitulo == 163.0 }
        assertTrue(cap163 != null)
        assertEquals("Run Like Hell", cap163?.ingles)
    }

    @Test
    fun testExtractComickFanIgnoresScanMetadata() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/comickfan.html"), "UTF-8")
        val volumes = controller.extractComickFan(doc)
        val cap1 = volumes[0].capitulos.find { it.capitulo == 1.0 }
        assertTrue(cap1 != null)
        assertEquals("Birth of a Slave", cap1!!.ingles)
        assertFalse(cap1.ingles.contains("Ards"))
        assertFalse(cap1.ingles.contains("upvotes"))
        assertFalse(cap1.ingles.contains("months ago"))
    }

    @Test
    fun testCleanImportedTitlePreservesSuffix() {
        assertEquals("Cura", controller.cleanImportedTitle("Chapter 153 Cura"))
    }

    @Test
    fun testExtractMangaKatana() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/mangakatana-slave-snippet.html"), "UTF-8")
        val volumes = controller.extractMangaKatana(doc)

        assertEquals(1, volumes.size)
        assertEquals(2, volumes[0].capitulos.size)
        val cap180 = volumes[0].capitulos.find { it.capitulo == 180.0 }
        val cap177 = volumes[0].capitulos.find { it.capitulo == 177.0 }
        assertTrue(cap180 != null)
        assertEquals("", cap180?.ingles)
        assertTrue(cap177 != null)
        assertEquals("The Weight She Carries", cap177?.ingles)
    }

    @Test
    fun testExtractMangaFireChainedSoldierSnippet() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/mangafire-chained-soldier-snippet.html"), "UTF-8")
        val volumes = controller.extractMangaFire(doc)

        assertEquals(1, volumes.size)
        assertEquals(18.0, volumes[0].volume)
        assertEquals(3, volumes[0].capitulos.size)
        val cap152 = volumes[0].capitulos.find { it.capitulo == 152.0 }
        val cap153 = volumes[0].capitulos.find { it.capitulo == 153.0 }
        assertTrue(cap152 != null)
        assertEquals("A Grande Batalha", cap152?.ingles)
        assertTrue(cap153 != null)
        assertEquals("Cura", cap153?.ingles)
    }

    @Test
    fun testExtractZazaManga() {
        val doc = Jsoup.parse(File("src/test/resources/fixtures/zazamanga.html"), "UTF-8")
        val volumes = controller.extractZazaManga(doc)

        assertEquals(1, volumes.size)
        assertEquals(2, volumes[0].capitulos.size)
        val cap405 = volumes[0].capitulos.find { it.capitulo == 405.0 }
        val cap404 = volumes[0].capitulos.find { it.capitulo == 404.0 }
        assertTrue(cap405 != null)
        assertEquals("", cap405?.ingles)
        assertTrue(cap404 != null)
        assertEquals("New Beginning", cap404?.ingles)
    }
}
