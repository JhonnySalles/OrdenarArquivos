package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

class OpenRouterOcrEngine : OcrEngineStrategy {

    private val mLog = LoggerFactory.getLogger(OpenRouterOcrEngine::class.java)
    private val mClient = OkHttpClient().apply {
        setConnectTimeout(60, TimeUnit.SECONDS)
        setReadTimeout(60, TimeUnit.SECONDS)
        setWriteTimeout(60, TimeUnit.SECONDS)
    }

    private var mPrompt = ""

    override fun prepare(linguagem: Linguagem) {
        if (!isAvailable()) {
            throw OcrException("Chave da API OpenRouter não configurada em secrets.properties.")
        }
        val model = Configuracao.openrouterModel
        if (model.isEmpty()) {
            throw OcrException("Modelo do OpenRouter não configurado nas propriedades.")
        }

        mLog.info("Validando se o modelo $model ainda continua gratuito no OpenRouter.")
        try {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .get()
                .addHeader("Authorization", "Bearer ${Configuracao.openrouterApiKey}")
                .build()

            val response = mClient.newCall(request).execute()
            if (!response.isSuccessful || response.body() == null) {
                response.body()?.close()
                throw OcrException("Falha ao obter os modelos do OpenRouter: ${response.code()} - ${response.message()}")
            }

            val bodyString = response.body().string()
            val json = JSONObject(bodyString)
            val dataArray = json.getJSONArray("data")
            var modelFound = false
            var isFree = false

            for (i in 0 until dataArray.length()) {
                val modelObj = dataArray.getJSONObject(i)
                if (modelObj.getString("id") == model) {
                    modelFound = true
                    val pricing = modelObj.optJSONObject("pricing")
                    if (pricing != null) {
                        val promptCost = pricing.optString("prompt", "0").toDoubleOrNull() ?: 0.0
                        val completionCost = pricing.optString("completion", "0").toDoubleOrNull() ?: 0.0
                        if (promptCost == 0.0 && completionCost == 0.0) {
                            isFree = true
                        }
                    }
                    break
                }
            }

            if (!modelFound) {
                throw OcrException("O modelo $model não foi encontrado no OpenRouter.")
            }
            if (!isFree) {
                throw OcrException("O modelo $model não é gratuito no OpenRouter.")
            }
            mLog.info("Modelo $model validado com sucesso e está gratuito.")

        } catch (e: OcrException) {
            throw e
        } catch (e: Exception) {
            mLog.error("Erro de conexão ao validar modelo no OpenRouter: ${e.message}", e)
            throw OcrException("Não foi possível validar o modelo no OpenRouter por falta de conexão: ${e.message}")
        }
    }

    fun setPrompt(prompt: String) {
        mPrompt = prompt
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        return processOpenRouter(image)
    }

    override fun clear() {}

    override fun isAvailable(): Boolean =
        Configuracao.openrouterApiKey.isNotEmpty()

    private fun converteToBase64(imagem: File): String =
        Base64.getEncoder().encodeToString(imagem.readBytes())

    private fun mimeType(imagem: File): String =
        Files.probeContentType(imagem.toPath()) ?: "image/jpg"

    private fun processOpenRouter(imagem: File): String {
        mLog.info("Preparando consulta ao OpenRouter.")
        val base64 = converteToBase64(imagem)
        val mime = mimeType(imagem)

        val requestBodyJson = JSONObject().apply {
            put("model", Configuracao.openrouterModel)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", mPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:$mime;base64,$base64")
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(RequestBody.create(MediaType.parse("application/json"), requestBodyJson.toString()))
            .addHeader("Authorization", "Bearer ${Configuracao.openrouterApiKey}")
            .addHeader("Content-Type", "application/json")
            .build()

        mLog.info("Consultando OpenRouter.")
        val response = mClient.newCall(request).execute()
        mLog.info("Resposta OpenRouter: ${response.code()} - ${response.message()}")

        if (!response.isSuccessful || response.body() == null) {
            response.body()?.close()
            throw OcrException("Erro ao consultar o OpenRouter: ${response.code()} - ${response.message()}")
        }

        return try {
            val responseBody = response.body()!!.string()
            mLog.info("Resposta OpenRouter: $responseBody")
            val jsonObject = JSONObject(responseBody)
            jsonObject.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .replace("'", "")
        } catch (e: JSONException) {
            mLog.error(e.message, e)
            throw OcrException("Erro ao processar resposta do OpenRouter: ${e.message}")
        }
    }
}
