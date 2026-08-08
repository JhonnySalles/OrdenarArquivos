package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.squareup.okhttp.*
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class OptiicOcrEngine : OcrEngineStrategy {

    private val mLog = LoggerFactory.getLogger(OptiicOcrEngine::class.java)
    private val mClient = OkHttpClient().apply {
        setConnectTimeout(60, TimeUnit.SECONDS)
        setReadTimeout(60, TimeUnit.SECONDS)
        setWriteTimeout(60, TimeUnit.SECONDS)
    }

    override fun prepare(linguagem: Linguagem) {
        if (!isAvailable()) {
            throw OcrException("Chave da API Optiic não configurada em secrets.properties.")
        }
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        return processOptiic(image, linguagem)
    }

    override fun clear() {}

    override fun isAvailable(): Boolean =
        Configuracao.optiicApiKey.isNotEmpty()

    private fun processOptiic(imagem: File, linguagem: Linguagem): String {
        mLog.info("Preparando consulta ao Optiic.")
        val optiicLang = when (linguagem) {
            Linguagem.JAPANESE -> "ja"
            else -> "en"
        }

        val requestBody = MultipartBuilder()
            .type(MultipartBuilder.FORM)
            .addFormDataPart("apiKey", Configuracao.optiicApiKey)
            .addFormDataPart("lang", optiicLang)
            .addFormDataPart("image", imagem.name, RequestBody.create(MediaType.parse("image/jpeg"), imagem))
            .build()

        val request = Request.Builder()
            .url("https://api.optiic.dev/process")
            .post(requestBody)
            .addHeader("Authorization", "Bearer ${Configuracao.optiicApiKey}")
            .build()

        mLog.info("Consultando Optiic.")
        val response = mClient.newCall(request).execute()
        mLog.info("Resposta Optiic: ${response.code()} - ${response.message()}")

        if (!response.isSuccessful || response.body() == null) {
            response.body()?.close()
            throw OcrException("Erro ao consultar o Optiic: ${response.code()} - ${response.message()}")
        }

        return try {
            val responseBody = response.body().string()
            mLog.info("Resposta Optiic: $responseBody")
            val jsonObject = JSONObject(responseBody)
            if (jsonObject.has("text")) {
                jsonObject.getString("text")
            } else if (jsonObject.has("error")) {
                throw OcrException("Erro no processamento do Optiic: ${jsonObject.getString("error")}")
            } else {
                ""
            }
        } catch (e: JSONException) {
            mLog.error(e.message, e)
            throw OcrException("Erro ao processar resposta do Optiic: ${e.message}")
        }
    }
}
