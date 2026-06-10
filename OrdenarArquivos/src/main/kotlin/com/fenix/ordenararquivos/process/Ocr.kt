package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.OcrEngine
import com.fenix.ordenararquivos.process.ocr.GeminiOcrEngine
import com.fenix.ordenararquivos.process.ocr.OcrEngineFactory
import com.fenix.ordenararquivos.process.ocr.OcrPrompts
import com.fenix.ordenararquivos.process.ocr.OcrTextNormalizer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

object Ocr {

    internal val OCR_CHAPTER_REGEX = OcrTextNormalizer.OCR_CHAPTER_REGEX

    private val mLog = LoggerFactory.getLogger(Ocr::class.java)

    var isTeste: Boolean = false
    var testSuggestion: String = "001-05 Suggestion"

    var mLibs = false
        private set

    /** @deprecated Use Configuracao.ocrEngine. Mantido para compatibilidade com testes legados. */
    var mGemini = false
        private set

    private var activeStrategy = OcrEngineFactory.resolve()
    private var currentPrompt = ""

    init {
        mLibs = try {
            loadLibraries()
            true
        } catch (e: Exception) {
            mLog.error("Erro ao carregar as libs do Opencv", e)
            false
        } catch (e: Error) {
            mLog.error("Erro ao carregar as libs do Opencv", e)
            false
        }
        refreshGeminiFlag()
    }

    @JvmStatic
    fun refreshConfiguration() {
        refreshGeminiFlag()
        activeStrategy = OcrEngineFactory.resolve()
        currentPrompt = ""
    }

    private fun refreshGeminiFlag() {
        mGemini = Configuracao.ocrEngine == OcrEngine.GEMINI &&
            (Configuracao.geminiKey1.isNotEmpty() || Configuracao.geminiKey2.isNotEmpty())
    }

    private fun loadLibraries() {
        val path = File(Paths.get("").toAbsolutePath().toString() + "/natives/").path
        val osName = System.getProperty("os.name").lowercase()
        val bit = System.getProperty("sun.arch.data.model")?.toIntOrNull() ?: 32
        if (osName.contains("win")) {
            when (bit) {
                32 -> {
                    System.load(Paths.get(path, "opencv_320_32.dll").toString())
                    System.load(Paths.get(path, "opencv_ffmpeg320_32.dll").toString())
                    System.load(Paths.get(path, "openh264-1.6.0-win32msvc.dll").toString())
                }
                64 -> {
                    System.load(Paths.get(path, "opencv_java320_64.dll").toString())
                    System.load(Paths.get(path, "opencv_ffmpeg320_64.dll").toString())
                    System.load(Paths.get(path, "openh264-1.6.0-win64msvc.dll").toString())
                }
                else -> {
                    System.load(Paths.get(path, "opencv_java320_32.dll").toString())
                    System.load(Paths.get(path, "openh264-1.6.0-win32msvc.dll").toString())
                }
            }
        } else if (osName.contains("mac")) {
            mLog.info("This version os the application cannot run on MAC OS yet.")
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            if (bit == 64) {
                System.load(Paths.get(path, "libopencv_320_64.so").toString())
                System.load(Paths.get(path, "libopenh264-1.6.0-linux64.3.so").toString())
            }
        }
    }

    @JvmStatic
    fun prepare(isJapanese: Boolean) {
        prepare(if (isJapanese) Linguagem.JAPANESE else Linguagem.ENGLISH)
    }

    @JvmStatic
    fun prepare(linguagem: Linguagem) {
        refreshGeminiFlag()
        activeStrategy = OcrEngineFactory.resolve()
        OcrEngineFactory.validateAvailable(Configuracao.ocrEngine)
        activeStrategy.prepare(linguagem)
        if (activeStrategy is GeminiOcrEngine) {
            (activeStrategy as GeminiOcrEngine).setPrompt(currentPrompt)
        }
    }

    @JvmStatic
    fun clear() {
        activeStrategy.clear()
    }

    @JvmStatic
    @JvmOverloads
    fun process(
        image: File,
        separadorPagina: String,
        separadorCapitulo: String,
        linguagem: Linguagem = Linguagem.JAPANESE
    ): String {
        if (!image.exists() || image.length() == 0L) return ""
        if (isTeste) return testSuggestion

        activeStrategy = OcrEngineFactory.resolve()
        if (Configuracao.ocrEngine == OcrEngine.GEMINI) {
            currentPrompt = OcrPrompts.geraPromptSumario(separadorPagina, separadorCapitulo)
            (activeStrategy as GeminiOcrEngine).setPrompt(currentPrompt)
        }

        val rawText = activeStrategy.recognize(image, linguagem)
        return OcrTextNormalizer.normalizeToString(rawText, separadorPagina, separadorCapitulo)
    }

    internal fun ocrToCapitulo(
        textos: String,
        separadorPagina: String = "-",
        separadorCapitulo: String = "|"
    ): String = OcrTextNormalizer.ocrToCapitulo(textos, separadorPagina, separadorCapitulo)
}
