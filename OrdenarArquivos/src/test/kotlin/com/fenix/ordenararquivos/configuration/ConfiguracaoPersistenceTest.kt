package com.fenix.ordenararquivos.configuration

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
        
        // Recarregar para o estado original
        reloadConfig()
    }

    private fun reloadConfig() {
        val loadProps = Configuracao::class.java.getDeclaredMethod("loadProperties")
        loadProps.isAccessible = true
        loadProps.invoke(Configuracao)

        val loadSecrets = Configuracao::class.java.getDeclaredMethod("loadSecrets")
        loadSecrets.isAccessible = true
        loadSecrets.invoke(Configuracao)
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
        
        // 5. Forcar reload via reflection
        reloadConfig()
        
        // 6. Verificar se o objeto Configuracao refletiu a mudanca do disco
        assertEquals(120, Configuracao.registrosConsultaMal)
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
