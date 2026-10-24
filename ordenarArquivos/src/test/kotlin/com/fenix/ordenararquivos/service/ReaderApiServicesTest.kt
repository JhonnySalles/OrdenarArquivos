package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.configuration.Configuracao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.thread

class ReaderApiServicesTest {

    companion object {
        private var serverSocket: ServerSocket? = null
        private var running = true
        private var serverPort = 0
        val sampleComicId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

        @BeforeAll
        @JvmStatic
        fun setupServer() {
            serverSocket = ServerSocket(0)
            serverPort = serverSocket!!.localPort
            running = true

            thread(isDaemon = true) {
                while (running) {
                    try {
                        val client: Socket = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (_: Exception) {
                        break
                    }
                }
            }

            Configuracao.readerApiUrl = "http://127.0.0.1:$serverPort"
            Configuracao.readerApiUsername = "admin"
            Configuracao.readerApiPassword = "admin"
        }

        private fun handleClient(client: Socket) {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                val requestLine = reader.readLine() ?: return
                val headers = mutableMapOf<String, String>()
                var line = reader.readLine()
                while (!line.isNullOrBlank()) {
                    val idx = line.indexOf(":")
                    if (idx > 0) {
                        headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                    }
                    line = reader.readLine()
                }

                val out = client.getOutputStream()

                when {
                    requestLine.startsWith("GET /health") -> {
                        val body = """{"status":"UP","service":"ReaderServer API"}"""
                        sendResponse(out, 200, "application/json", body)
                    }
                    requestLine.startsWith("POST /auth/signin") -> {
                        val body = """{
                            "username": "admin",
                            "authenticated": true,
                            "accessToken": "mock-token-xyz",
                            "refreshToken": "mock-refresh-token",
                            "expiration": ${System.currentTimeMillis() + 3600000}
                        }"""
                        sendResponse(out, 200, "application/json", body)
                    }
                    requestLine.startsWith("GET /api/comicinfo/search/advanced") -> {
                        val auth = headers["authorization"]
                        if (auth != "Bearer mock-token-xyz") {
                            sendResponse(out, 401, "application/json", """{"error":"unauthorized"}""")
                            return
                        }
                        val body = """{
                            "_embedded": {
                                "comicInfoDtoList": [
                                    {
                                        "id": "11111111-1111-1111-1111-111111111111",
                                        "series": "One Piece",
                                        "title": "Romance Dawn",
                                        "volume": 1.0,
                                        "number": 1.0,
                                        "publisher": "Shueisha",
                                        "languageISO": "pt"
                                    },
                                    {
                                        "id": "22222222-2222-2222-2222-222222222222",
                                        "series": "One Piece",
                                        "title": "Wano Kuni",
                                        "volume": 100.0,
                                        "number": 1000.0,
                                        "publisher": "Shueisha",
                                        "languageISO": "pt"
                                    },
                                    {
                                        "id": "33333333-3333-3333-3333-333333333333",
                                        "series": "One Piece",
                                        "title": "Alabasta",
                                        "volume": 20.0,
                                        "number": 180.0,
                                        "publisher": "Shueisha",
                                        "languageISO": "pt"
                                    }
                                ]
                            },
                            "page": {
                                "size": 20,
                                "totalElements": 3,
                                "totalPages": 1,
                                "number": 0
                            }
                        }"""
                        sendResponse(out, 200, "application/json", body)
                    }
                    requestLine.startsWith("GET /api/comicinfo/$sampleComicId/download") -> {
                        val auth = headers["authorization"]
                        if (auth != "Bearer mock-token-xyz") {
                            sendResponse(out, 401, "application/xml", "<error/>")
                            return
                        }
                        val xml = """<?xml version="1.0" encoding="utf-8"?>
                        <ComicInfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                            <Title>Romance Dawn</Title>
                            <Series>One Piece</Series>
                            <Number>1</Number>
                            <Volume>1</Volume>
                            <Writer>Eiichiro Oda</Writer>
                            <Publisher>Shueisha</Publisher>
                            <Genre>Shounen</Genre>
                            <LanguageISO>pt</LanguageISO>
                            <Summary>Inicio da jornada de Luffy</Summary>
                        </ComicInfo>"""
                        sendResponse(out, 200, "application/xml;charset=UTF-8", xml)
                    }
                    else -> {
                        sendResponse(out, 404, "text/plain", "Not Found")
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    client.close()
                } catch (_: Exception) {}
            }
        }

        private fun sendResponse(out: java.io.OutputStream, status: Int, contentType: String, body: String) {
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            val header = "HTTP/1.1 $status OK\r\n" +
                    "Content-Type: $contentType\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray(StandardCharsets.UTF_8))
            out.write(bytes)
            out.flush()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            running = false
            Configuracao.readerApiUrl = ""
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
        }
    }

    @Test
    fun testConectarEAutenticar() {
        val conectado = ReaderApiServices.descobrirEConectar(forcar = true)
        assertTrue(conectado)
        assertEquals(ReaderApiServices.ApiStatus.CONECTADO, ReaderApiServices.status)
        assertEquals("http://127.0.0.1:$serverPort", ReaderApiServices.baseUrl)
    }

    @Test
    fun testPesquisarOrdenadoPorVolumeDecrescente() {
        ReaderApiServices.descobrirEConectar(forcar = true)
        val resultados = ReaderApiServices.pesquisar("One Piece")

        assertEquals(3, resultados.size)
        // Deve vir ordenado pelo volume mais recente para o menor: 100 -> 20 -> 1
        assertEquals(100.0f, resultados[0].volume)
        assertEquals("Wano Kuni", resultados[0].title)

        assertEquals(20.0f, resultados[1].volume)
        assertEquals("Alabasta", resultados[1].title)

        assertEquals(1.0f, resultados[2].volume)
        assertEquals("Romance Dawn", resultados[2].title)
    }

    @Test
    fun testBaixarComicInfoXml() {
        ReaderApiServices.descobrirEConectar(forcar = true)
        val comicInfo = ReaderApiServices.baixarComicInfo(sampleComicId)

        assertNotNull(comicInfo)
        assertEquals("Romance Dawn", comicInfo.title)
        assertEquals("One Piece", comicInfo.series)
        assertEquals(1, comicInfo.volume)
        assertEquals(1f, comicInfo.number)
        assertEquals("Eiichiro Oda", comicInfo.writer)
        assertEquals("Shueisha", comicInfo.publisher)
        assertEquals("Inicio da jornada de Luffy", comicInfo.summary)
    }
}
