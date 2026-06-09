package com.fenix.ordenararquivos.webview

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.nio.charset.StandardCharsets

object WebViewSessionManager {

    private val mLOG: Logger = LoggerFactory.getLogger(WebViewSessionManager::class.java)

    private const val COOKIE_FIELD_SEP = "\t"
    private const val COOKIE_VERSION = 1

    @Volatile
    private var inicializado = false

    private lateinit var profileDir: File
    private lateinit var cookiesFile: File
    private var cookieManager: CookieManager? = null

    /** Sobrescreve o diretório base (apenas testes). */
    @JvmField
    internal var baseDirOverride: File? = null

    internal fun resetForTests() {
        inicializado = false
        cookieManager = null
        baseDirOverride = null
    }

    @Synchronized
    fun inicializar(): File {
        if (inicializado)
            return profileDir

        val baseDir = baseDirOverride ?: File(System.getProperty("user.home"), ".ordenararquivos/webview")
        profileDir = File(baseDir, "profile")
        cookiesFile = File(File(baseDir, "cookies"), "cookies.dat")

        profileDir.mkdirs()
        cookiesFile.parentFile?.mkdirs()

        val manager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        carregarCookiesDoDisco(manager.cookieStore)
        CookieHandler.setDefault(manager)
        cookieManager = manager
        inicializado = true

        mLOG.info("Sessão WebView inicializada: profile={}, cookies={}", profileDir.absolutePath, cookiesFile.absolutePath)
        return profileDir
    }

    fun obterProfileDir(): File {
        if (!inicializado)
            inicializar()
        return profileDir
    }

    @Synchronized
    fun salvarCookies() {
        val manager = cookieManager ?: return
        try {
            cookiesFile.parentFile?.mkdirs()
            BufferedWriter(OutputStreamWriter(FileOutputStream(cookiesFile), StandardCharsets.UTF_8)).use { writer ->
                writer.write("$COOKIE_VERSION\n")
                for (cookie in manager.cookieStore.cookies) {
                    writer.write(serializeCookie(cookie))
                    writer.newLine()
                }
            }
            mLOG.debug("Cookies WebView salvos: {}", cookiesFile.absolutePath)
        } catch (e: Exception) {
            mLOG.warn("Falha ao salvar cookies WebView: {}", e.message, e)
        }
    }

    @Synchronized
    fun limparSessao() {
        cookieManager?.cookieStore?.removeAll()
        if (::cookiesFile.isInitialized && cookiesFile.exists())
            cookiesFile.delete()
        if (::profileDir.isInitialized && profileDir.exists())
            profileDir.deleteRecursively()
        mLOG.info("Sessão WebView limpa")
    }

    internal fun carregarCookiesDoDisco(store: CookieStore) {
        if (!::cookiesFile.isInitialized || !cookiesFile.exists())
            return

        try {
            BufferedReader(InputStreamReader(FileInputStream(cookiesFile), StandardCharsets.UTF_8)).use { reader ->
                val versionLine = reader.readLine() ?: return
                if (versionLine.toIntOrNull() != COOKIE_VERSION) {
                    mLOG.warn("Formato de cookies desconhecido (versão {}), ignorando arquivo", versionLine)
                    return
                }
                reader.lineSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { deserializeCookie(it) }
                    .forEach { (uri, cookie) ->
                        try {
                            store.add(uri, cookie)
                        } catch (e: Exception) {
                            mLOG.debug("Cookie ignorado ({}): {}", cookie.name, e.message)
                        }
                    }
            }
            mLOG.debug("Cookies WebView carregados de {}", cookiesFile.absolutePath)
        } catch (e: Exception) {
            mLOG.warn("Falha ao carregar cookies WebView: {}", e.message, e)
        }
    }

    private fun serializeCookie(cookie: HttpCookie): String {
        return listOf(
            cookie.domain ?: "",
            cookie.path ?: "/",
            cookie.name,
            cookie.value,
            cookie.maxAge.toString(),
            cookie.secure.toString(),
            cookie.isHttpOnly.toString()
        ).joinToString(COOKIE_FIELD_SEP) { escapeField(it) }
    }

    private fun deserializeCookie(line: String): Pair<URI, HttpCookie>? {
        val parts = splitEscapedFields(line)
        if (parts.size < 7)
            return null

        val domain = parts[0].ifBlank { return null }
        val path = parts[1].ifBlank { "/" }
        val name = parts[2]
        val value = parts[3]
        val maxAge = parts[4].toLongOrNull() ?: -1L
        val secure = parts[5].toBooleanStrictOrNull() ?: false
        val httpOnly = parts[6].toBooleanStrictOrNull() ?: false

        val cookie = HttpCookie(name, value).apply {
            this.domain = domain
            this.path = path
            this.maxAge = maxAge
            this.secure = secure
            isHttpOnly = httpOnly
        }

        val host = domain.removePrefix(".")
        val scheme = if (secure) "https" else "http"
        val uri = URI("$scheme://$host$path")
        return uri to cookie
    }

    private fun escapeField(value: String): String =
        value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

    private fun unescapeField(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    't' -> sb.append('\t')
                    'n' -> sb.append('\n')
                    '\\' -> sb.append('\\')
                    else -> sb.append(value[i + 1])
                }
                i += 2
            } else {
                sb.append(value[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun splitEscapedFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            if (line[i] == '\\' && i + 1 < line.length) {
                current.append(line[i])
                current.append(line[i + 1])
                i += 2
            } else if (line[i] == '\t') {
                fields.add(current.toString())
                current.clear()
                i++
            } else {
                current.append(line[i])
                i++
            }
        }
        fields.add(current.toString())
        return fields.map { unescapeField(it) }
    }
}
