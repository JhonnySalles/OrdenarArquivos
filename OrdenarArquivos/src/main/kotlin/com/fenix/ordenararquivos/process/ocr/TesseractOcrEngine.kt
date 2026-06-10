package com.fenix.ordenararquivos.process.ocr

import com.fenix.ordenararquivos.exceptions.OcrException
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.process.Smv
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import net.sourceforge.tess4j.util.LoadLibs
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import java.awt.image.DataBufferByte
import java.io.File
import javax.imageio.ImageIO

class TesseractOcrEngine : OcrEngineStrategy {

    private val mLog = LoggerFactory.getLogger(TesseractOcrEngine::class.java)
    private val pastaTemporaria = File(System.getProperty("user.dir"), "temp/")
    private val arquivoAux = File(pastaTemporaria, "ocr_aux.jpg")

    private var linguagemAtual: Linguagem = Linguagem.JAPANESE
    private lateinit var instance: ITesseract
    private lateinit var ocrFile: File
    private lateinit var input: Mat
    private lateinit var output: Mat
    private lateinit var aux: Mat

    private val mGerarImagens = false

    override fun prepare(linguagem: Linguagem) {
        linguagemAtual = linguagem
        val tessdata = NativePaths.tessdataDir
        if (!tessdata.exists()) {
            throw TesseractException("Erro ao iniciar o Tesseract.\nArquivos de dados do Tesseract não encontrado.")
        }
        instance = Tesseract()
        LoadLibs.extractTessResources("tessdata")
        instance.setDatapath(tessdata.path)
        instance.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO)
        instance.setVariable("preserve_interword_spaces", "1")
    }

    override fun recognize(image: File, linguagem: Linguagem): String {
        linguagemAtual = linguagem
        return when (linguagem) {
            Linguagem.JAPANESE -> recognizeJapanese(image)
            else -> recognizeSinglePass(image, tessLanguage(linguagem), vertical = false)
        }
    }

    override fun clear() {
        if (::ocrFile.isInitialized && ocrFile.exists()) ocrFile.delete()
        if (arquivoAux.exists()) arquivoAux.delete()
    }

    override fun isAvailable(): Boolean = NativePaths.tessdataDir.exists()

    private fun recognizeJapanese(image: File): String {
        val horizontal = recognizeSinglePass(image, "jpn", vertical = false)
        val vertical = recognizeSinglePass(image, "jpn_vert", vertical = true)
        return JapaneseLayoutHelper.pickBestResult(listOf(horizontal, vertical), "-", "|")
    }

    private fun recognizeSinglePass(image: File, language: String, vertical: Boolean): String {
        configureLanguage(language, vertical)
        return processTesseract(image)
    }

    private fun configureLanguage(language: String, vertical: Boolean) {
        instance.setLanguage(language)
        if (vertical) {
            instance.setVariable("textord_tabfind_vertical_text", "1")
        } else {
            instance.setVariable("textord_tabfind_vertical_text", "0")
        }
    }

    private fun tessLanguage(linguagem: Linguagem): String = when (linguagem) {
        Linguagem.JAPANESE -> "jpn"
        Linguagem.PORTUGUESE -> "por"
        Linguagem.ENGLISH -> "eng"
        else -> "eng"
    }

    private fun processTesseract(image: File): String {
        try {
            ocrFile = File(pastaTemporaria, "ocr_${image.name.substringBeforeLast(".")}.jpg")
            val ocrImage = ImageIO.read(image) ?: return ""
            pastaTemporaria.mkdirs()
            ImageIO.write(ocrImage, "jpg", ocrFile)

            input = Mat(ocrImage.height, ocrImage.width, CvType.CV_8UC3)
            input.put(0, 0, (ocrImage.raster.dataBuffer as DataBufferByte).data)
            output = Mat()
            aux = Mat()

            Imgproc.GaussianBlur(input, output, Size(15.0, 15.0), 0.0, 0.0)
            if (mGerarImagens) Imgcodecs.imwrite(debugPath(image, "_01gaugasian.jpg"), output)

            Imgproc.cvtColor(output, aux, Imgproc.COLOR_RGB2GRAY, 0)
            if (mGerarImagens) Imgcodecs.imwrite(debugPath(image, "_02grayscale.jpg"), aux)

            Imgproc.Laplacian(aux, output, CvType.CV_16S, 3, 2.0, 0.0)
            val mgdArray = getMgdArray(output)
            aux = arrayToMat(mgdArray, output.height(), output.width(), CvType.CV_16S)

            aux.convertTo(aux, CvType.CV_8UC1)
            Imgproc.threshold(aux, output, 80.0, 255.0, Imgproc.THRESH_BINARY)

            val textBlocks = findTextBlocks(output, input, image.absolutePath)
            if (textBlocks.size < 2) {
                Imgproc.adaptiveThreshold(
                    aux, output, 255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 11, 2.0
                )
            }

            val kernel = getStructuringElement(input.height() * input.width())
            Imgproc.dilate(output, aux, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernel))

            val finalBlocks = findTextBlocks(aux, input, image.absolutePath)
            val orientation = JapaneseLayoutHelper.detectOrientationFromRects(finalBlocks)

            if (finalBlocks.isNotEmpty()) {
                val sorted = JapaneseLayoutHelper.sortRects(finalBlocks, orientation)
                val textos = StringBuilder()
                for (block in sorted) {
                    val crop = Mat(input, block)
                    val lang = if (linguagemAtual == Linguagem.JAPANESE) {
                        if (block.height > block.width * 1.2) "jpn_vert" else "jpn"
                    } else {
                        tessLanguage(linguagemAtual)
                    }
                    configureLanguage(lang, lang == "jpn_vert")
                    val texto = instance.doOCR(preprocessTextBlock(crop)).trim()
                    if (texto.isNotEmpty()) textos.append(texto).append("\n")
                }
                return textos.toString().trim()
            }

            val region = boundingRegion(finalBlocks, ocrImage.width, ocrImage.height)
            val textBlock = Mat(input, region)
            return instance.doOCR(preprocessTextBlock(textBlock))
        } catch (e: Error) {
            mLog.error("Erro ao processar o OCR.", e)
            throw OcrException(e.message ?: "Erro ao processar o OCR.")
        } catch (e: Exception) {
            mLog.error("Erro ao processar o OCR.", e)
            throw OcrException(e.message ?: "Erro ao processar o OCR.")
        }
    }

    private fun boundingRegion(textBlocks: List<Rect>, imgWidth: Int, imgHeight: Int): Rect {
        if (textBlocks.isEmpty()) return Rect(0, 0, imgWidth, imgHeight)
        var x = textBlocks.minOf { it.x }
        var y = textBlocks.minOf { it.y }
        val maxX = textBlocks.maxOf { it.x + it.width }
        val maxY = textBlocks.maxOf { it.y + it.height }
        return Rect(x, y, maxX - x, maxY - y)
    }

    private fun debugPath(image: File, suffix: String): String =
        image.absolutePath.replace(".jpg", suffix)

    private fun findTextBlocks(dilated: Mat, original: Mat, file: String): List<Rect> {
        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        val numberOfLabels = Imgproc.connectedComponentsWithStats(dilated, labels, stats, centroids, 8, CvType.CV_32S)
        val textBlocks = mutableListOf<Rect>()

        for (i in 1 until numberOfLabels) {
            val textBlock = Rect(Point(stats[i, 0][0], stats[i, 1][0]), Size(stats[i, 2][0], stats[i, 3][0]))
            val crop = Mat(original, textBlock)
            val isHorizontalOrSquare = (textBlock.width.toDouble() / textBlock.height.coerceAtLeast(1)) >= 0.8
            val isVertical = textBlock.height > textBlock.width * 1.2
            if (isHorizontalOrSquare || isVertical) {
                if (stats[i, 4][0] > dilated.height() * dilated.width() * 0.002) {
                    Imgproc.cvtColor(crop, crop, Imgproc.COLOR_RGB2GRAY, 0)
                    Imgproc.resize(crop, crop, Size(100.0, 50.0), 4.0, 4.0, Imgproc.INTER_LINEAR)
                    if (Smv.blockContainsText(crop)) {
                        textBlocks.add(textBlock)
                    }
                }
            }
        }
        return textBlocks
    }

    private fun preprocessTextBlock(textBlock: Mat): File {
        Imgproc.cvtColor(textBlock, aux, Imgproc.COLOR_RGB2GRAY, 0)
        Imgproc.GaussianBlur(aux, input, Size(0.0, 0.0), 3.0)
        val unsharp = Mat()
        Core.addWeighted(aux, 1.5, input, -0.5, 0.0, unsharp)
        Core.normalize(unsharp, aux, 0.0, 1.0, Core.NORM_MINMAX)
        val binary = thresholdImageWithKmeans(aux)
        if (arquivoAux.exists()) arquivoAux.delete()
        Imgcodecs.imwrite(arquivoAux.absolutePath, binary)
        return arquivoAux
    }

    private fun getMgdArray(laplaceImage: Mat): Array<DoubleArray> {
        val laplaceArray = matToArray(laplaceImage)
        val mgdArray = Array(laplaceImage.height()) { DoubleArray(laplaceImage.width()) }
        for (i in 0 until laplaceImage.height()) {
            for (j in 10 until laplaceImage.width() - 10) {
                mgdArray[i][j] = getMgdNumber(i, j, laplaceArray)
            }
        }
        return mgdArray
    }

    private fun getMgdNumber(i: Int, j: Int, matArray: Array<DoubleArray>): Double {
        var min = matArray[i][j - 10]
        var max = matArray[i][j - 10]
        for (k in j - 9..j + 10) {
            if (matArray[i][k] > max) max = matArray[i][k]
            if (matArray[i][k] < min) min = matArray[i][k]
        }
        return max - min
    }

    private fun arrayToMat(array: Array<DoubleArray>, height: Int, width: Int, matType: Int): Mat {
        val image = Mat(height, width, matType)
        for (i in 0 until height) {
            for (j in 0 until width) {
                image.put(i, j, array[i][j])
            }
        }
        return image
    }

    private fun matToArray(frame: Mat): Array<DoubleArray> {
        val array = Array(frame.height()) { DoubleArray(frame.width()) }
        for (i in 0 until frame.height()) {
            for (j in 0 until frame.width()) {
                array[i][j] = frame[i, j][0]
            }
        }
        return array
    }

    private fun thresholdImageWithKmeans(image: Mat): Mat {
        val dt = Mat(image.height() * image.width(), 1, CvType.CV_32FC1)
        var k = 0
        for (i in 0 until image.height()) {
            for (j in 0 until image.width()) {
                dt.put(k, 0, image[i, j][0])
                k++
            }
        }
        val labels = Mat()
        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0)
        val centers = Mat()
        Core.kmeans(dt, 2, labels, criteria, 5, Core.KMEANS_PP_CENTERS, centers)
        return convertLabelsToBinary(labels, image)
    }

    private fun convertLabelsToBinary(labels: Mat, image: Mat): Mat {
        val binary = Mat(image.height(), image.width(), CvType.CV_8UC1)
        var k = 0
        for (i in 0 until binary.height()) {
            for (j in 0 until binary.width()) {
                binary.put(i, j, if (labels[k, 0][0] == 1.0) 255.0 else 0.0)
                k++
            }
        }
        return binary
    }

    private fun getStructuringElement(imageResolution: Int): Size = when {
        imageResolution < 500000 -> Size(5.0, 5.0)
        imageResolution < 1000000 -> Size(7.0, 7.0)
        imageResolution < 1500000 -> Size(11.0, 11.0)
        imageResolution < 2000000 -> Size(13.0, 13.0)
        else -> Size(15.0, 15.0)
    }
}
