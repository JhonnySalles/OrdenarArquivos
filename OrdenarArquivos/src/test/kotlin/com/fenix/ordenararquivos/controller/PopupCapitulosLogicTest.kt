package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PopupCapitulosLogicTest : BaseJfxTest() {

    private val controller = PopupCapitulos()

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
}
