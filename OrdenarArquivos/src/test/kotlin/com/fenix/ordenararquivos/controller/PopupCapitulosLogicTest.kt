package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertTrue(volumes[0].capitulos.any { it.capitulo == 206.0 })
    }

    @Test
    fun testExtractMangaHere() {
        val htmlFile = File("src/test/resources/fixtures/mangahere.html")
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val volumes = controller.extractMangaHere(doc)

        assertEquals(1, volumes.size)
        assertTrue(volumes[0].capitulos.any { it.capitulo == 71.0 })
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
}
