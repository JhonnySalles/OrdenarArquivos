package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.Linguagem
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets

class PaddleOcrJsonEngine : OcrEngineStrategy {

    private val mLog = LoggerFactory.getLogger(PaddleOcrJsonEngine::class.java)
    private var configPath = ""

    override fun prepare(linguagem: Linguagem) {
        if (!isAvailable()) {
            throw OcrException(
                "PaddleOCR-json não encontrado em ${NativePaths.paddleExe.absolutePath}. " +
                    NativePaths.paddleOcrInstallHint()
            )
        }
        configPath = resolveConfigPath(linguagem)
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        if (linguagem == Linguagem.JAPANESE) {
            val results = mutableListOf<String>()
            results.add(runOcr(image))
            val rotated90 = rotateImage(image, Core.ROTATE_90_CLOCKWISE)
            try {
                results.add(runOcr(rotated90))
            } finally {
                rotated90.delete()
            }
            val rotated270 = rotateImage(image, Core.ROTATE_90_COUNTERCLOCKWISE)
            try {
                val r270 = runOcr(rotated270)
                if (OcrTextNormalizer.normalize(r270).size >
                    OcrTextNormalizer.normalize(results.lastOrNull() ?: "").size
                ) {
                    results.add(r270)
                }
            } finally {
                rotated270.delete()
            }
            return JapaneseLayoutHelper.pickBestResult(results, "-", "|")
        }
        return runOcr(image)
    }

    override fun clear() {}

    override fun isAvailable(): Boolean = NativePaths.paddleExe.exists()

    private fun runOcr(image: File): String {
        val args = mutableListOf(
            NativePaths.paddleExe.absolutePath,
            "-image_path=${image.absolutePath}"
        )
        if (configPath.isNotEmpty()) {
            args.add(1, "-config_path=$configPath")
        }

        val process = ProcessBuilder(args)
            .directory(NativePaths.paddleDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            mLog.warn("PaddleOCR-json exit code $exitCode: $output")
        }

        return parseJsonOutput(output, JapaneseLayoutHelper.detectOrientation(emptyList()))
    }

    internal fun parseJsonOutput(json: String, orientation: TextOrientation): String {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return ""

        return try {
            val root = JSONObject(trimmed)
            if (root.optInt("code", -1) != 100 && root.optInt("code", -1) != 0 && !root.has("data")) {
                mLog.warn("PaddleOCR-json resposta inesperada: $trimmed")
                return ""
            }
            val data = root.optJSONArray("data") ?: return ""
            blocksToText(data, orientation)
        } catch (e: Exception) {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) {
                try {
                    val root = JSONObject(trimmed.substring(start, end + 1))
                    val data = root.optJSONArray("data") ?: return trimmed
                    blocksToText(data, orientation)
                } catch (ex: Exception) {
                    mLog.error("Erro ao parsear JSON do PaddleOCR: ${ex.message}")
                    ""
                }
            } else {
                mLog.error("Erro ao parsear JSON do PaddleOCR: ${e.message}")
                ""
            }
        }
    }

    private fun blocksToText(data: JSONArray, orientation: TextOrientation): String {
        val blocks = mutableListOf<OcrTextBlock>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val text = item.optString("text", "").trim()
            if (text.isEmpty()) continue
            val box = item.optJSONArray("box")
            if (box != null && box.length() >= 4) {
                val xs = (0 until box.length()).mapNotNull { box.optJSONArray(it)?.optDouble(0) }
                val ys = (0 until box.length()).mapNotNull { box.optJSONArray(it)?.optDouble(1) }
                if (xs.isNotEmpty() && ys.isNotEmpty()) {
                    val minX = xs.min().toInt()
                    val minY = ys.min().toInt()
                    val maxX = xs.max().toInt()
                    val maxY = ys.max().toInt()
                    blocks.add(OcrTextBlock(text, minX, minY, maxX - minX, maxY - minY))
                    continue
                }
            }
            blocks.add(OcrTextBlock(text, 0, i * 20, 100, 20))
        }

        val resolvedOrientation = if (orientation == TextOrientation.UNCERTAIN) {
            JapaneseLayoutHelper.detectOrientation(blocks)
        } else orientation

        return JapaneseLayoutHelper.sortBlocks(blocks, resolvedOrientation)
            .joinToString("\n") { it.text }
    }

    private fun resolveConfigPath(linguagem: Linguagem): String {
        val configName = when (linguagem) {
            Linguagem.JAPANESE -> "config_japan.txt"
            Linguagem.ENGLISH -> "config_en.txt"
            Linguagem.PORTUGUESE -> "config_latin.txt"
            else -> "config_en.txt"
        }
        val configFile = NativePaths.paddleConfigFile(configName)
        return if (configFile.exists()) configFile.absolutePath else ""
    }

    private fun rotateImage(source: File, rotationCode: Int): File {
        val mat = Imgcodecs.imread(source.absolutePath)
        if (mat.empty()) return source
        val rotated = Mat()
        Core.rotate(mat, rotated, rotationCode)
        val out = File(source.parentFile, "paddle_rot_${System.nanoTime()}.jpg")
        Imgcodecs.imwrite(out.absolutePath, rotated)
        mat.release()
        rotated.release()
        return out
    }

}
