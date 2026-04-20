package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.process.Ocr
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class OcrServicesIntegrationTest {

    @TempDir
    lateinit var tempDir: Path
    
    private val ocrServices = OcrServices()
    
    @BeforeEach
    fun setUp() {
        Ocr.isTeste = true
    }

    @AfterEach
    fun tearDown() {
        Ocr.isTeste = false
    }

    @Test
    fun testProcessOcrBasicFlow() {
        val sumarioFile = File(tempDir.toFile(), "sumario.txt")
        sumarioFile.writeText("Página 1\nCapítulo 1: Início\nPágina 10\nCapítulo 2: Meio")
        
        // Simular o comportamento do motor OCR
        // Nota: O motor OCR real (Ocr.kt) pode ter lógica específica que precisamos respeitar
        val result = ocrServices.processOcr(sumarioFile, "Página", "Capítulo")
        
        // Validar se o resultado contém os marcadores esperados conforme a lógica do Ocr.kt
        assertNotNull(result)
        assertTrue(result.contains("1") || result.contains("Início"), "OCR deveria ter processado o conteúdo.")
    }

    @Test
    fun testProcessOcrEmptyFile() {
        val emptyFile = File(tempDir.toFile(), "empty.txt").apply { createNewFile() }
        val result = ocrServices.processOcr(emptyFile, "Page", "Chap")
        assertEquals("", result, "Resultado para arquivo vazio deve ser string vazia.")
    }

    @Test
    fun testProcessOcrComplexSeparators() {
        val sumarioFile = File(tempDir.toFile(), "complex_sumario.txt")
        sumarioFile.writeText("[PAGE 1]\n{CHAP 01} Hero's Journey\n[PAGE 5]\n{CHAP 02} Conflict")
        
        val result = ocrServices.processOcr(sumarioFile, "[PAGE", "{CHAP")
        assertNotNull(result)
        // O motor OCR deveria ser capaz de lidar com prefixos de separadores
    }
}
