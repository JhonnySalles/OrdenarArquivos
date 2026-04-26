package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.util.NaturalOrderComparator
import com.fenix.ordenararquivos.util.Utils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer

class TarParse : Parse {

    private val mLOG = LoggerFactory.getLogger(TarParse::class.java)

    private var mEntradas: MutableList<TarEntry> = ArrayList()
    private lateinit var mLegendas: List<TarEntry>

    private class TarEntry(val entry: TarArchiveEntry, val bytes: ByteArray)

    @Throws(IOException::class)
    override fun parse(file: File) {
        val fis = BufferedInputStream(FileInputStream(file))
        val `is` = TarArchiveInputStream(fis)
        var entry = `is`.nextTarEntry
        while (entry != null) {
            if (entry.isDirectory)
                continue

            if (Utils.isImage(entry.name))
                mEntradas.add(TarEntry(entry, Utils.toByteArray(`is`)))

            entry = `is`.nextTarEntry
        }
        Collections.sort(mEntradas, object : NaturalOrderComparator<TarEntry>() {
            override fun stringValue(o: TarEntry): String = o.entry.name
        }.thenComparing(object : NaturalOrderComparator<TarEntry>() {
            override fun stringValue(o: TarEntry): String = o.entry.name
        }))
    }

    override fun getSize(): Int = mEntradas.size

    @Throws(IOException::class)
    override fun getPagina(num: Int): InputStream = ByteArrayInputStream(mEntradas[num].bytes)

    override fun getTipo(): String = "tar"

    @Throws(IOException::class)
    override fun destroir() {
    }

    override fun getLegenda(): List<String> {
        val legendas: MutableList<String> = ArrayList()
        mLegendas.forEach(Consumer { it: TarEntry ->
            val sub: InputStream = ByteArrayInputStream(it.bytes)
            val reader: BufferedReader
            try {
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
            if (path.isNotEmpty() && !arquivos.containsKey(path)) arquivos[path] = i
        }
        return arquivos
    }

    private fun getName(obj: TarEntry): String = obj.entry.name

    override fun getPaginaPasta(num: Int): String {
        return if (mEntradas.size < num) "" else getName(mEntradas[num])
    }

    override fun getPastas(): Map<String, Int> {
        val pastas: MutableMap<String, Int> = HashMap()
        for (i in mEntradas.indices) {
            val path: String = Utils.getPasta(getName(mEntradas[i]))
            if (path.isNotEmpty() && !pastas.containsKey(path)) pastas[path] = i
        }
        return pastas
    }
}