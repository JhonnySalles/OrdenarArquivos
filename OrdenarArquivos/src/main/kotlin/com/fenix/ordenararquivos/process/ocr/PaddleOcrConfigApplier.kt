package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.configuration.Configuracao
import org.slf4j.LoggerFactory
import java.io.File

object PaddleOcrConfigApplier {

    private val mLog = LoggerFactory.getLogger(PaddleOcrConfigApplier::class.java)

    private val CONFIG_FILES = listOf("config_japan.txt", "config_en.txt", "config_latin.txt")

    private val MANAGED_KEYS = listOf("cls", "use_angle_cls", "limit_side_len")

    fun isInstalled(): Boolean = NativePaths.paddleExe.exists()

    fun readCurrentValues(): PaddleOcrSettings {
        val reference = NativePaths.paddleConfigFile("config_japan.txt")
        if (!reference.exists()) {
            return PaddleOcrSettings(
                cls = Configuracao.paddleCls,
                useAngleCls = Configuracao.paddleUseAngleCls,
                limitSideLen = Configuracao.paddleLimitSideLen
            )
        }
        val parsed = parseConfigFile(reference)
        return PaddleOcrSettings(
            cls = parsed["cls"]?.toBooleanStrictOrNull() ?: Configuracao.paddleCls,
            useAngleCls = parsed["use_angle_cls"]?.toBooleanStrictOrNull() ?: Configuracao.paddleUseAngleCls,
            limitSideLen = parsed["limit_side_len"]?.toIntOrNull() ?: Configuracao.paddleLimitSideLen
        )
    }

    fun applyFromConfiguracao() {
        val settings = PaddleOcrSettings(
            cls = Configuracao.paddleCls,
            useAngleCls = Configuracao.paddleUseAngleCls,
            limitSideLen = Configuracao.paddleLimitSideLen
        )
        applySettings(settings)
    }

    fun applySettings(settings: PaddleOcrSettings, modelsDir: File = File(NativePaths.paddleDir, "models")) {
        val values = mapOf(
            "cls" to settings.cls.toString(),
            "use_angle_cls" to settings.useAngleCls.toString(),
            "limit_side_len" to settings.limitSideLen.toString()
        )
        for (fileName in CONFIG_FILES) {
            val file = File(modelsDir, fileName)
            if (!file.exists()) {
                mLog.warn("Arquivo PaddleOCR não encontrado: ${file.absolutePath}")
                continue
            }
            writeConfigFile(file, values)
        }
    }

    internal fun parseConfigFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, String>()
        file.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            val idx = trimmed.indexOf('=')
            if (idx > 0) {
                result[trimmed.substring(0, idx).trim()] = trimmed.substring(idx + 1).trim()
            }
        }
        return result
    }

    internal fun writeConfigFile(file: File, values: Map<String, String>) {
        val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
        val presentKeys = mutableSetOf<String>()

        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val idx = trimmed.indexOf('=')
            if (idx <= 0) continue
            val key = trimmed.substring(0, idx).trim()
            if (key in values) {
                lines[i] = "$key=${values[key]}"
                presentKeys.add(key)
            }
        }

        for (key in MANAGED_KEYS) {
            if (key !in presentKeys && key in values) {
                lines.add("$key=${values[key]}")
            }
        }

        file.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "")
    }
}
