package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.exceptions.OcrException
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import net.sourceforge.tess4j.util.LoadLibs
import org.json.JSONException
import org.json.JSONObject
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import java.awt.image.DataBufferByte
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO


object Ocr {
    internal val OCR_CHAPTER_REGEX = Regex("第?([\\d]+)話?[\\D]*([\\d]+)")

    private val mLog = LoggerFactory.getLogger(Ocr::class.java)
    private val mClient = OkHttpClient().apply {
        setConnectTimeout(60, TimeUnit.SECONDS)
        setReadTimeout(60, TimeUnit.SECONDS)
        setWriteTimeout(60, TimeUnit.SECONDS)
    }

    var isTeste : Boolean = false
    private const val mGerarImagens = true
    private var mGeminiKey : String
    private var mIsFirstKey : Boolean = true
    var mLibs = false
        private set

    var mGemini = false
        private set

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

        mGemini = Configuracao.geminiKey1.isNotEmpty() || Configuracao.geminiKey2.isNotEmpty()
        mGeminiKey = Configuracao.geminiKey1
        mIsFirstKey = true
    }

    /**
     * Loads Native Libraries for the detected OS
     */
    private fun loadLibraries() {
        val path = File(Paths.get("").toAbsolutePath().toString() + "/natives/").path
        val osName = System.getProperty("os.name").lowercase()
        val bit = System.getProperty("sun.arch.data.model")?.toIntOrNull() ?: 32
        if (osName.contains("win")) {
            when (bit) {
                32 -> {
                    System.load(Paths.get(path, "opencv_320_32.dll").toString())
                    mLog.info("Loaded OpenCV for Windows 32 bit")
                    System.load(Paths.get(path, "opencv_ffmpeg320_32.dll").toString())
                    mLog.info("Loaded FFMPEG for Windows 32 bit")
                    System.load(Paths.get(path, "openh264-1.6.0-win32msvc.dll").toString())
                    mLog.info("Loaded OpenH264 for Windows 32 bit")
                }
                64 -> {
                    System.load(Paths.get(path, "opencv_java320_64.dll").toString())
                    mLog.info("Loaded OpenCV for Windows 64 bit")
                    System.load(Paths.get(path, "opencv_ffmpeg320_64.dll").toString())
                    mLog.info("Loaded FFMPEG for Windows 64 bit")
                    System.load(Paths.get(path, "openh264-1.6.0-win64msvc.dll").toString())
                    mLog.info("Loaded OpenH264 for Windows 64 bit")
                }
                else -> {
                    mLog.info("Unknown Windows bit - trying with 32")
                    System.load(Paths.get(path, "opencv_java320_32.dll").toString())
                    mLog.info("Loaded OpenCV for Windows 32 bit")
                    System.load(Paths.get(path, "openh264-1.6.0-win32msvc.dll").toString())
                    mLog.info("Loaded OpenH264 for Windows 32 bit")
                }
            }
        } else if (osName.contains("mac")) {
            mLog.info("This version os the application cannot run on MAC OS yet.")
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            when (bit) {
                32 -> {
                    //todo add support
                    mLog.info("32-bit Linux not supported yet")
                }
                64 -> {
                    System.load(Paths.get(path, "libopencv_320_64.so").toString())
                    mLog.info("Loaded OpenCV for Linux 64 bit")
                    System.load(Paths.get(path, "libopenh264-1.6.0-linux64.3.so").toString())
                    mLog.info("Loaded OpenH264 for Linux 64 bit")
                }
                else -> {
                    mLog.info("Unknown Linux bit - trying with 32")
                    mLog.info("OS not supported yet")
                }
            }
        }
    }

    private val PASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private val ARQUIVO_AUX = "$PASTA_TEMPORARIA\\ocr_aux.jpg"
    private var TESSERACT : File? = null

    init {
        try {
            TESSERACT = File(Paths.get("").toAbsolutePath().toString() + "/tessdata/")
        } catch (e: IOException) {
            mLog.error("Erro ao carregar os dados do tesseract", e)
        }
    }

    private lateinit var instance: ITesseract

    private lateinit var ocrFile : File

    private lateinit var input : Mat
    private lateinit var output : Mat
    private lateinit var aux : Mat

    @JvmStatic
    fun prepare(isJapanese : Boolean) {
        if (TESSERACT == null && mGemini)
            return

        if (TESSERACT == null)
            throw TesseractException("Erro ao iniciar o Tesseract.\nArquivos de dados do Tesseract não encontrado.")

        instance = Tesseract()
        LoadLibs.extractTessResources("tessdata")
        instance.setDatapath(TESSERACT!!.path)

        if (isJapanese)
            instance.setLanguage("jpn")
        else
            instance.setLanguage("eng")
    }

    @JvmStatic
    fun clear() {
        if (ocrFile.exists())
            ocrFile.delete()

        val block = File(ARQUIVO_AUX)
        if (block.exists())
            block.delete()
    }

    @JvmStatic
    fun process(image : File, separadorPagina : String, separadorCapitulo: String) : String {
        if (!image.exists() || image.length() == 0.toLong())
            return ""

        if (isTeste)
            return "001-05 Suggestion"

        return if (mGemini)
            processGemini(image, geraPromptGemini(separadorPagina, separadorCapitulo))
        else
            ocrToCapitulo(processTesseract(image), separadorPagina, separadorCapitulo)
    }

    //<--------------------------  TESSERACT   -------------------------->
    private fun processTesseract(image: File): String {
        try {
            ocrFile = File(PASTA_TEMPORARIA.toString() + "\\ocr_" + image.name.substringBeforeLast(".") + ".jpg")
            val ocrImage = ImageIO.read(image)
            if (ocrImage == null) return ""

            ImageIO.write(ocrImage, "jpg", ocrFile)

            input = Mat(ocrImage.height, ocrImage.width, CvType.CV_8UC3)
            input.put(0, 0, (ocrImage.raster.dataBuffer as DataBufferByte).data)
            output = Mat()
            aux = Mat()

            /*
            Apply Gaussian Blurred Filter
            GaussianBlur Parameters:
            src – input image
            dst – output image of the same size and type as src.
            ksize – Gaussian kernel size. ksize.width and ksize.height
                    can differ but they both must be positive and odd.
                    Or, they can be zero’s and then they are computed from sigma* .
            sigmaX – Gaussian kernel standard deviation in X direction.
            sigmaY – Gaussian kernel standard deviation in Y direction;
                    if sigmaY is zero, it is set to be equal to sigmaX,
                    if both sigmas are zeros, they are computed from ksize.width and ksize.height
             */
            Imgproc.GaussianBlur(input, output, Size(15.0, 15.0), 0.0, 0.0)
            if (mGerarImagens)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_01gaugasian.jpg"), output)

            // Convert to GrayScale
            Imgproc.cvtColor(output, aux, Imgproc.COLOR_RGB2GRAY, 0)
            if (mGerarImagens)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_02grayscale.jpg"), aux)

            /*
            Apply the Laplacian Filter
            Laplacian Parameters:
            src – Source image.
            dst – Destination image of the same size and the same number of channels as src .
            ddepth – Desired depth of the destination image.
            ksize – Aperture size used to compute the second-derivative filters.
                    The ksize must be positive and odd. Bigger ksize leads to stronger intensity.
            scale – Optional scale factor for the computed Laplacian values.
                    By default, no scaling is applied.
            delta – Optional delta value that is added to the results prior to storing them in dst.
             */
            Imgproc.Laplacian(aux, output, CvType.CV_16S, 3, 2.0, 0.0)
            if (mGerarImagens)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_03filter.jpg"), aux)

            // Apply the MaximumGradientDifference(MGD) operator
            val mgdArray: Array<DoubleArray> = getMgdArray(output)

            // Convert the mgdArray back again into a Mat object
            aux = arrayToMat(mgdArray, output.height(), output.width(), CvType.CV_16S)
            if (mGerarImagens)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_04array.jpg"), aux)

            // Convert to Binary
            aux.convertTo(aux, CvType.CV_8UC1)
            Imgproc.threshold(aux, output, 80.0, 255.0, Imgproc.THRESH_BINARY)
            if (mGerarImagens)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_05binary.jpg"), output)

            val kernel: Size = getStructuringElement(input.height() * input.width())
            val structuringElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernel)
            mLog.info("[Structuring Element: ${kernel.height} x ${kernel.width}]")

            /*
            Apply the morphological operation Dilation
            ImgProc.dilate Parameters:
            src – input image; the number of channels can be arbitrary,
                  but the depth should be one of CV_8U, CV_16U, CV_16S, CV_32F` or ``CV_64F.
            dst – output image of the same size and type as src.
            element – structuring element used for dilation;
                  if element=Mat() , a 3 x 3 rectangular structuring element is used.
            anchor – position of the anchor within the element;
                  default value (-1, -1) means that the anchor is at the element center.
        */
            Imgproc.dilate(output, aux, structuringElement)
            if (mGerarImagens)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_06dilatation.jpg"), aux)

            // Find the candidate text blocks by finding the connected components of dilated image
            val textBlocks: List<Rect> = findTextBlocks(aux, input, image.absolutePath.toString())
            if (mGerarImagens) {
                paintTextBlocks(textBlocks, aux)
                Imgcodecs.imwrite(image.absolutePath.toString().replace(".jpg", "_08textblock.jpg"), aux)
            }

            val region = Rect(0, 0, ocrImage.height, ocrImage.width)
            if (textBlocks.isNotEmpty()) {
                region.x = textBlocks[0].x
                region.y = textBlocks[0].y
                region.width = textBlocks[0].width
                region.height = textBlocks[0].height

                for (block in textBlocks) {
                    if (region.x > block.x)
                        region.x = block.x
                    if (region.y > block.y)
                        region.y = block.y

                    if ((region.x + region.width) < (block.x + block.width))
                        region.width = (block.x + block.width) - region.x
                    if ((region.y + region.height) < (block.y + block.height))
                        region.height = (block.y + block.height) - region.y
                }
            }

            /*// Use only get small block
            for (textBlock in getCroppedTextBlocks(textBlocks, input)) {
                val texto = instance.doOCR(preprocessTextBlock(textBlock))
                textos.add(texto)
                mLOG.info(texto)
            }*/

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

    internal fun ocrToCapitulo(textos: String, separadorPagina : String = "-", separadorCapitulo: String = "|"): String {
        if (textos.isEmpty())
            return ""

        val linhas = textos.split("\n")

        val capitulos = mutableMapOf<Int, Int>()
        for (linha in linhas) {
            OCR_CHAPTER_REGEX.matchEntire(linha)?.let {
                if (it.groups.size > 2) {
                    if (it.groups[1] != null && it.groups[2] != null)
                        capitulos[Integer.parseInt(it.groups[1]!!.value)] = Integer.parseInt(it.groups[2]!!.value)
                }
            }
        }

        var capAnterior = 0
        var pagAnterior = 0
        var sugestao = ""
        capitulos.keys.sorted().forEach {
            if (it > capAnterior && capitulos[it]!! > pagAnterior) {
                capAnterior = it
                pagAnterior = capitulos[it]!!
                sugestao += it.toString() + separadorPagina + capitulos[it] + "\n"
            }
        }
        return sugestao.trim()
    }

    /**
     * Finds the text block areas by filtering the connected components of the dilated Mat image.
     * First, finds the candidate text blocks and then filters them.
     * @param dilated The dilated Mat image
     * @return A List of Rect representing the finalist text block areas
     */
    private fun findTextBlocks(dilated: Mat, original: Mat, file : String): List<Rect> {
        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        val numberOfLabels = Imgproc.connectedComponentsWithStats(dilated, labels, stats, centroids, 8, CvType.CV_32S)
        val textBlocks: MutableList<Rect> = ArrayList()

        // Label 0 is considered to be the background label, so we skip it
        for (i in 1 until numberOfLabels) {
            // stats columns; [0-4] : [left top width height area}
            val textBlock = Rect(Point(stats[i, 0][0], stats[i, 1][0]), Size(stats[i, 2][0], stats[i, 3][0]))
            val crop = Mat(original, textBlock)
            if ((textBlock.width / textBlock.height).toDouble().compareTo(1.0) >= 0) { // FILTER 1
                if (stats[i, 4][0].compareTo(dilated.height() * dilated.width() * 0.002) > 0) { // FILTER 2
                    Imgproc.cvtColor(crop, crop, Imgproc.COLOR_RGB2GRAY, 0)
                    Imgproc.resize(crop, crop, Size(100.0, 50.0), 4.0, 4.0, Imgproc.INTER_LINEAR)
                    if (Smv.blockContainsText(crop)) { // FILTER 3
                        if (mGerarImagens)
                            Imgcodecs.imwrite(file.replace(".jpg", "_07findtext.jpg"), crop)
                        textBlocks.add(textBlock)
                    } else if (mGerarImagens)
                        Imgcodecs.imwrite(file.replace(".jpg", "_07findtext.jpg"), crop)
                } else if (mGerarImagens)
                    Imgcodecs.imwrite(file.replace(".jpg", "_07findtext.jpg"), crop)
            } else if (mGerarImagens)
                Imgcodecs.imwrite(file.replace(".jpg", "_07findtext.jpg"), crop)
        }
        return textBlocks
    }

    /**
     * Paints with red the text block boundaries of the original image
     * @param textBlocks The list of text blocks
     * @param original Original image
     */
    private fun paintTextBlocks(textBlocks: List<Rect>, original: Mat) {
        for (r in textBlocks) {
            Imgproc.rectangle(
                original, Point(r.x.toDouble(), r.y.toDouble()), Point((r.x + r.width).toDouble(), (r.y + r.height).toDouble()),
                Scalar(255.0), 2
            )
        }
    }

    /**
     * Crops areas from the original image which correspond to the given Rect blocks
     * @param textBlocks The Rect text blocks list
     * @return A list of Mat crops
     */
    private fun getCroppedTextBlocks(textBlocks: List<Rect>, original: Mat): List<Mat> {
        val textRegions: MutableList<Mat> = java.util.ArrayList()
        for (r in textBlocks) {
            val crop = Mat(original, r)
            textRegions.add(crop)
        }
        return textRegions
    }

    /**
     * Preprocesses the text blocks, before proceeding to ocr, in order
     * to achieve better extraction results
     * @param textBlock List of image's text blocks in Rect format
     */
    private fun preprocessTextBlock(textBlock: Mat): File {
        Imgproc.cvtColor(textBlock, aux, Imgproc.COLOR_RGB2GRAY, 0)
        Imgproc.GaussianBlur(aux, input, Size(0.0, 0.0), 3.0)
        val unsharp = Mat()
        Core.addWeighted(aux, 1.5, input, -0.5, 0.0, unsharp)
        Core.normalize(unsharp, aux, 0.0, 1.0, Core.NORM_MINMAX)
        val binary: Mat = thresholdImageWithKmeans(aux)

        val block = File(ARQUIVO_AUX)
        if (block.exists())
            block.delete()

        Imgcodecs.imwrite(ARQUIVO_AUX, binary)
        return block
    }

    /**
     * Returns the MaximumGradientDifference operation of the input image
     * by calculating the MGD number of every pixel
     * @param laplaceImage The target Mat image
     * @return The MGD result in double 2D array format
     */
    private fun getMgdArray(laplaceImage: Mat): Array<DoubleArray> {
        val laplaceArray: Array<DoubleArray> = matToArray(laplaceImage)
        val mgdArray = Array(laplaceImage.height()) { DoubleArray(laplaceImage.width()) }
        for (i in 0 until laplaceImage.height()) {
            for (j in 10 until laplaceImage.width() - 10)
                mgdArray[i][j] = getMgdNumber(i, j, laplaceArray)
        }
        return mgdArray
    }

    /**
     * Calculates the MGD number of a single image pixel
     * @param I The height-coordinate of the given pixel
     * @param J The width-coordinate of the given pixel
     * @param matArray The entire respective image stored in a 2D array
     * @return The MGD number for the pixel that was given
     */
    private fun getMgdNumber(I: Int, J: Int, matArray: Array<DoubleArray>): Double {
        var min = matArray[I][J - 10]
        var max = matArray[I][J - 10]
        for (j in J - 9..J + 10) {
            if (matArray[I][j] > max) {
                max = matArray[I][j]
            }
            if (matArray[I][j] < min) {
                min = matArray[I][j]
            }
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
            for (j in 0 until frame.width())
                array[i][j] = frame[i, j][0]
        }
        return array
    }


    /**
     * Applies the k-means clustering algorithm, using only 2 clusters,
     * in order to achieve image thresholding
     * @param image The target Mat image
     * @return The original image converted to binary (black and white)
     */
    private fun thresholdImageWithKmeans(image: Mat): Mat {
        val dt = Mat(image.height() * image.width(), 1, CvType.CV_32FC1)
        var k = 0
        for (i in 0 until image.height()) {
            for (j in 0 until image.width()) {
                dt.put(k, 0, image[i, j][0])
                k++
            }
        }
        val clusters = 2
        val labels = Mat()

        /*
         TermCriteria Constructor Parameters:
         type: The type of termination criteria, one of TermCriteria::Type
         maxCount: The maximum number of iterations or elements to compute.
         epsilon: The desired accuracy or change in parameters at which the iterative algorithm stops.
        */
        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0)
        val attempts = 5
        val flag = Core.KMEANS_PP_CENTERS
        val centers = Mat()
        Core.kmeans(dt, clusters, labels, criteria, attempts, flag, centers)
        return convertLabelsToBinary(labels, image)
    }

    /**
     * Converts the k-means labels result into a new binary image
     * @param labels The labels result from the kmeans algorithm
     * @param image The Mat image for which we calculated the kmeans thresholding
     * @return The binary image that has been created
     */
    private fun convertLabelsToBinary(labels: Mat, image: Mat): Mat {
        val binary = Mat(image.height(), image.width(), CvType.CV_8UC1)
        var k = 0
        for (i in 0 until binary.height()) {
            for (j in 0 until binary.width()) {
                if (java.lang.Double.compare(labels[k, 0][0], 1.0) == 0) {
                    binary.put(i, j, 255.0)
                } else {
                    binary.put(i, j, 0.0)
                }
                k++
            }
        }
        return binary
    }


    /**
     * Returns a kernel to be used as a structuring element for the dilation process,
     * where its size depends on the image resolution
     * @return The selected structuring element in the format of Size
     */
    // TODO consider larger kernels
    private fun getStructuringElement(imageResolution: Int): Size {
        return if (imageResolution < 500000) {
            Size(5.0, 5.0)
        } else if (imageResolution < 1000000) {
            Size(7.0, 7.0)
        } else if (imageResolution < 1500000) {
            Size(11.0, 11.0)
        } else if (imageResolution < 2000000) {
            Size(13.0, 13.0)
        } else {
            Size(15.0, 15.0)
        }
    }

    //<--------------------------  GEMINI   -------------------------->
    private fun converteToBase64(imagem: File): String {
        val bytes = imagem.readBytes()
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun mimeType(imagem: File): String {
        val mimeType = Files.probeContentType(imagem.toPath())
        return mimeType ?: "image/jpg"
    }

    private const val URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta/models/"

    private fun processGemini(imagem : File, texto : String = "") : String {
        mLog.info("Preparando consulta ao Gemini.")
        val mediaType = MediaType.parse("application/json")
        val base64 = converteToBase64(imagem)
        val mime = mimeType(imagem)
        val body = RequestBody.create(mediaType, "{\"contents\":[{\"parts\":[{\"text\":\"$texto\"},{\"inline_data\":{\"mime_type\":\"$mime\",\"data\":\"$base64\"}}]}]}")

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
            response.body().close()
            mGeminiKey = Configuracao.geminiKey2
            mIsFirstKey = false
            return processGemini(imagem, texto)
        }

        if (response.code() > 299 || response.body() == null) {
            response.body().close()
            throw Exception("Erro ao consultar o Gemini: ${response.code()} - ${response.message()}")
        }

        return try {
            val body = response.body()!!.string()
            mLog.info("Resposta Gemini: $body")
            val jsonObject = JSONObject(body)
            var texto = jsonObject.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")

            if (texto.contains("'"))
                texto = texto.replace("'", "")

            if (texto.contains("\n") && texto.contains("-")) {
                var sugestao = ""
                for (linha in texto.split("\n"))
                    sugestao += linha.substringBefore("-").replace("第", "").replace("話", "").trim().padStart(3, '0') + "-" + linha.substringAfter("-").trim() + "\n"
                sugestao.substringBeforeLast("\n")
            } else
                texto
        } catch (e: JSONException) {
            mLog.error(e.message, e)
            throw e
        }
    }

    private val TEXTO_PADRAO = """Esta é uma imagem de um sumário. Realize duas tarefas: extração e ordenação.
                1. Extraia os capítulos e páginas.
                2. Gere a saída ESTRITAMENTE ordenada da seguinte forma:
                   - Primeiro: Itens que começam com números, em ordem crescente (ex: 001, 002, 10).
                   - Depois: Itens que começam com letras ou texto, em ordem alfabética (ex: A, B, Apêndice, Glossário).
                   
                Use o formato: 'Número do capítulo %s Número da Página %s Descrição do capítulo'.
                Se não houver número da página, use XXX.
                
                Exemplos de saída ordenada:
                '001%s05%sIntrodução'
                '002%s12%sDesenvolvimento'
                '010%s50%sConclusão'
                'A%s90%sApêndice A'
                
                Não inclua cabeçalhos, Markdown ou explicações, apenas a lista formatada e ordenada.""".trimIndent()

    private fun geraPromptGemini(separadorPagina: String, separadorCapitulo: String) : String = String.format(
        TEXTO_PADRAO,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo
    )

}
