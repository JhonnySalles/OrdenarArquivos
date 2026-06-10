package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

class GeminiOcrEngine : OcrEngineStrategy {

    private val mLog = LoggerFactory.getLogger(GeminiOcrEngine::class.java)
    private val mClient = OkHttpClient().apply {
        setConnectTimeout(60, TimeUnit.SECONDS)
        setReadTimeout(60, TimeUnit.SECONDS)
        setWriteTimeout(60, TimeUnit.SECONDS)
    }

    private var mGeminiKey = Configuracao.geminiKey1
    private var mIsFirstKey = true
    private var mPrompt = ""

    override fun prepare(linguagem: Linguagem) {
        if (!isAvailable()) {
            throw OcrException("Chave da API Gemini não configurada em secrets.properties.")
        }
        mGeminiKey = Configuracao.geminiKey1
        mIsFirstKey = true
    }

    fun setPrompt(prompt: String) {
        mPrompt = prompt
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        return processGemini(image, mPrompt)
    }

    override fun clear() {}

    override fun isAvailable(): Boolean =
        Configuracao.geminiKey1.isNotEmpty() || Configuracao.geminiKey2.isNotEmpty()

    private fun converteToBase64(imagem: File): String =
        Base64.getEncoder().encodeToString(imagem.readBytes())

    private fun mimeType(imagem: File): String =
        Files.probeContentType(imagem.toPath()) ?: "image/jpg"

    private fun processGemini(imagem: File, texto: String): String {
        mLog.info("Preparando consulta ao Gemini.")
        val mediaType = MediaType.parse("application/json")
        val base64 = converteToBase64(imagem)
        val mime = mimeType(imagem)
        val escapedText = texto.replace("\"", "\\\"")
        val body = RequestBody.create(
            mediaType,
            "{\"contents\":[{\"parts\":[{\"text\":\"$escapedText\"},{\"inline_data\":{\"mime_type\":\"$mime\",\"data\":\"$base64\"}}]}]}"
        )

        val url = "$URL_GEMINI${Configuracao.geminiModel}:generateContent?key=$mGeminiKey"
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        mLog.info("Consultando Gemini.")
        val response = mClient.newCall(request).execute()
        mLog.info("Resposta Gemini: ${response.code()} - ${response.message()}")

        if (response.code() == 429 && mIsFirstKey && Configuracao.geminiKey2.isNotEmpty()) {
            response.body()?.close()
            mGeminiKey = Configuracao.geminiKey2
            mIsFirstKey = false
            return processGemini(imagem, texto)
        }

        if (response.code() > 299 || response.body() == null) {
            response.body()?.close()
            throw OcrException("Erro ao consultar o Gemini: ${response.code()} - ${response.message()}")
        }

        return try {
            val responseBody = response.body()!!.string()
            mLog.info("Resposta Gemini: $responseBody")
            val jsonObject = JSONObject(responseBody)
            jsonObject.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .replace("'", "")
        } catch (e: JSONException) {
            mLog.error(e.message, e)
            throw OcrException("Erro ao processar resposta do Gemini: ${e.message}")
        }
    }

    companion object {
        private const val URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta/models/"
    }
}
