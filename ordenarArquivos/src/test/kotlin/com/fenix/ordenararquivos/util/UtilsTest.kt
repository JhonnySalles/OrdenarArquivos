package com.fenix.ordenararquivos.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun testJapaneseNumberConversions() {
        // Test From Japanese
        val jpNumbers = "\uFF10\uFF11\uFF12\uFF13\uFF14\uFF15\uFF16\uFF17\uFF18\uFF19\uFF0E"
        val convertedFrom = Utils.fromNumberJapanese(jpNumbers)
        assertEquals("0123456789.", convertedFrom)

        // Test To Japanese
        val normalNumbers = "0123456789."
        val convertedTo = Utils.toNumberJapanese(normalNumbers)
        assertEquals("\uFF10\uFF11\uFF12\uFF13\uFF14\uFF15\uFF16\uFF17\uFF18\uFF19\uFF0E", convertedTo)
    }

    @Test
    fun testNormalizationTitleCase() {
        // Basic normalization
        assertEquals("One Piece", Utils.normaliza("ONE PIECE"))
        assertEquals("Minha Manga", Utils.normaliza("minha manga"))
        
        // Complex strings with special separators
        assertEquals("The Seven Deadly Sins: Nanatsu No Taizai", Utils.normaliza("the seven deadly sins: nanatsu no taizai"))
        assertEquals("It's A Great Story", Utils.normaliza("it's a great story"))
        assertEquals("Man-Machine", Utils.normaliza("man-machine"))
        
        // Empty/Blank strings
        assertEquals("", Utils.normaliza(""))
        assertEquals("   ", Utils.normaliza("   "))
    }

    @Test
    fun testPathHelperFunctions() {
        // Windows-style paths
        val winPath = "C:\\Users\\User\\Downloads\\Manga.cbr"
        assertEquals("C:\\Users\\User\\Downloads", Utils.getCaminho(winPath))
        assertEquals("Manga.cbr", Utils.getNome(winPath))
        assertEquals(".cbr", Utils.getExtenssao(winPath))

        // Unix-style paths
        val unixPath = "/home/user/mangas/onepiece.zip"
        assertEquals("/home/user/mangas", Utils.getCaminho(unixPath))
        assertEquals("onepiece.zip", Utils.getNome(unixPath))
        assertEquals(".zip", Utils.getExtenssao(unixPath))
    }

    @Test
    fun testFileFormatDetection() {
        assertTrue(Utils.isRar("test.rar"))
        assertTrue(Utils.isRar("test.CBR"))
        assertTrue(Utils.isRar("FOLDER/test.cbr"))
        
        assertFalse(Utils.isRar("test.zip"))
        assertFalse(Utils.isRar("test.pdf"))
        assertFalse(Utils.isRar("testrar"))
    }

    @Test
    fun testGetNumberFromText() {
        assertEquals(10.5, Utils.getNumber("Volume 10.5"))
        assertEquals(300.0, Utils.getNumber("Ch. 300!"))
        
        // Null for non-numbers
        assertNull(Utils.getNumber("No numbers here"))
    }

    @Test
    fun testLimparTitulo() {
        assertEquals("O Início", Utils.limparTitulo("Capítulo 01 — O Início"))
        assertEquals("The Beginning", Utils.limparTitulo("Chapter 1: The Beginning"))
        assertEquals("Special", Utils.limparTitulo("Ch. 10.5 Special"))
        assertEquals("Manga Name", Utils.limparTitulo("Manga Name"))
    }

    @Test
    fun testMD5() {
        val hash = Utils.MD5("test string")
        assertNotNull(hash)
        assertEquals(32, hash.length)
        
        // Same input should produce same hash
        assertEquals(hash, Utils.MD5("test string"))
        assertNotEquals(hash, Utils.MD5("other string"))
    }

    @Test
    fun testGetNomeNormalizadoOrdenacao() {
        // Testa a lógica de preenchimento de zeros à esquerda no final da string para ordenação
        // getNomeNormalizadoOrdenacao("Manga 1") -> "Manga 0000000001"
        val normalized = Utils.getNomeNormalizadoOrdenacao("Manga 1.zip")
        assertTrue(normalized.contains("0000000001"))
        assertTrue(normalized.endsWith(".zip"))
        
        val normalized2 = Utils.getNomeNormalizadoOrdenacao("Manga 10.zip")
        assertTrue(normalized2.contains("0000000010"))
    }

    @Test
    fun testFileFormatDetectionExtended() {
        assertTrue(Utils.isZip("test.zip"))
        assertTrue(Utils.isZip("test.CBZ"))
        assertTrue(Utils.isImage("test.png"))
        assertTrue(Utils.isImage("test.WEBP"))
        assertTrue(Utils.isSevenZ("test.7z"))
        assertTrue(Utils.isSevenZ("test.cb7"))
        assertTrue(Utils.isTarball("test.cbt"))
    }
}

