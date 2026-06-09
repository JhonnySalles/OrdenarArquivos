package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.process.Winrar
import com.fenix.ordenararquivos.util.Utils
import com.github.junrar.exception.UnsupportedRarV5Exception
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*

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

        private fun tryParse(parse: Parse, file: File, tentarConversao: Boolean = true): Parse {
            try {
                parse.parse(file)
            } catch (e: IOException) {
                if (tentarConversao && parse is RarParse && e.cause is UnsupportedRarV5Exception) {
                    mLOG.warn("Tentando conversão RAR v5 → v4: {}", file.absolutePath)
                    if (Winrar.converterRar5ParaRar4(file))
                        return tryParse(parse, file, tentarConversao = false)
                    mLOG.error("Conversão RAR v5 → v4 falhou: {}", file.absolutePath)
                } else {
                    mLOG.error(e.message, e)
                }
                throw kotlin.Exception("Não foi possível abrir o arquivo", e)
            }
            return parse
        }
    }
}