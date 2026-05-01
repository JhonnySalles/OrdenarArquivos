package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.configuration.Configuracao
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.File
import java.io.FileInputStream

object GoogleDriveUploadService {

    private const val APPLICATION_NAME = "OrdenarArquivos-Uploader"
    private const val CLIENT_SECRETS_FILE = "secrets-drive.json"

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Uso: GoogleDriveUploader <arquivo1> <arquivo2> ...")
            return
        }

        try {
            println("Autenticando com Google Drive (OAuth2)...")
            val driveService = createDriveService()

            for (filePath in args) {
                uploadFile(driveService, filePath)
            }

            println("\nProcesso concluído com sucesso!")
        } catch (e: Exception) {
            println("\nERRO FATAL: ${e.message}")
            e.printStackTrace()
            System.exit(1)
        }
    }

    private fun createDriveService(): Drive {
        val jsonFactory = GsonFactory.getDefaultInstance()
        val clientSecrets = FileInputStream(CLIENT_SECRETS_FILE).use {
            GoogleClientSecrets.load(jsonFactory, it.bufferedReader())
        }

        val refreshToken = Configuracao.googleDriveRefreshToken
        if (refreshToken.isBlank()) {
            throw IllegalStateException("Refresh Token não configurado em secrets.properties (google_drive_refresh_token)")
        }

        // Criamos um JSON de credenciais de usuário em memória
        // Isso evita depender da classe UserRefreshCredentials diretamente na compilação
        val credentialsJson = """
            {
              "client_id": "${clientSecrets.details.clientId}",
              "client_secret": "${clientSecrets.details.clientSecret}",
              "refresh_token": "$refreshToken",
              "type": "authorized_user"
            }
        """.trimIndent()

        val credentials = GoogleCredentials.fromStream(credentialsJson.byteInputStream())
            .createScoped(listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE))

        return Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            HttpCredentialsAdapter(credentials)
        ).setApplicationName(APPLICATION_NAME).build()
    }

    private fun uploadFile(service: Drive, filePath: String) {
        val localFile = File(filePath)
        if (!localFile.exists()) {
            println("Aviso: Arquivo não encontrado: $filePath")
            return
        }

        val folderId = Configuracao.googleDriveFolderId
        if (folderId.isBlank()) {
            println("Erro: ID da pasta não configurado em secrets.properties (google_drive_folder_id)")
            return
        }

        // Listagem de depuração: O que o script consegue ver nesta pasta?
        println("Verificando arquivos visíveis na pasta $folderId...")
        val listResult = service.files().list()
            .setQ("'$folderId' in parents and trashed = false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val visibleFiles = listResult.files ?: emptyList()
        if (visibleFiles.isEmpty()) {
            println("Aviso: O script não consegue ver nenhum arquivo nesta pasta (escopo restrito).")
        } else {
            println("Arquivos detectados na pasta:")
            visibleFiles.forEach { println(" - [${it.id}] ${it.name}") }
        }

        // Busca o arquivo específico entre os visíveis
        val existingFile = visibleFiles.find { it.name == localFile.name }
        val mediaContent = FileContent("application/java-archive", localFile)

        if (existingFile != null) {
            println("Arquivo correspondente encontrado! Atualizando versão (ID: ${existingFile.id})...")
            service.files().update(existingFile.id, null, mediaContent)
                .setFields("id")
                .execute()
            println("Sucesso! Versão atualizada.")
        } else {
            println("Arquivo '${localFile.name}' não encontrado entre os arquivos visíveis. Criando novo...")
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = localFile.name
                parents = listOf(folderId)
            }
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            println("Sucesso! Novo arquivo criado (ID: ${uploadedFile.id}).")
        }
    }
}