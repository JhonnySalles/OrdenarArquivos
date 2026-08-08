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

class OcrSpaceOcrEngine : OcrEngineStrategy {

    private val mLog = LoggerFactory.getLogger(OcrSpaceOcrEngine::class.java)
    private val mClient = OkHttpClient().apply {
        setConnectTimeout(60, TimeUnit.SECONDS)
        setReadTimeout(60, TimeUnit.SECONDS)
        setWriteTimeout(60, TimeUnit.SECONDS)
    }

    override fun prepare(linguagem: Linguagem) {
        if (!isAvailable()) {
            throw OcrException("Chave da API OCR.space não configurada em secrets.properties.")
        }
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        return processOcrSpace(image, linguagem)
    }

    override fun clear() {}

    override fun isAvailable(): Boolean =
        Configuracao.ocrspaceApiKey.isNotEmpty()

    private fun processOcrSpace(imagem: File, linguagem: Linguagem): String {
        mLog.info("Preparando consulta ao OCR.space.")
        val ocrspaceLang = when (linguagem) {
            Linguagem.JAPANESE -> "jpn"
            else -> "eng"
        }

        val requestBody = MultipartBuilder()
            .type(MultipartBuilder.FORM)
            .addFormDataPart("apikey", Configuracao.ocrspaceApiKey)
            .addFormDataPart("language", ocrspaceLang)
            .addFormDataPart("isOverlayRequired", "false")
            .addFormDataPart("file", imagem.name, RequestBody.create(MediaType.parse("image/jpeg"), imagem))
            .build()

        val request = Request.Builder()
            .url("https://api.ocr.space/parse/image")
            .post(requestBody)
            .addHeader("apikey", Configuracao.ocrspaceApiKey)
            .build()

        mLog.info("Consultando OCR.space.")
        val response = mClient.newCall(request).execute()
        mLog.info("Resposta OCR.space: ${response.code()} - ${response.message()}")

        if (!response.isSuccessful || response.body() == null) {
            response.body()?.close()
            throw OcrException("Erro ao consultar o OCR.space: ${response.code()} - ${response.message()}")
        }

        return try {
            val responseBody = response.body().string()
            mLog.info("Resposta OCR.space: $responseBody")
            val jsonObject = JSONObject(responseBody)
            if (jsonObject.optBoolean("IsErroredOnProcessing", false)) {
                val errorMessage = jsonObject.optJSONArray("ErrorMessage")?.optString(0) ?: "Erro desconhecido"
                throw OcrException("Erro no processamento do OCR.space: $errorMessage")
            }
            val parsedResults = jsonObject.getJSONArray("ParsedResults")
            if (parsedResults.length() > 0) {
                parsedResults.getJSONObject(0).getString("ParsedText")
            } else {
                ""
            }
        } catch (e: JSONException) {
            mLog.error(e.message, e)
            throw OcrException("Erro ao processar resposta do OCR.space: ${e.message}")
        }
    }
}
