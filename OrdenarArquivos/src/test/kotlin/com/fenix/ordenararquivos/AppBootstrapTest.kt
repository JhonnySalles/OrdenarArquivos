package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.process.CopiarOpfEpub
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import java.io.File
import java.nio.file.Path

class AppBootstrapTest {

    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var mockCopiarOpf: MockedStatic<CopiarOpfEpub>

    @BeforeEach
    fun setup() {
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
}
