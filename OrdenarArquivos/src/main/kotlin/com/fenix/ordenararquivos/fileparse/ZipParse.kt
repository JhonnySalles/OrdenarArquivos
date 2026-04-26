package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.util.Utils
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipParse : Parse {

    private val mLOG = LoggerFactory.getLogger(ZipParse::class.java)

    private lateinit var mArquivoZip: ZipFile
    private var mEntrada: ArrayList<ZipEntry> = ArrayList()
    private lateinit var mLegendas: ArrayList<ZipEntry>

    @Throws(IOException::class)
    override fun parse(file: File) {
        mArquivoZip = ZipFile(file.absolutePath)
        val e = mArquivoZip.entries()
        while (e.hasMoreElements()) {
            val ze = e.nextElement()
            if (!ze.isDirectory && Utils.isImage(ze.name)) {
                mEntrada.add(ze)
            }
        }
        Collections.sort(mEntrada, Comparator<ZipEntry> { a, b -> Utils.getCaminho(a.name).compareTo(Utils.getCaminho(b.name)) }
            .thenComparing { a, b -> Utils.getNomeNormalizadoOrdenacao(a.name).compareTo(Utils.getNomeNormalizadoOrdenacao(b.name)) })
    }

    override fun getSize(): Int = mEntrada.size

    @Throws(IOException::class)
    override fun getPagina(num: Int): InputStream = mArquivoZip.getInputStream(mEntrada[num])

    override fun getTipo(): String = "zip"

    @Throws(IOException::class)
    override fun destroir() = mArquivoZip.close()

    override fun getLegenda(): List<String> {
        val legendas: MutableList<String> = ArrayList()
        mLegendas.forEach(Consumer { it: ZipEntry ->
            val sub: InputStream
            val reader: BufferedReader
            try {
                sub = mArquivoZip.getInputStream(it)
                reader = BufferedReader(InputStreamReader(sub, StandardCharsets.UTF_8))
                val content = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    content.append(line)
                    line = reader.readLine()
                }
                legendas.add(content.toString())
            } catch (e: IOException) {
                mLOG.error(e.message, e)
            }
        })
        return legendas
    }

    override fun getLegendaNomes(): Map<String, Int> {
        val arquivos: MutableMap<String, Int> = HashMap()
        for (i in mLegendas.indices) {
            val path: String = Utils.getNome(getName(mLegendas[i]))
            if (path.isNotEmpty() && !arquivos.containsKey(path))
                arquivos[path] = i
        }
        return arquivos
    }

    private fun getName(entry: ZipEntry): String = entry.name

    override fun getPaginaPasta(num: Int): String {
        return if (mEntrada.size < num) "" else getName(mEntrada[num])
    }

    override fun getPastas(): Map<String, Int> {
        val pastas: MutableMap<String, Int> = HashMap()
        for (i in mEntrada.indices) {
            val path: String = Utils.getPasta(getName(mEntrada[i]))
            if (path.isNotEmpty() && !pastas.containsKey(path))
                pastas[path] = i
        }
        return pastas
    }
}