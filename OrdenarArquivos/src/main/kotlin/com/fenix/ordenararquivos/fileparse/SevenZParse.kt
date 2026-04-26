package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.util.NaturalOrderComparator
import com.fenix.ordenararquivos.util.Utils
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer

class SevenZParse : Parse {
    private val mLOG = LoggerFactory.getLogger(SevenZParse::class.java)

    private var mEntrada: MutableList<SevenZEntry> = ArrayList()
    private lateinit var mLegendas: List<SevenZEntry>

    private class SevenZEntry(val entry: SevenZArchiveEntry, val bytes: ByteArray)

    @Throws(IOException::class)
    override fun parse(file: File) {
        val sevenZFile = SevenZFile(file)
        try {
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    continue
                }
                if (Utils.isImage(entry.name)) {
                    val content = ByteArray(entry.size.toInt())
                    sevenZFile.read(content)
                    mEntrada.add(SevenZEntry(entry, content))
                }
                entry = sevenZFile.nextEntry
            }
            Collections.sort(mEntrada, object : NaturalOrderComparator<SevenZEntry>() {
                override fun stringValue(o: SevenZEntry): String = o.entry.name
            }.thenComparing(object : NaturalOrderComparator<SevenZEntry>() {
                override fun stringValue(o: SevenZEntry): String = o.entry.name
            }))
        } finally {
            sevenZFile.close()
        }
    }

    override fun getSize(): Int = mEntrada.size

    @Throws(IOException::class)
    override fun getPagina(num: Int): InputStream = ByteArrayInputStream(mEntrada[num].bytes)

    override fun getTipo(): String = "tar"

    @Throws(IOException::class)
    override fun destroir() {
    }

    override fun getLegenda(): List<String> {
        val legendas: MutableList<String> = ArrayList()
        mLegendas.forEach(Consumer { it: SevenZEntry ->
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
            if (path.isNotEmpty() && !arquivos.containsKey(path))
                arquivos[path] = i
        }
        return arquivos
    }

    private fun getName(obj: SevenZEntry): String = obj.entry.name

    override fun getPaginaPasta(num: Int): String {
        return if (mEntrada.size < num) "" else getName(mEntrada[num])
    }

    override fun getPastas(): Map<String, Int> {
        val pastas: MutableMap<String, Int> = HashMap()
        for (i in mEntrada.indices) {
            val path: String = Utils.getPasta(getName(mEntrada[i]))
            if (path.isNotEmpty() && !pastas.containsKey(path)) pastas[path] = i
        }
        return pastas
    }
}