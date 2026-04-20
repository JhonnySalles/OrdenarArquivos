package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.process.Winrar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.io.File
import java.nio.file.Files

@ExtendWith(MockitoExtension::class)
class WinrarServicesTest {

    private lateinit var mService: WinrarServices
    private lateinit var tempFile: File

    @BeforeEach
    fun setUp() {
        mService = WinrarServices()
        tempFile = Files.createTempFile("test_winrar", ".rar").toFile()
        tempFile.deleteOnExit()
    }

    @Test
    fun testInsereArquivo_Single() {
        mockStatic(Winrar::class.java).use { winrarMock ->
            val arquivo = File("dummy.txt")
            winrarMock.`when`<Boolean> { Winrar.compactarArquivo(eq(tempFile), any()) }.thenReturn(true)
            
            val result = mService.insereArquivo(tempFile, arquivo)
            
            assertTrue(result)
            winrarMock.verify(MockedStatic.Verification { Winrar.compactarArquivo(eq(tempFile), any()) })
        }
    }

    @Test
    fun testInsereArquivo_List() {
        mockStatic(Winrar::class.java).use { winrarMock ->
            val arquivos = listOf(File("1.txt"), File("2.txt"))
            winrarMock.`when`<Boolean> { Winrar.compactarArquivo(eq(tempFile), eq(arquivos)) }.thenReturn(true)
            
            val result = mService.insereArquivo(tempFile, arquivos)
            
            assertTrue(result)
            winrarMock.verify(MockedStatic.Verification { Winrar.compactarArquivo(eq(tempFile), eq(arquivos)) })
        }
    }

    @Test
    fun testExtraiComicInfo() {
        mockStatic(Winrar::class.java).use { winrarMock ->
            val expectedResult = File("ComicInfo.xml")
            winrarMock.`when`<File?> { Winrar.extrairArquivo(any(), any()) }.thenReturn(expectedResult)
            
            val result = mService.extraiComicInfo(tempFile)
            
            assertNotNull(result)
            assertEquals("ComicInfo.xml", result?.name)
            winrarMock.verify(MockedStatic.Verification { Winrar.extrairArquivo(eq(tempFile), eq("ComicInfo.xml")) })
        }
    }

    @Test
    fun testExtraiSumario() {
        mockStatic(Winrar::class.java).use { winrarMock ->
            val tempDir = Files.createTempDirectory("test_sumario").toFile()
            tempDir.deleteOnExit()
            
            // Create a dummy zSumario file to test deletion
            val dummySumario = File(tempDir, "zSumário_old.txt")
            dummySumario.createNewFile()
            assertTrue(dummySumario.exists())

            val expectedResult = File(tempDir, "zSumário.txt")
            winrarMock.`when`<File?> { Winrar.extrairArquivo(any(), any()) }.thenReturn(expectedResult)
            
            val result = mService.extraiSumario(tempFile, tempDir)
            
            assertNotNull(result)
            assertFalse(dummySumario.exists(), "Arquivos de sumário antigos devem ser deletados")
            winrarMock.verify(MockedStatic.Verification { Winrar.extrairArquivo(eq(tempFile), eq("*zSumário.*")) })
            
            tempDir.deleteRecursively()
        }
    }
}
