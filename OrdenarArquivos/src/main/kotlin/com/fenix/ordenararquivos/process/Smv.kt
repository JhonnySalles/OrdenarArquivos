package com.fenix.ordenararquivos.process

import cern.colt.list.IntArrayList
import cern.colt.map.OpenIntIntHashMap
import libsvm.*
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*


object Smv {

    private val LOG = LoggerFactory.getLogger(Smv::class.java)

    private val prob = svm_problem()
    private val param = svm_parameter()
    private var model: svm_model? = null
    private const val PREDICTION_THRESHOLD = 5

    init {
        try {
            model = svm.svm_load_model(Paths.get("").toAbsolutePath().toString() + "/models/10000.model")
        } catch (e: IOException) {
            LOG.error("Erro ao carregar o modelo SVM", e)
        }
    }

    // ================================================================== SMV ==================================================================
    fun setParameters() {
        param.svm_type = svm_parameter.ONE_CLASS
        param.nu = 0.01
        param.kernel_type = svm_parameter.LINEAR
        param.probability = 1
    }

    fun createProblem(train: Array<DoubleArray>, labels: DoubleArray) {
        val dataCount = train.size
        prob.l = dataCount
        prob.x = arrayOfNulls(dataCount)
        for (i in 0 until dataCount) {
            val features = train[i]
            prob.x[i] = arrayOfNulls(features.size)
            for (j in features.indices) {
                val node = svm_node()
                node.index = j
                node.value = features[j]
                prob.x[i][j] = node
            }
        }
        prob.y = Arrays.copyOf(labels, labels.size)
    }

    fun train(): svm_model? {
        println(svm.svm_check_parameter(prob, param))
        return svm.svm_train(prob, param) // returns the model
    }

    fun save(path: String?, model: svm_model?) {
        try {
            svm.svm_save_model(path, model)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun evaluate(features: DoubleArray, model: svm_model?): Double {
        val nodes = arrayOfNulls<svm_node>(features.size)
        for (i in features.indices) {
            val node = svm_node()
            node.index = i
            node.value = features[i]
            nodes[i] = node
        }
        return svm.svm_predict(model, nodes)
    }

    fun blockContainsText(block: Mat): Boolean {
        var negativeRegions = 0
        for (subregion in getSubregions(block)) {
            val intData: IntArray = get_ULBP_Features(subregion)
            val doubleData: DoubleArray = intArrayToDouble(intData)
            normalizeArray(doubleData)
            val score: Double = evaluate(doubleData, model)
            if (java.lang.Double.compare(score, -1.0) == 0)
                negativeRegions++
        }
        return negativeRegions <= PREDICTION_THRESHOLD
    }

    // ================================================================== TRAINING ==================================================================
    private const val FEATURE_DIMENSIONS = 59

    private fun getLabels(dataCounter: Int): DoubleArray? {
        val labels = DoubleArray(dataCounter)
        for (i in labels.indices) {
            labels[i] = 0.0
        }
        return labels
    }

    private  fun getTrainingData(path: String?): Array<DoubleArray>? {
        val dir = File(path)
        val numberOfData = dir.listFiles().size
        val training_data = Array(numberOfData * 50) { DoubleArray(FEATURE_DIMENSIONS) }
        var regionCounter = 0
        var featureCounter = 0
        for (img in dir.listFiles()) {
            val src = Imgcodecs.imread(img.absolutePath)
            for (subregion in getSubregions(src)) {
                for (feature in get_ULBP_Features(subregion)) {
                    training_data[regionCounter][featureCounter++] = feature.toDouble()
                }
                featureCounter = 0
                regionCounter++
            }
        }
        for (data in training_data) {
            normalizeArray(data)
        }
        return training_data
    }

    private fun intArrayToDouble(array: IntArray): DoubleArray {
        val dArray = DoubleArray(array.size)
        for (i in array.indices) {
            dArray[i] = array[i].toDouble()
        }
        return dArray
    }

    private fun getSubregions(image: Mat): List<Mat> {
        val subregions: MutableList<Mat> = ArrayList()
        var i = 0
        while (i < image.rows()) {
            var j = 0
            while (j < image.cols()) {
                val crop = Rect(Point(j.toDouble(), i.toDouble()), Size(10.0, 10.0)) // points coords are reversed! Point(column,row)
                val region = Mat(image, crop)
                subregions.add(region)
                j = j + 10
            }
            i = i + 10
        }
        return subregions
    }

    private fun normalizeArray(array: DoubleArray) {
        var max = array[0]
        var min = array[0]
        for (i in 1 until array.size) {
            if (java.lang.Double.compare(array[i], max) > 0)
                max = array[i]
            if (java.lang.Double.compare(array[i], min) < 0)
                min = array[i]
        }
        for (i in array.indices)
            array[i] = (array[i] - min) / (max - min)
    }


    // ================================================================== BINARY PATTERN ==================================================================

    // Using this number to store all non-uniform patter frequencies
    private const val NON_UNIFORM_BIN = 999

    // 59 dimensions feature vector, based on the LocalBinaryPattern histogram
    private val histogramTemplate: OpenIntIntHashMap = getHistogramTemplate()

    /**
     * Extracts features from a Mat image based on the LocalBinaryPattern (Uniform Rotated Local Binary Pattern) as
     * described in the following [paper/a>.
     * @param image The Mat image
     * @return A 59-length feature vector - the value set of the histogram
    ](https://www.cs.tut.fi/~mehta/rlbp) */
    private fun get_URLBP_Features(image: Mat): IntArray? {
        // Using: R(Radius) = 1, P(Pixel Neighbours) = 8
        val histogram: OpenIntIntHashMap = histogramTemplate.clone() as OpenIntIntHashMap
        for (x in 1 until image.rows() - 1) {
            for (y in 1 until image.cols() - 1) {
                val diffs = DoubleArray(8)
                // TODO try with >= !!!!!!!
                // Find the differences between the current center pixel and its neighbours
                val center = image[x, y][0] // Starting from the same row, right column pixel, clockwise
                diffs[0] = if (java.lang.Double.compare(image[x, y + 1][0], center) > 0) image[x, y + 1][0] - center else 0.0
                diffs[1] = if (java.lang.Double.compare(image[x + 1, y + 1][0], center) > 0) image[x + 1, y + 1][0] - center else 0.0
                diffs[2] = if (java.lang.Double.compare(image[x + 1, y][0], center) > 0) image[x + 1, y][0] - center else 0.0
                diffs[3] = if (java.lang.Double.compare(image[x + 1, y - 1][0], center) > 0) image[x + 1, y - 1][0] - center else 0.0
                diffs[4] = if (java.lang.Double.compare(image[x, y - 1][0], center) > 0) image[x, y - 1][0] - center else 0.0
                diffs[5] = if (java.lang.Double.compare(image[x - 1, y - 1][0], center) > 0) image[x - 1, y - 1][0] - center else 0.0
                diffs[6] = if (java.lang.Double.compare(image[x - 1, y][0], center) > 0) image[x - 1, y][0] - center else 0.0
                diffs[7] = if (java.lang.Double.compare(image[x - 1, y + 1][0], center) > 0) image[x - 1, y + 1][0] - center else 0.0

                // Finding the dominant direction pixel i.e. the pixel with the biggest difference
                var max = Double.MIN_VALUE
                var dominantDirection = -1
                for (i in diffs.indices) {
                    if (java.lang.Double.compare(diffs[i], 0.0) > 0) {
                        if (java.lang.Double.compare(diffs[i], max) > 0) {
                            max = diffs[i]
                            dominantDirection = i
                        }
                    }
                }

                // if none of pixel neighbours have a greater value, the result is 0
                if (dominantDirection == -1) {
                    histogram.put(0, histogram.get(0) + 1)
                    continue
                }

                // Calculating the binary pattern
                var binary_pattern = 0
                var power = 1
                for (i in dominantDirection until diffs.size) {
                    if (java.lang.Double.compare(diffs[i], 0.0) > 0) {
                        binary_pattern += power
                    }
                    power *= 2
                }
                for (i in 0 until dominantDirection) {
                    if (java.lang.Double.compare(diffs[i], 0.0) > 0) {
                        binary_pattern += power
                    }
                    power *= 2
                }

                // If binary pattern is uniform increase is hist value otherwise increase the NON_UNIFORM_BIN
                if (isBinaryUniform(convertToBinary(binary_pattern))) { // Key already exists, so increment its value
                    histogram.put(binary_pattern, histogram.get(binary_pattern) + 1)
                } else {
                    histogram.put(NON_UNIFORM_BIN, histogram.get(NON_UNIFORM_BIN) + 1)
                }
            }
        }
        val keyList = IntArrayList()
        val valueList = IntArrayList()
        histogram.pairsSortedByKey(keyList, valueList)
        return valueList.elements() // returning the 59 freq-values of the histogram
    }

    private fun isBinaryUniform(binary: IntArray): Boolean {
        var transitions = 0
        for (i in 0 until binary.size - 1) {
            if (binary[i] != binary[i + 1]) {
                transitions++
            }
        }
        return transitions <= 2
    }

    private fun convertToBinary(number: Int): IntArray {
        val binary = IntArray(8)
        var i = 7
        var num = number
        while (i >= 0) {
            binary[i] = num and 1
            i--
            num = num ushr 1
        }
        return binary
    }

    private fun getHistogramTemplate(): OpenIntIntHashMap { // Should be called only one time
        val histogram = OpenIntIntHashMap()
        histogram.put(NON_UNIFORM_BIN, 0) // Single bin for non-uniform patterns
        for (i in 0..255)  // Placing bins for uniform patterns separately. Map size should be 59
        {
            if (isBinaryUniform(convertToBinary(i))) {
                histogram.put(i, 0)
            }
        }
        return histogram
    }

    fun get_ULBP_Features(image: Mat): IntArray {
        // Using: R(Radius) = 1, P(Pixel Neighbours) = 8
        val histogram: OpenIntIntHashMap = histogramTemplate.clone() as OpenIntIntHashMap
        for (x in 1 until image.rows() - 1) {
            for (y in 1 until image.cols() - 1) {
                val diffs = IntArray(8)
                // Find the differences between the current center pixel and its neighbours
                val center = image[x, y][0] // Starting from the same row, right column pixel, clockwise
                diffs[0] = if (java.lang.Double.compare(image[x, y + 1][0], center) > 0) 1 else 0
                diffs[1] = if (java.lang.Double.compare(image[x + 1, y + 1][0], center) > 0) 1 else 0
                diffs[2] = if (java.lang.Double.compare(image[x + 1, y][0], center) > 0) 1 else 0
                diffs[3] = if (java.lang.Double.compare(image[x + 1, y - 1][0], center) > 0) 1 else 0
                diffs[4] = if (java.lang.Double.compare(image[x, y - 1][0], center) > 0) 1 else 0
                diffs[5] = if (java.lang.Double.compare(image[x - 1, y - 1][0], center) > 0) 1 else 0
                diffs[6] = if (java.lang.Double.compare(image[x - 1, y][0], center) > 0) 1 else 0
                diffs[7] = if (java.lang.Double.compare(image[x - 1, y + 1][0], center) > 0) 1 else 0
                var decimal = 0
                var power = 1
                for (i in diffs.indices) {
                    if (diffs[i] == 1) {
                        decimal += power
                    }
                    power *= 2
                }

                // If binary pattern is uniform increase is hist value otherwise increase the NON_UNIFORM_BIN
                if (isBinaryUniform(convertToBinary(decimal))) { // Key already exists, so increment its value
                    histogram.put(decimal, histogram.get(decimal) + 1)
                } else {
                    histogram.put(NON_UNIFORM_BIN, histogram.get(NON_UNIFORM_BIN) + 1)
                }
            }
        }
        val keyList = IntArrayList()
        val valueList = IntArrayList()
        histogram.pairsSortedByKey(keyList, valueList)
        return valueList.elements() // returning the 59 freq-values of the histogram
    }

}