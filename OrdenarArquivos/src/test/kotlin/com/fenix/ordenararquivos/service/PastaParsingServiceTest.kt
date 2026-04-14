package com.fenix.ordenararquivos.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PastaParsingServiceTest {

    private val service = PastaParsingService()

    @Test
    fun testParseStandardFormat() {
        val fileName = "[Scan] Manga Name - Vol 01 - Capítulo 05 - O Início"
        val result = service.parse(fileName)
        
        assertEquals("01", result.volume)
        assertEquals("05", result.capitulo)
        assertEquals("Scan", result.scan)
        assertEquals("O Início", result.titulo)
        assertFalse(result.isCapa)
    }

    @Test
    fun testParseOnlyCapitulo() {
        val fileName = "010.5"
        val result = service.parse(fileName)
        
        assertEquals("0", result.volume)
        assertEquals("010.5", result.capitulo)
        assertEquals("", result.scan)
    }

    @Test
    fun testParseWithShortSeparators() {
        val fileName = "[Scan] Manga ch.10 vol.2"
        val result = service.parse(fileName)
        
        assertEquals("2", result.volume)
        assertEquals("10", result.capitulo)
        assertEquals("Scan", result.scan)
    }

    @Test
    fun testParseCapa() {
        val fileName = "[Scan] Manga Vol 01 Capa"
        val result = service.parse(fileName)
        
        assertEquals("01", result.volume)
        assertEquals("0", result.capitulo)
        assertTrue(result.isCapa)
    }

    @Test
    fun testParseWithUnderscores() {
        val fileName = "Scan_Manga_Name_Capitulo_01"
        val result = service.parse(fileName)
        
        assertEquals("01", result.capitulo)
        assertEquals("Scan Manga Name", result.scan)
    }

    @Test
    fun testComplexScenario() {
        val fileName = "[Membro] Manga_Vol.10_Chapter_050.5_Special"
        val result = service.parse(fileName)
        
        assertEquals("10", result.volume)
        assertEquals("050.5", result.capitulo)
        assertEquals("Membro", result.scan)
        assertEquals("Special", result.titulo)
    }
}
