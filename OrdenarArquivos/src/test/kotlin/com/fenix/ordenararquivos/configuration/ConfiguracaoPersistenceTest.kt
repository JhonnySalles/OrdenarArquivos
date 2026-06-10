package com.fenix.ordenararquivos.configuration

import com.fenix.ordenararquivos.model.enums.OcrEngine
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfiguracaoPersistenceTest {

    private val appFile = File("app.properties")
    private val secretsFile = File("secrets.properties")
    
    private var appBackup: ByteArray? = null
    private var secretsBackup: ByteArray? = null

    @BeforeAll
    fun backup() {
        if (appFile.exists()) appBackup = appFile.readBytes()
        if (secretsFile.exists()) secretsBackup = secretsFile.readBytes()
    }

    @AfterAll
    fun restore() {
        if (appBackup != null) appFile.writeBytes(appBackup!!) else appFile.delete()
        if (secretsBackup != null) secretsFile.writeBytes(secretsBackup!!) else secretsFile.delete()
        
        Configuracao.reload()
    }

    private fun reloadConfig() {
        Configuracao.reload()
    }

    @Test
    fun `test salvar e carregar propriedades do disco`() {
        val testPath = "C:/Test/Path/Tagger"
        val testUpdate = "https://test.update.com"
        
        // 1. Modificar em memoria
        Configuracao.caminhoCommicTagger = testPath
        Configuracao.updateLink = testUpdate
        Configuracao.registrosConsultaMal = 75
        
        // 2. Salvar no disco
        Configuracao.saveProperties()
        
        // 3. Verificar se o arquivo existe e contem os dados
        assertTrue(appFile.exists(), "O arquivo app.properties deveria ter sido criado")
        val props = Properties()
        appFile.inputStream().use { props.load(it) }
        
        assertEquals(testPath, props.getProperty("caminho.commictagger"))
        assertEquals(testUpdate, props.getProperty("app.update_link"))
        assertEquals("75", props.getProperty("mal.registros_consulta"))
        
        // 4. Modificar o arquivo no disco manualmente para testar o carregamento
        props.setProperty("mal.registros_consulta", "120")
        appFile.outputStream().use { props.store(it, null) }
        
        // 5. Forcar reload em memoria
        reloadConfig()
        
        // 6. Verificar se o objeto Configuracao refletiu a mudanca do disco
        assertEquals(120, Configuracao.registrosConsultaMal)
    }

    @Test
    fun `test salvar e carregar ocr engine`() {
        val original = Configuracao.ocrEngine
        try {
            Configuracao.ocrEngine = OcrEngine.PADDLE
            Configuracao.saveProperties()

            val props = Properties()
            appFile.inputStream().use { props.load(it) }
            assertEquals(OcrEngine.PADDLE.configValue, props.getProperty("ocr.engine"))

            props.setProperty("ocr.engine", OcrEngine.GEMINI.configValue)
            appFile.outputStream().use { props.store(it, null) }
            reloadConfig()
            assertEquals(OcrEngine.GEMINI, Configuracao.ocrEngine)
        } finally {
            Configuracao.ocrEngine = original
            Configuracao.saveProperties()
        }
    }

    @Test
    fun `test salvar e carregar parametros ollama e paddle`() {
        val originalOllamaUrl = Configuracao.ollamaUrl
        val originalOllamaModel = Configuracao.ollamaModel
        val originalPaddleCls = Configuracao.paddleCls
        val originalPaddleAngle = Configuracao.paddleUseAngleCls
        val originalPaddleLimit = Configuracao.paddleLimitSideLen

        try {
            Configuracao.ollamaUrl = "http://127.0.0.1:11434"
            Configuracao.ollamaModel = "llava"
            Configuracao.paddleCls = false
            Configuracao.paddleUseAngleCls = true
            Configuracao.paddleLimitSideLen = 1920
            Configuracao.saveProperties()

            val props = Properties()
            appFile.inputStream().use { props.load(it) }
            assertEquals("http://127.0.0.1:11434", props.getProperty("ocr.ollama.url"))
            assertEquals("llava", props.getProperty("ocr.ollama.model"))
            assertEquals("false", props.getProperty("ocr.paddle.cls"))
            assertEquals("true", props.getProperty("ocr.paddle.use_angle_cls"))
            assertEquals("1920", props.getProperty("ocr.paddle.limit_side_len"))

            reloadConfig()
            assertEquals("http://127.0.0.1:11434", Configuracao.ollamaUrl)
            assertEquals("llava", Configuracao.ollamaModel)
            assertFalse(Configuracao.paddleCls)
            assertTrue(Configuracao.paddleUseAngleCls)
            assertEquals(1920, Configuracao.paddleLimitSideLen)
        } finally {
            Configuracao.ollamaUrl = originalOllamaUrl
            Configuracao.ollamaModel = originalOllamaModel
            Configuracao.paddleCls = originalPaddleCls
            Configuracao.paddleUseAngleCls = originalPaddleAngle
            Configuracao.paddleLimitSideLen = originalPaddleLimit
            Configuracao.saveProperties()
        }
    }

    @Test
    fun `test persistencia de segredos`() {
        // Segredos sao apenas leitura pelo objeto, entao testamos a criacao do arquivo default
        if (secretsFile.exists()) secretsFile.delete()
        
        reloadConfig()
        
        assertTrue(secretsFile.exists(), "O arquivo secrets.properties deveria ser criado automaticamente se faltar")
        
        val props = Properties()
        secretsFile.inputStream().use { props.load(it) }
        
        assertTrue(props.containsKey("google_drive_api_key"))
        assertTrue(props.containsKey("gemini_api_key_1"))
    }

    @Test
    fun `test integridade do arquivo secrets editado manualmente`() {
        val testKey = "fake_key_123"
        val props = Properties()
        
        // Simular edicao manual do usuario no secrets.properties
        if (secretsFile.exists()) secretsFile.inputStream().use { props.load(it) }
        props.setProperty("google_drive_api_key", testKey)
        secretsFile.outputStream().use { props.store(it, null) }
        
        reloadConfig()
        
        assertEquals(testKey, Configuracao.googleDriveApiKey, "Configuracao deveria ler a chave alterada no disco")
    }
}
