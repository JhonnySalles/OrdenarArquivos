package com.fenix.ordenararquivos.webview

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.HttpCookie

class WebViewSessionManagerTest {

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        WebViewSessionManager.resetForTests()
        WebViewSessionManager.baseDirOverride = tempDir
    }

    @AfterEach
    fun tearDown() {
        WebViewSessionManager.resetForTests()
        CookieHandler.setDefault(null)
    }

    @Test
    fun testInicializarCriaDiretoriosDeProfile() {
        val profile = WebViewSessionManager.inicializar()
        assertTrue(profile.exists())
        assertTrue(profile.isDirectory)
        assertEquals(File(tempDir, "profile").absolutePath, profile.absolutePath)
        assertTrue(File(tempDir, "cookies").exists())
    }

    @Test
    fun testSalvarECarregarCookiesRoundTrip() {
        WebViewSessionManager.inicializar()

        val manager = CookieHandler.getDefault() as CookieManager
        val cookie = HttpCookie("session", "abc123").apply {
            domain = ".example.com"
            path = "/"
            maxAge = 3600
            secure = true
            isHttpOnly = true
        }
        manager.cookieStore.add(java.net.URI("https://example.com/"), cookie)

        WebViewSessionManager.salvarCookies()

        val novoStore = CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL).cookieStore
        WebViewSessionManager.carregarCookiesDoDisco(novoStore)

        val carregados = novoStore.cookies
        assertEquals(1, carregados.size)
        assertEquals("session", carregados[0].name)
        assertEquals("abc123", carregados[0].value)
        assertEquals(".example.com", carregados[0].domain)
        assertTrue(carregados[0].secure)
        assertTrue(carregados[0].isHttpOnly)
    }

    @Test
    fun testLimparSessaoRemoveCookiesEPasta() {
        WebViewSessionManager.inicializar()
        val manager = CookieHandler.getDefault() as CookieManager
        manager.cookieStore.add(
            java.net.URI("https://example.com/"),
            HttpCookie("x", "1")
        )
        WebViewSessionManager.salvarCookies()

        WebViewSessionManager.limparSessao()

        assertFalse(File(tempDir, "cookies/cookies.dat").exists())
        assertFalse(File(tempDir, "profile").exists())
    }
}
