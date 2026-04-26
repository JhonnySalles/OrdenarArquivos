package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.util.Utils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.jvm.java
import kotlin.text.lowercase

class ParseFactory {
    companion object {
        private val mLOG = LoggerFactory.getLogger(ParseFactory::class.java)

        fun create(file: String): Parse {
            return create(File(file))
        }

        fun create(file: File): Parse {
            val parser: Parse
            val fileName = file.absolutePath.lowercase(Locale.getDefault())
            if (Utils.isRar(fileName))
                parser = RarParse()
            else if (Utils.isZip(fileName))
                parser = ZipParse()
            else if (Utils.isSevenZ(fileName))
                parser = SevenZParse()
            else if (Utils.isTarball(fileName))
                parser = TarParse()
            else
                throw kotlin.Exception("Tipo não implementado")

            return tryParse(parser, file)
        }

        private fun tryParse(parse: Parse, file: File): Parse {
            try {
                parse.parse(file)
            } catch (e: IOException) {
                mLOG.error(e.message, e)
                throw kotlin.Exception("Não foi possível abrir o arquivo")
            }
            return parse
        }
    }
}