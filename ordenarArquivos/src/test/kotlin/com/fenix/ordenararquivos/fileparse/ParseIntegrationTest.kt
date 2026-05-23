package com.fenix.ordenararquivos.fileparse

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ParseIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testZipParse() {
        val zipFile = File(tempDir.toFile(), "test.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry = ZipEntry("capitulo1/01.jpg")
            zos.putNextEntry(entry)
            zos.write("DUMMY_IMAGE_CONTENT_ZIP".toByteArray())
            zos.closeEntry()
        }

        val parse = ParseFactory.create(zipFile)
        assertTrue(parse is ZipParse)
        assertEquals("zip", parse.getTipo())
        assertEquals(1, parse.getSize())
        assertEquals("capitulo1/01.jpg", parse.getPaginaPasta(0))

        parse.getPagina(0).use { input ->
            val content = input.bufferedReader().readText()
            assertEquals("DUMMY_IMAGE_CONTENT_ZIP", content)
        }
        parse.destroir()
    }

    @Test
    fun testTarParse() {
        val tarFile = File(tempDir.toFile(), "test.cbt")
        TarArchiveOutputStream(FileOutputStream(tarFile)).use { tos ->
            val contentBytes = "DUMMY_IMAGE_CONTENT_TAR".toByteArray()
            val entry = TarArchiveEntry("capitulo1/01.jpg")
            entry.size = contentBytes.size.toLong()
            tos.putArchiveEntry(entry)
            tos.write(contentBytes)
            tos.closeArchiveEntry()
        }

        val parse = ParseFactory.create(tarFile)
        assertTrue(parse is TarParse)
        assertEquals("tar", parse.getTipo())
        assertEquals(1, parse.getSize())
        assertEquals("capitulo1/01.jpg", parse.getPaginaPasta(0))

        parse.getPagina(0).use { input ->
            val content = input.bufferedReader().readText()
            assertEquals("DUMMY_IMAGE_CONTENT_TAR", content)
        }
        parse.destroir()
    }

    @Test
    @Disabled("Requer a dependência opcional org.tukaani:xz para LZMA2/XZ compressão no 7z")
    fun testSevenZParse() {
        val sevenZFile = File(tempDir.toFile(), "test.7z")
        SevenZOutputFile(sevenZFile).use { szOut ->
            szOut.setContentCompression(SevenZMethod.COPY)
            val contentBytes = "DUMMY_IMAGE_CONTENT_7Z".toByteArray()
            val entry = szOut.createArchiveEntry(File(tempDir.toFile(), "01.jpg"), "capitulo1/01.jpg")
            entry.size = contentBytes.size.toLong()
            szOut.putArchiveEntry(entry)
            szOut.write(contentBytes)
            szOut.closeArchiveEntry()
        }

        val parse = ParseFactory.create(sevenZFile)
        assertTrue(parse is SevenZParse)
        assertEquals("tar", parse.getTipo()) // Note: SevenZParse returns "tar" in getTipo() based on code
        assertEquals(1, parse.getSize())
        assertEquals("capitulo1/01.jpg", parse.getPaginaPasta(0))

        parse.getPagina(0).use { input ->
            val content = input.bufferedReader().readText()
            assertEquals("DUMMY_IMAGE_CONTENT_7Z", content)
        }
        parse.destroir()
    }
}
