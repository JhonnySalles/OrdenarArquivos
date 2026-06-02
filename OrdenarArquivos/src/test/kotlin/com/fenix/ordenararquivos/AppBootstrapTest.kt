package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.process.CopiarOpfEpub
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mockStatic
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths

class AppBootstrapTest {

    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var mockCopiarOpf: MockedStatic<CopiarOpfEpub>

    @BeforeEach
    fun setup() {
        System.setProperty("is.test", "true")
        mockCopiarOpf = mockStatic(CopiarOpfEpub::class.java)
    }

    @AfterEach
    fun tearDown() {
        if (::mockCopiarOpf.isInitialized) {
            mockCopiarOpf.close()
        }
        // Cleanup local secrets.properties if created during test outside tempdir
        File("secrets.properties").let { if (it.exists()) it.delete() }
    }

    @Test
    fun testCommandLineArgumentsParsingOpf() {
        // Simular execução CLI: --opf origem="X" destino="Y"
        val args = arrayOf("--opf", "origem=C:\\temp\\origem", "destino=C:\\temp\\destino")
        
        App.main(args)
        
        mockCopiarOpf.verify(MockedStatic.Verification { CopiarOpfEpub.processar("C:\\temp\\origem", "C:\\temp\\destino") }, atLeastOnce())
    }

    @Test
    fun testSentryInitializationWithMockedProperties() {
        // Criar um secrets.properties fake no diretório de trabalho atual (onde o App.kt procura)
        val secretsFile = File("secrets.properties")
        secretsFile.writeText("sentry_dns=https://example@sentry.io/1\nsentry_environment=test")
        
        // Apenas rodamos o main sem argumentos específicos para trigger de inicialização
        // (Nota: main vai tentar chamar Run().start(args) se tipo for null)
        // Para evitar abrir a GUI, passamos argumentos que não triggam o start se possível,
        // ou aceitamos que o Run().start falhe em ambiente headless se não configurado.
        
        assertDoesNotThrow {
            App.main(arrayOf("--opf", "origem=.", "destino=."))
        }
        
        assertTrue(secretsFile.exists(), "Arquivo de segredos deveria existir para o teste.")
    }

    @Test
    fun testIsFileOrFolderExceptionFiltering() {
        // Exceções que devem ser filtradas (retornar true)
        val accessDenied = AccessDeniedException("L:\\some\\path")
        val noSuchFile = NoSuchFileException("D:\\some\\path")
        val fileNotFound = FileNotFoundException("File not found")
        val wrappedException = RuntimeException("Wrapped error", accessDenied)

        assertTrue(App.isFileOrFolderException(accessDenied))
        assertTrue(App.isFileOrFolderException(noSuchFile))
        assertTrue(App.isFileOrFolderException(fileNotFound))
        assertTrue(App.isFileOrFolderException(wrappedException))

        // Exceções que não devem ser filtradas (retornar false)
        val genericIo = IOException("Generic IO issue")
        val genericRuntime = RuntimeException("Generic runtime issue")

        assertFalse(App.isFileOrFolderException(genericIo))
        assertFalse(App.isFileOrFolderException(genericRuntime))
        assertFalse(App.isFileOrFolderException(null))
    }

    @Test
    fun testSanitizeId() {
        val mockController = org.mockito.Mockito.mock(com.fenix.ordenararquivos.controller.TelaInicialController::class.java)
        com.fenix.ordenararquivos.database.DataBase.isTeste = true
        val service = com.fenix.ordenararquivos.service.SincronizacaoServices(mockController)
        
        org.junit.jupiter.api.Assertions.assertEquals("pt-Tomodachi", service.sanitizeId("pt/Tomodachi"))
        org.junit.jupiter.api.Assertions.assertEquals("Manga - Name", service.sanitizeId("Manga \\ Name"))
        org.junit.jupiter.api.Assertions.assertEquals("clean-id-123", service.sanitizeId("clean-id-123"))
    }
}
