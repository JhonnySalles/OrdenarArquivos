package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.model.entities.DriveFile
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.File
import java.nio.file.Files

class GoogleDriveServiceTest {

    private val downloadService = GoogleDriveDownloadService()

    @Test
    fun `test extrair id da pasta do link drive`() {
        val url = "https://drive.google.com/drive/folders/1a2b3c4d5e6f7g8h9i0j_k_l_m_n_o_p_q"
        val id = downloadService.extractFolderId(url)
        assertEquals("1a2b3c4d5e6f7g8h9i0j_k_l_m_n_o_p_q", id)
    }

    @Test
    fun `test extrair id da pasta link invalido deve lancar erro`() {
        val url = "https://google.com/search?q=teste"
        assertThrows(IllegalArgumentException::class.java) {
            downloadService.extractFolderId(url)
        }
    }

    @Test
    fun `test comparacao de versoes`() {
        val files = listOf(
            DriveFile("1", "ordenarArquivos-1.0.0.jar", 100),
            DriveFile("2", "ordenarArquivos-1.0.1.jar", 100),
            DriveFile("3", "ordenarArquivos-1.0.1-SNAPSHOT.jar", 100)
        )
        
        val latest = downloadService.findLatestVersion(files)
        assertEquals("1.0.1", latest, "Versao release deve ser maior que SNAPSHOT e versao menor")
    }

    @Test
    fun `test encontrar ultima versao em lista mista`() {
        val files = listOf(
            DriveFile("1", "ordenarArquivos-0.9.0.jar", 50),
            DriveFile("2", "ordenarArquivos-1.1.0-SNAPSHOT.jar", 50),
            DriveFile("3", "ordenarArquivos-1.0.5.jar", 50)
        )
        val latest = downloadService.findLatestVersion(files)
        assertEquals("1.1.0-SNAPSHOT", latest, "1.1.0-SNAPSHOT e maior que 1.0.5")
    }

    @Test
    fun `test geracao de arquivo bat de execucao`() {
        val tempDir = Files.createTempDirectory("test_bat").toFile()
        try {
            val version = "1.2.3"
            downloadService.updateBatFile(tempDir, version)
            
            val batFile = File(tempDir, "ExecutarOrdenaArquivo.bat")
            assertTrue(batFile.exists())
            
            val content = batFile.readText()
            assertTrue(content.contains("ordenarArquivos-1.2.3-jar-with-dependencies.jar"))
            assertTrue(content.contains("java -Dfile.encoding=utf-8"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test regex de confirmacao de download do google drive`() {
        // Simula o HTML que o Google retorna quando o arquivo e grande demais para scan de virus
        val html = """
            <html>
                <body>
                    <form action="https://drive.google.com/uc?export=download&confirm=t_ABCD&id=file_123" method="post">
                        <input type="hidden" name="confirm" value="t_ABCD">
                        <input type="hidden" name="uuid" value="uuid_999">
                    </form>
                    <a href="/uc?export=download&confirm=t_ABCD&id=file_123">Download anyway</a>
                </body>
            </html>
        """.trimIndent()

        val confirmToken = Regex("""confirm=([^&"'\s>]+)""").find(html)?.groupValues?.get(1)
        val uuid = Regex("""name=["']uuid["']\s+value=["']([^"']+)["']""").find(html)?.groupValues?.get(1)

        assertEquals("t_ABCD", confirmToken)
        assertEquals("uuid_999", uuid)
    }

    @Test
    fun `test upload file deve atualizar se ja existir`() {
        // Injetar ID de pasta via reflection para passar na validação do Configuracao
        val secretsField = com.fenix.ordenararquivos.configuration.Configuracao.javaClass.getDeclaredField("secrets")
        secretsField.isAccessible = true
        (secretsField.get(com.fenix.ordenararquivos.configuration.Configuracao) as java.util.Properties)
            .setProperty("google_drive_folder_id", "test_folder_id")

        val mockDrive: Drive = mock()
        val mockFiles: Drive.Files = mock()
        val mockList: Drive.Files.List = mock()
        val mockUpdate: Drive.Files.Update = mock()
        
        val fileId = "existing_id"
        val fileName = "test_upload.jar"
        
        val existingFile = com.google.api.services.drive.model.File().apply {
            id = fileId
            name = fileName
        }
        val fileList = FileList().setFiles(listOf(existingFile))

        whenever(mockDrive.files()).thenReturn(mockFiles)
        whenever(mockFiles.list()).thenReturn(mockList)
        whenever(mockList.setQ(any())).thenReturn(mockList)
        whenever(mockList.setSpaces(any())).thenReturn(mockList)
        whenever(mockList.setFields(any())).thenReturn(mockList)
        whenever(mockList.execute()).thenReturn(fileList)

        whenever(mockFiles.update(eq(fileId), anyOrNull(), any())).thenReturn(mockUpdate)
        whenever(mockUpdate.setFields(any())).thenReturn(mockUpdate)

        // Criar diretório temporário e arquivo com nome fixo para coincidir com o Mock
        val tempDir = Files.createTempDirectory("test_upload_dir").toFile()
        val tempFile = File(tempDir, fileName)
        tempFile.writeText("dummy content")

        try {
            val method = GoogleDriveUploadService::class.java.getDeclaredMethod("uploadFile", Drive::class.java, String::class.java)
            method.isAccessible = true
            method.invoke(GoogleDriveUploadService, mockDrive, tempFile.absolutePath)

            verify(mockFiles).update(eq(fileId), anyOrNull(), any())
            verify(mockFiles, never()).create(any(), any())
        } finally {
            tempFile.delete()
            tempDir.delete()
            // Limpar segredo após o teste
            (secretsField.get(com.fenix.ordenararquivos.configuration.Configuracao) as java.util.Properties)
                .remove("google_drive_folder_id")
        }
    }
}
