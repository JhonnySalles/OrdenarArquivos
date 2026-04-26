package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.util.Utils
import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import com.github.junrar.rarfile.FileHeader
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer

class RarParse : Parse {
    private val mLOG = LoggerFactory.getLogger(RarParse::class.java)

    private lateinit var mArquivo: Archive
    private lateinit var mPastaCache: File
    private val mCabecalhos = ArrayList<FileHeader>()
    private val mLegendas = ArrayList<FileHeader>()

    @Throws(IOException::class)
    override fun parse(file: File) {
        mArquivo = try {
            Archive(file)
        } catch (e: RarException) {
            mLOG.error(e.message, e)
            throw IOException("unable to open archive")
        }
        var cabecalho = mArquivo.nextFileHeader()
        while (cabecalho != null) {
            if (!cabecalho.isDirectory) {
                val name = getName(cabecalho)
                if (Utils.isImage(name)) {
                    mCabecalhos.add(cabecalho)
                }
            }
            cabecalho = mArquivo.nextFileHeader()
        }
        Collections.sort(mCabecalhos, Comparator<FileHeader> { a, b -> Utils.getCaminho(getName(a)).compareTo(Utils.getCaminho(getName(b))) }
            .thenComparing { a, b -> Utils.getNomeNormalizadoOrdenacao(getName(a)).compareTo(Utils.getNomeNormalizadoOrdenacao(getName(b))) })
    }

    override fun getSize(): Int = mCabecalhos.size

    @Throws(IOException::class)
    override fun getPagina(num: Int): InputStream {
        return try {
            val cabecalho = mCabecalhos[num]
            if (this::mPastaCache.isInitialized) {
                val name = getName(cabecalho)
                val cacheFile = File(mPastaCache, Utils.MD5(name))

                if (cacheFile.exists())
                    return FileInputStream(cacheFile)

                synchronized(this) {
                    if (!cacheFile.exists()) {
                        val os = FileOutputStream(cacheFile)
                        try {
                            mArquivo.extractFile(cabecalho, os)
                        } catch (e: Exception) {
                            cacheFile.delete()
                            throw e
                        } finally {
                            os.close()
                        }
                    }
                }
                FileInputStream(cacheFile)
            } else
                mArquivo.getInputStream(cabecalho)
        } catch (e: RarException) {
            throw IOException("unable to parse rar")
        }
    }

    @Throws(IOException::class)
    override fun destroir() {
        if (this::mPastaCache.isInitialized) {
            for (f in mPastaCache.listFiles()) {
                f.delete()
            }
            mPastaCache.delete()
        }
        mArquivo.close()
    }

    override fun getTipo(): String = "rar"

    fun setCacheDirectory(cacheDirectory: File) {
        mPastaCache = cacheDirectory
        if (!mPastaCache.exists()) mPastaCache.mkdirs()
        if (mPastaCache.listFiles() != null) {
            for (f in mPastaCache.listFiles()) f.delete()
        }
    }

    override fun getLegenda(): List<String> {
        val legendas: MutableList<String> = ArrayList()
        mLegendas.forEach(Consumer { it: FileHeader ->
            val sub: InputStream
            val reader: BufferedReader
            try {
                sub = mArquivo.getInputStream(it)
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

    private fun getName(header: FileHeader): String = header.fileName

    override fun getPaginaPasta(num: Int): String {
        return if (mCabecalhos.size < num) "" else getName(mCabecalhos[num])
    }

    override fun getPastas(): Map<String, Int> {
        val pastas: MutableMap<String, Int> = HashMap()
        for (i in mCabecalhos.indices) {
            val path: String = Utils.getPasta(getName(mCabecalhos[i]))
            if (path.isNotEmpty() && !pastas.containsKey(path))
                pastas[path] = i
        }
        return pastas
    }
}