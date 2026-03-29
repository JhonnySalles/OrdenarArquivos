package com.fenix.ordenararquivos.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class UtilsTest {

    @Test
    fun `test toDateTime with valid date`() {
        val dateStr = "2023-10-27T10:15:30"
        val expected = LocalDateTime.of(2023, 10, 27, 10, 15, 30)
        assertEquals(expected, Utils.toDateTime(dateStr))
    }

    @Test
    fun `test toDateTime with empty string`() {
        assertEquals(LocalDateTime.MIN, Utils.toDateTime(""))
    }

    @Test
    fun `test fromDateTime`() {
        val dateTime = LocalDateTime.of(2023, 10, 27, 10, 15, 30)
        val expected = "2023-10-27T10:15:30"
        assertEquals(expected, Utils.fromDateTime(dateTime))
    }

    @Test
    fun `test isRar`() {
        assertTrue(Utils.isRar("file.rar"))
        assertTrue(Utils.isRar("file.cbr"))
        assertTrue(Utils.isRar("FILE.RAR"))
        assertTrue(Utils.isRar("FILE.CBR"))
        assertFalse(Utils.isRar("file.zip"))
        assertFalse(Utils.isRar("file.txt"))
        assertFalse(Utils.isRar("rarfile"))
    }

    @Test
    fun `test getCaminho`() {
        assertEquals("C:/folder", Utils.getCaminho("C:/folder/file.txt"))
        assertEquals("C:\\folder", Utils.getCaminho("C:\\folder\\file.txt"))
        assertEquals("folder", Utils.getCaminho("folder/file.txt"))
        assertEquals("path/to", Utils.getCaminho("path/to/nested/file.txt").let { Utils.getCaminho(it) })
    }

    @Test
    fun `test getNome`() {
        assertEquals("file.txt", Utils.getNome("C:/folder/file.txt"))
        assertEquals("file.txt", Utils.getNome("C:\\folder\\file.txt"))
        assertEquals("file.txt", Utils.getNome("file.txt"))
    }

    @Test
    fun `test getExtenssao`() {
        assertEquals(".txt", Utils.getExtenssao("file.txt"))
        assertEquals(".rar", Utils.getExtenssao("archive.rar"))
        assertEquals("noextension", Utils.getExtenssao("noextension"))
        assertEquals(".gz", Utils.getExtenssao("file.tar.gz"))
    }

    @Test
    fun `test getNumber`() {
        assertEquals(123.45, Utils.getNumber("Volume 123.45"))
        assertEquals(1.0, Utils.getNumber("Chapter 1"))
        assertEquals(50.5, Utils.getNumber("abc 50.5 xyz"))
        assertNull(Utils.getNumber("No number here"))
    }

    @Test
    fun `test fromNumberJapanese`() {
        assertEquals("0123.45", Utils.fromNumberJapanese("\uFF10\uFF11\uFF12\uFF13\uFF0E\uFF14\uFF15"))
        assertEquals("987", Utils.fromNumberJapanese("\uFF19\uFF18\uFF17"))
        assertEquals("abc1", Utils.fromNumberJapanese("abc\uFF11"))
    }

    @Test
    fun `test toNumberJapanese`() {
        assertEquals("\uFF10\uFF11\uFF12\uFF13\uFF0E\uFF14\uFF15", Utils.toNumberJapanese("0123.45"))
        assertEquals("\uFF19\uFF18\uFF17", Utils.toNumberJapanese("987"))
        assertEquals("abc\uFF11", Utils.toNumberJapanese("abc1"))
    }

    @Test
    fun `test normaliza`() {
        assertEquals("Manga Name", Utils.normaliza("manga name"))
        assertEquals("One-Piece", Utils.normaliza("one-piece"))
        assertEquals("It'S Me", Utils.normaliza("it's me"))
        assertEquals("Already Capitalized", Utils.normaliza("Already Capitalized"))
        assertEquals("Multiple Spaces", Utils.normaliza("multiple spaces"))
        assertEquals("", Utils.normaliza(""))
    }
}
