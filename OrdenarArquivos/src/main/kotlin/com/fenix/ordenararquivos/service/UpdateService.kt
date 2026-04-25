package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.entities.DriveFile
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class UpdateService {

    private val mLOG = LoggerFactory.getLogger(UpdateService::class.java)

    fun extractFolderId(url: String): String {
        val regex = Regex("""folders/([a-zA-Z0-9_-]+)""")
        val match = regex.find(url)
        return match?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Não foi possível extrair o ID da pasta do link: $url")
    }

    fun listJarFiles(folderId: String): List<DriveFile> {
        val apiKey = Configuracao.googleDriveApiKey
        if (apiKey.isBlank())
            throw IllegalStateException("API Key do Google Drive não configurada em secrets.properties (google_drive_api_key)")

        val query = URLEncoder.encode("'$folderId' in parents and trashed=false and mimeType='application/java-archive'", "UTF-8")
        val fields = URLEncoder.encode("files(id,name,size)", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$query&key=$apiKey&fields=$fields&pageSize=100"

        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        try {
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Sem detalhes"
                throw RuntimeException("Erro ao listar arquivos do Google Drive (HTTP $responseCode): $errorBody")
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val jsonObject = JsonParser.parseString(responseBody).asJsonObject
            val filesArray = jsonObject.getAsJsonArray("files") ?: return emptyList()

            return filesArray.map { element ->
                val obj = element.asJsonObject
                DriveFile(
                    id = obj.get("id").asString,
                    name = obj.get("name").asString,
                    size = obj.get("size")?.asLong ?: 0
                )
            }.filter { it.name.endsWith(".jar") }
        } finally {
            connection.disconnect()
        }
    }

    fun findLatestVersion(files: List<DriveFile>): String? {
        return files
            .mapNotNull { it.version.ifBlank { null } }
            .distinct()
            .maxWithOrNull(Comparator { a, b -> compareVersions(a, b) })
    }

    fun getCurrentVersion(): String? {
        val currentDir = File(System.getProperty("user.dir"))
        val jarFile = currentDir.listFiles()
            ?.filter { it.name.matches(Regex("""ordenarArquivos-.+?\.jar""")) }
            ?.filter { !it.name.endsWith(".old") }
            ?.maxByOrNull { it.lastModified() }

        if (jarFile != null) {
            val regex = Regex("""ordenarArquivos-(.+?)(?:-jar-with-dependencies)?\.jar""")
            val match = regex.find(jarFile.name)
            return match?.groupValues?.get(1)
        }
        return null
    }

    fun downloadFile(fileId: String, destination: File, onProgress: ((Long, Long) -> Unit)? = null) {
        val url = "https://drive.google.com/uc?id=$fileId&export=download"
        var connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        try {
            connection.connect()
            var responseCode = connection.responseCode

            // Handle virus scan warning for large files
            if (responseCode == 200) {
                val contentType = connection.contentType ?: ""
                if (contentType.contains("text/html")) {
                    val html = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    // Tenta encontrar o token ou o link usando padrões variados (URL ou Campos de Formulário)
                    val confirmToken = Regex("""confirm=([^&"'\s>]+)""").find(html)?.groupValues?.get(1)
                        ?: Regex("""name=["']confirm["']\s+value=["']([^"']+)["']""").find(html)?.groupValues?.get(1)

                    val uuid = Regex("""uuid=([^&"'\s>]+)""").find(html)?.groupValues?.get(1)
                        ?: Regex("""name=["']uuid["']\s+value=["']([^"']+)["']""").find(html)?.groupValues?.get(1)

                    var confirmUrl: String? = when {
                        confirmToken != null && uuid != null -> {
                            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=$confirmToken&uuid=$uuid"
                        }
                        confirmToken != null -> {
                            val cleanToken = confirmToken.replace("&amp;", "&")
                            if (cleanToken.startsWith("/uc?")) "https://drive.google.com$cleanToken"
                            else "$url&confirm=$cleanToken"
                        }
                        uuid != null -> {
                            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t&uuid=$uuid"
                        }
                        else -> {
                            // Tenta encontrar qualquer link que contenha /uc? e confirm= (mais permissivo)
                            val manualLink = Regex("""(/uc\?[^"'\s>]*confirm=[^"'\s>]*)""").find(html)?.groupValues?.get(1)
                                ?.replace("&amp;", "&")
                            if (manualLink != null) {
                                if (manualLink.startsWith("http")) manualLink
                                else "https://drive.google.com$manualLink"
                            } else null
                        }
                    }

                    if (confirmUrl != null) {
                        mLOG.info("Link de confirmação encontrado: $confirmUrl")
                        connection = URI(confirmUrl).toURL().openConnection() as HttpURLConnection
                        connection.instanceFollowRedirects = true
                        connection.connectTimeout = 30000
                        connection.readTimeout = 60000
                        connection.connect()
                        responseCode = connection.responseCode
                    } else {
                        // Log mais detalhado para identificar o problema na estrutura do HTML
                        val bodySnippet = if (html.contains("<body")) html.substring(html.indexOf("<body")) else html
                        val finalSnippet = if (bodySnippet.length > 2000) bodySnippet.substring(0, 2000) else bodySnippet
                        mLOG.error("Google Drive: Falha ao extrair confirmação. Título: ${Regex("<title>(.*?)</title>").find(html)?.groupValues?.get(1)}. Snippet do Body: $finalSnippet")
                        throw RuntimeException("Erro ao baixar arquivo: Google Drive retornou um aviso ou página de erro (HTML) sem link de confirmação detectável.")
                    }
                }
            }

            if (responseCode != 200)
                throw RuntimeException("Erro ao baixar arquivo (HTTP $responseCode)")

            val totalSize = connection.contentLengthLong
            destination.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        onProgress?.invoke(totalBytesRead, totalSize)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun performUpdate(
        onProgress: (message: String, progress: Double) -> Unit
    ): UpdateResult {
        val updateLink = Configuracao.updateLink
        if (updateLink.isBlank())
            throw IllegalStateException("Link de atualização não configurado em app.properties (app.update_link)")

        onProgress("Verificando atualizações...", -1.0)
        val folderId = extractFolderId(updateLink)
        val remoteFiles = listJarFiles(folderId)

        if (remoteFiles.isEmpty())
            throw RuntimeException("Nenhum arquivo JAR encontrado na pasta do Google Drive.")

        val remoteVersion = findLatestVersion(remoteFiles)
            ?: throw RuntimeException("Não foi possível determinar a versão dos arquivos remotos.")

        val currentVersion = getCurrentVersion()
        mLOG.info("Versão atual: $currentVersion | Versão remota: $remoteVersion")

        if (currentVersion != null && compareVersions(currentVersion, remoteVersion) >= 0) {
            return UpdateResult(false, currentVersion, remoteVersion, "Aplicativo já está na versão mais recente ($currentVersion).")
        }

        val filesToDownload = remoteFiles.filter { it.version == remoteVersion }
        val currentDir = File(System.getProperty("user.dir"))
        val tempDir = File(currentDir, "temp/update")
        tempDir.mkdirs()

        val totalFiles = filesToDownload.size
        for ((index, driveFile) in filesToDownload.withIndex()) {
            onProgress("Baixando ${driveFile.name} (${index + 1}/$totalFiles)...", index.toDouble() / totalFiles)
            val destFile = File(tempDir, driveFile.name)
            downloadFile(driveFile.id, destFile) { bytesRead, totalBytes ->
                if (totalBytes > 0) {
                    val fileProgress = bytesRead.toDouble() / totalBytes
                    val overallProgress = (index + fileProgress) / totalFiles
                    onProgress("Baixando ${driveFile.name}... ${(fileProgress * 100).toInt()}%", overallProgress)
                }
            }
            mLOG.info("Download concluído: ${driveFile.name}")
        }

        onProgress("Substituindo arquivos...", 0.9)
        val downloadedFiles = tempDir.listFiles()?.filter { it.name.endsWith(".jar") } ?: emptyList()

        for (newJar in downloadedFiles) {
            val existingJar = File(currentDir, newJar.name)
            val isWithDeps = newJar.name.contains("jar-with-dependencies")
            val oldPattern = if (isWithDeps) {
                Regex("""ordenarArquivos-.+-jar-with-dependencies\.jar""")
            } else {
                Regex("""ordenarArquivos-[^-]+-SNAPSHOT\.jar$""")
            }

            currentDir.listFiles()
                ?.filter { it.name.matches(oldPattern) && it.name != newJar.name }
                ?.forEach { oldFile ->
                    val backupFile = File(currentDir, "${oldFile.name}.old")
                    if (backupFile.exists()) backupFile.delete()
                    oldFile.renameTo(backupFile)
                    mLOG.info("Arquivo antigo renomeado: ${oldFile.name} -> ${backupFile.name}")
                }

            if (existingJar.exists()) {
                val backupFile = File(currentDir, "${existingJar.name}.old")
                if (backupFile.exists()) backupFile.delete()
                existingJar.renameTo(backupFile)
            }

            Files.copy(newJar.toPath(), existingJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            mLOG.info("Novo arquivo copiado: ${newJar.name}")
        }

        onProgress("Atualizando ExecutarOrdenaArquivo.bat...", 0.95)
        updateBatFile(currentDir, remoteVersion)

        tempDir.listFiles()?.forEach { it.delete() }
        tempDir.delete()

        onProgress("Atualização concluída! Reiniciando...", 1.0)
        return UpdateResult(true, currentVersion, remoteVersion, "Atualizado de $currentVersion para $remoteVersion.")
    }

    fun updateBatFile(directory: File, version: String) {
        val batFile = File(directory, "ExecutarOrdenaArquivo.bat")
        val jarName = "ordenarArquivos-$version-jar-with-dependencies.jar"
        val content = "chcp 65001\r\njava -Dfile.encoding=utf-8 --add-opens java.base/java.lang.reflect=ALL-UNNAMED -jar $jarName\r\n"
        batFile.writeText(content, Charsets.UTF_8)
        mLOG.info("Arquivo .bat atualizado com versão: $version")
    }

    fun restartApplication() {
        val currentDir = File(System.getProperty("user.dir"))
        val batFile = File(currentDir, "ExecutarOrdenaArquivo.bat")

        if (!batFile.exists()) {
            mLOG.warn("ExecutarOrdenaArquivo.bat não encontrado para reinicialização.")
            return
        }

        val command = listOf("cmd", "/c", "start", "", batFile.absolutePath)

        mLOG.info("Reiniciando aplicativo: $command")
        try {
            ProcessBuilder(command)
                .directory(currentDir)
                .start()
            
            // Fecha a instância atual imediatamente
            System.exit(0)
        } catch (e: Exception) {
            mLOG.error("Erro ao tentar reiniciar o aplicativo", e)
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val normalize = { v: String -> v.replace("-SNAPSHOT", "").replace("-snapshot", "") }
        val parts1 = normalize(v1).split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = normalize(v2).split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }

        // SNAPSHOT is "lower" than release
        val isSnapshot1 = v1.contains("SNAPSHOT", ignoreCase = true)
        val isSnapshot2 = v2.contains("SNAPSHOT", ignoreCase = true)
        return when {
            isSnapshot1 && !isSnapshot2 -> -1
            !isSnapshot1 && isSnapshot2 -> 1
            else -> 0
        }
    }

    data class UpdateResult(
        val updated: Boolean,
        val previousVersion: String?,
        val newVersion: String,
        val message: String
    )
}
