package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.entities.api.ApiComicInfoResumo
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import jakarta.xml.bind.JAXBContext
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

object ReaderApiServices {

    private val mLOG: Logger = LoggerFactory.getLogger(ReaderApiServices::class.java)

    enum class ApiStatus {
        DESCONECTADO,
        PROCURANDO,
        CONECTADO,
        ERRO_AUTENTICACAO,
        INDISPONIVEL
    }

    private val statusListeners = CopyOnWriteArrayList<(ApiStatus, String?) -> Unit>()

    var status: ApiStatus = ApiStatus.DESCONECTADO
        private set(value) {
            field = value
            notificarListeners()
        }

    var baseUrl: String? = null
        private set

    var serverVersion: String? = null
        private set

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiration: Date? = null

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun addStatusListener(listener: (ApiStatus, String?) -> Unit) {
        statusListeners.add(listener)
        listener(status, baseUrl)
    }

    fun removeStatusListener(listener: (ApiStatus, String?) -> Unit) {
        statusListeners.remove(listener)
    }

    private fun notificarListeners() {
        val currentStatus = status
        val currentUrl = baseUrl
        for (listener in statusListeners) {
            try {
                listener(currentStatus, currentUrl)
            } catch (e: Exception) {
                mLOG.error("Erro no listener de status da API", e)
            }
        }
    }

    @Synchronized
    fun descobrirEConectar(forcar: Boolean = false): Boolean {
        if (!forcar && status == ApiStatus.CONECTADO && baseUrl != null && isTokenValido()) {
            return true
        }

        status = ApiStatus.PROCURANDO
        mLOG.info("Iniciando busca da ReaderServer API na rede local...")

        var urlEncontrada: String? = null

        // 1. Se configurado manualmente no app.properties, tentar primeiro
        if (Configuracao.readerApiUrl.isNotBlank()) {
            val configUrl = Configuracao.readerApiUrl.trim().removeSuffix("/")
            if (testarHealthCheck(configUrl)) {
                urlEncontrada = configUrl
                mLOG.info("API conectada via configuração manual: $urlEncontrada")
            }
        }

        // 2. Tentar descoberta via mDNS (JmDNS)
        if (urlEncontrada == null) {
            try {
                urlEncontrada = descobrirViaMdns()
            } catch (e: Exception) {
                mLOG.warn("Falha ao pesquisar mDNS: ${e.message}")
            }
        }

        // 3. Fallback: testar localhost:8083
        if (urlEncontrada == null) {
            val localUrl = "http://localhost:8083"
            if (testarHealthCheck(localUrl)) {
                urlEncontrada = localUrl
                mLOG.info("API conectada via fallback localhost: $urlEncontrada")
            }
        }

        if (urlEncontrada == null) {
            mLOG.warn("Não foi possível localizar a ReaderServer API na rede.")
            baseUrl = null
            serverVersion = null
            status = ApiStatus.INDISPONIVEL
            return false
        }

        baseUrl = urlEncontrada

        // 4. Autenticar na API
        val autenticado = autenticar()
        if (autenticado) {
            status = ApiStatus.CONECTADO
            mLOG.info("ReaderServer API conectada e autenticada com sucesso em $baseUrl (versão: ${serverVersion ?: "desconhecida"})")
            return true
        } else {
            status = ApiStatus.ERRO_AUTENTICACAO
            mLOG.warn("ReaderServer API localizada em $baseUrl, mas falhou ao autenticar.")
            return false
        }
    }

    private fun descobrirViaMdns(): String? {
        val timeoutMs = Configuracao.readerApiTimeout.toLong().coerceIn(1000L, 10000L)
        val interfaces = NetworkInterface.getNetworkInterfaces()
        val serviceTypes = listOf("_readerserver._tcp.local.", "_http._tcp.local.")

        while (interfaces.hasMoreElements()) {
            val netIf = interfaces.nextElement()
            if (!netIf.isUp || netIf.isLoopback || netIf.isVirtual) continue

            val addresses = netIf.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is java.net.Inet4Address) {
                    var jmdns: JmDNS? = null
                    try {
                        jmdns = JmDNS.create(addr)
                        for (serviceType in serviceTypes) {
                            val services: Array<ServiceInfo> = jmdns.list(serviceType, timeoutMs)
                            for (info in services) {
                                val isReader = serviceType == "_readerserver._tcp.local." ||
                                    info.name.contains("ReaderServer", ignoreCase = true) ||
                                    (info.application != null && info.application.contains("ReaderServer", ignoreCase = true))

                                if (isReader) {
                                    val port = info.port
                                    val host = info.hostAddresses.firstOrNull() ?: addr.hostAddress
                                    val candidateUrl = "http://$host:$port"
                                    val versaoTxt = info.getPropertyString("version")
                                    if (!versaoTxt.isNullOrBlank()) {
                                        serverVersion = versaoTxt
                                    }
                                    if (testarHealthCheck(candidateUrl)) {
                                        mLOG.info("Serviço mDNS encontrado: ${info.name} em $candidateUrl (versão: $serverVersion)")
                                        return candidateUrl
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        mLOG.debug("mDNS probe falhou em ${addr.hostAddress}: ${e.message}")
                    } finally {
                        try {
                            jmdns?.close()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return null
    }

    private fun testarHealthCheck(url: String): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$url/health"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                val json = JSONObject(response.body() as String)
                val isUp = json.optString("status").equals("UP", ignoreCase = true)
                if (isUp) {
                    val ver = json.optString("version")
                    if (ver.isNotBlank()) {
                        serverVersion = ver
                    }
                }
                isUp
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    private fun autenticar(): Boolean {
        val url = baseUrl ?: return false
        val username = Configuracao.readerApiUsername.trim()
        val password = Configuracao.readerApiPassword.trim()

        if (username.isBlank() || password.isBlank()) {
            mLOG.warn("Credenciais da ReaderServer API não configuradas em secrets.properties.")
            return false
        }

        try {
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$url/auth/signin"))
                .header("Content-Type", "application/json; charset=utf-8")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString(), StandardCharsets.UTF_8))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                mLOG.error("Falha ao autenticar na API: HTTP ${response.statusCode()} - ${response.body()}")
                return false
            }

            val body = response.body() ?: return false
            val json = JSONObject(body as String)
            accessToken = json.optString("accessToken")
            refreshToken = json.optString("refreshToken")
            val exp = json.optLong("expiration", 0L)
            tokenExpiration = if (exp > 0L) Date(exp) else Date(System.currentTimeMillis() + 3600000L)
            return !accessToken.isNullOrBlank()
        } catch (e: Exception) {
            mLOG.error("Exceção ao autenticar na ReaderServer API", e)
            return false
        }
    }

    private fun isTokenValido(): Boolean {
        if (accessToken.isNullOrBlank()) return false
        val exp = tokenExpiration ?: return true
        return exp.time > (System.currentTimeMillis() + 60000L)
    }

    private fun garantirAutenticacao(): Boolean {
        if (isTokenValido()) return true

        val currentRefresh = refreshToken
        val username = Configuracao.readerApiUsername.trim()
        val url = baseUrl

        if (!currentRefresh.isNullOrBlank() && username.isNotBlank() && url != null) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$url/auth/refresh/$username"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", currentRefresh)
                    .timeout(Duration.ofSeconds(10))
                    .PUT(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..299) {
                    val body = response.body()
                    if (body != null) {
                        val json = JSONObject(body as String)
                        accessToken = json.optString("accessToken")
                        refreshToken = json.optString("refreshToken")
                        val exp = json.optLong("expiration", 0L)
                        tokenExpiration = if (exp > 0L) Date(exp) else Date(System.currentTimeMillis() + 3600000L)
                        return true
                    }
                }
            } catch (e: Exception) {
                mLOG.warn("Falha no refresh token da API, tentando novo login: ${e.message}")
            }
        }

        return autenticar()
    }

    fun pesquisar(
        query: String,
        maxResults: Int = 100,
        sortField: String = "volume",
        direction: String = "desc"
    ): List<ApiComicInfoResumo> {
        val url = baseUrl ?: throw IllegalStateException("ReaderServer API não conectada.")
        if (!garantirAutenticacao()) {
            throw IllegalStateException("Falha na autenticação com a ReaderServer API.")
        }

        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        val searchUrl = "$url/api/comicinfo/search/advanced?query=$encodedQuery&page=0&size=$maxResults&sortField=$sortField&direction=$direction"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .header("Authorization", "Bearer $accessToken")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            if (autenticar()) {
                return pesquisar(query, maxResults, sortField, direction)
            }
            throw IllegalStateException("Acesso não autorizado na ReaderServer API (401/403).")
        }

        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Erro ao pesquisar na API: HTTP ${response.statusCode()}")
        }

        val body = response.body() ?: return emptyList()
        val json = JSONObject(body as String)

        val embedded = json.optJSONObject("_embedded") ?: return emptyList()
        val listKey = embedded.keys().asSequence().firstOrNull() ?: return emptyList()
        val itemsArray = embedded.optJSONArray(listKey) ?: return emptyList()

        val resultado = mutableListOf<ApiComicInfoResumo>()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)
            val idStr = item.optString("id")
            if (idStr.isBlank()) continue

            val id = try {
                UUID.fromString(idStr)
            } catch (_: Exception) {
                continue
            }

            val series = item.optString("series", "")
            val title = item.optString("title", "")
            val volume = if (item.has("volume") && !item.isNull("volume")) item.getDouble("volume").toFloat() else null
            val number = if (item.has("number") && !item.isNull("number")) item.getDouble("number").toFloat() else 0f
            val languageISO = item.optString("languageISO", null)
            val publisher = item.optString("publisher", null)
            val year = if (item.has("year") && !item.isNull("year")) item.getInt("year") else null
            val format = item.optString("format", null)

            resultado.add(
                ApiComicInfoResumo(
                    id = id,
                    series = series,
                    title = title,
                    volume = volume,
                    number = number,
                    languageISO = languageISO,
                    publisher = publisher,
                    year = year,
                    format = format
                )
            )
        }

        return resultado.sortedWith(
            compareByDescending<ApiComicInfoResumo> { it.volume ?: -1f }
                .thenByDescending { it.number }
        )
    }

    fun baixarComicInfo(id: UUID): ComicInfo {
        val url = baseUrl ?: throw IllegalStateException("ReaderServer API não conectada.")
        if (!garantirAutenticacao()) {
            throw IllegalStateException("Falha na autenticação com a ReaderServer API.")
        }

        val downloadUrl = "$url/api/comicinfo/$id/download"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .header("Authorization", "Bearer $accessToken")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            if (autenticar()) {
                return baixarComicInfo(id)
            }
            throw IllegalStateException("Acesso não autorizado ao baixar ComicInfo (401/403).")
        }

        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Erro ao baixar ComicInfo: HTTP ${response.statusCode()}")
        }

        val xmlContent = response.body()
            ?: throw IllegalStateException("Conteúdo XML vazio recebido da API.")

        val jaxb = JAXBContext.newInstance(ComicInfo::class.java)
        val unmarshaller = jaxb.createUnmarshaller()
        return unmarshaller.unmarshal(StringReader(xmlContent as String)) as ComicInfo
    }
}
