package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.service.WinrarServices
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Path

class WinrarRobustnessTest {

    @TempDir
    lateinit var tempDir: Path
    
    private val winrarServices = WinrarServices()

    @Test
    fun testExtractionOfCorruptedFile() {
        val corruptedFile = File(tempDir.toFile(), "corrupted.zip")
        corruptedFile.writeText("THIS IS NOT A ZIP CONTENT")
        
        assertDoesNotThrow {
            // extraiComicInfo tenta extrair o XML. Em um arquivo corrompido, deve retornar null.
            val result = winrarServices.extraiComicInfo(corruptedFile)
            assertNull(result, "Resultado para arquivo corrompido deve ser nulo.")
        }
    }

    @Test
    fun testNonExistentFile() {
        val missingFile = File(tempDir.toFile(), "missing.rar")
        
        // Verificando comportamento com arquivo inexistente
        val result = winrarServices.extraiComicInfo(missingFile)
        assertNull(result, "Resultado para arquivo inexistente deve ser nulo.")
    }

    @Test
    fun testEmptyZipFile() {
        val emptyZip = File(tempDir.toFile(), "empty.zip")
        emptyZip.createNewFile()
        
        val result = winrarServices.extraiComicInfo(emptyZip)
        assertNull(result, "Resultado para zip vazio deve ser nulo.")
    }
}
