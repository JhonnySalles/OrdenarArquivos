package com.fenix.ordenararquivos.process

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


object CopiarOpfEpub {

    private val LOG = LoggerFactory.getLogger(CopiarOpfEpub::class.java)

    fun processar(origem: String, destino: String) {
        if (origem.isEmpty()) {
            LOG.info("Necessário informar um caminho de origem para processar os arquivos.")
            return
        }

        if (destino.isEmpty()) {
            LOG.info("Necessário informar um caminho de destino para processar os arquivos.")
            return
        }

        var origem = File(origem)
        if (!origem.exists()) {
            LOG.error("Caminho de origem não localizado.")
            return
        }

        var destino = File(destino)
        if (!destino.exists()) {
            LOG.error("Caminho de destino não localizado.")
            return
        }

        processar(origem, destino)
        LOG.info("Processado com sucesso.")
    }

    private fun processar(origem: File, destino: File) {
        for (arquivo in origem.listFiles()!!) {
            if (arquivo.isDirectory)
                processar(arquivo, destino)
            else if (arquivo.extension == "zip" || arquivo.extension == "epub") {
                LOG.info("Processando arquivo " + arquivo.name + "...")
                val destino = getDestino(destino, arquivo.name)
                if (destino == null)
                    LOG.info("Arquivo de destino não encontrado.")
                else {
                    if (copiaOpf(origem, destino))
                        LOG.info("Arquivo processado. Origem: " + origem.name + ", Destno: " + destino.name)
                    else
                        LOG.info("Arquivo não processado. Origem: " + origem.name + ", Destno: " + destino.name)
                }
            }
        }
    }

    private fun getDestino(destino: File, nome: String): File? {
        var arq: File? = null
        for (arquivo in destino.listFiles()!!) {
            if (arquivo.isDirectory)
                arq = getDestino(arquivo, nome)
            else if (arquivo.name == nome)
                arq = arquivo

            if (arq != null)
                break
        }
        return arq
    }

    private fun copiaOpf(origem: File, destino: File): Boolean {
        var processado = false
        var FiS: FileInputStream
        // buffer to read and write data in the file
        // buffer to read and write data in the file
        val buffer = ByteArray(1024)
        try {
            FiS = FileInputStream(origem)
            val ZiS = ZipInputStream(FiS)
            var ZE: ZipEntry? = ZiS.getNextEntry()
            while (ZE != null) {
                if (ZE.name.lowercase().endsWith(".opf")) {
                    val opf = File(origem.parent + File.separator + ZE.name)
                    var FoS = FileOutputStream(opf)
                    var len: Int
                    while (ZiS.read(buffer).also { len = it } > 0) {
                        FoS.write(buffer, 0, len)
                    }
                    FoS.close()

                    FoS = FileOutputStream(destino)
                    val zipOut = ZipOutputStream(FoS)
                    val FiS = FileInputStream(opf)
                    zipOut.putNextEntry(ZipEntry(ZE.name))
                    val bytes = ByteArray(1024)
                    var length: Int
                    while (FiS.read(bytes).also { length = it } >= 0) {
                        zipOut.write(bytes, 0, length)
                    }
                    FiS.close()
                    processado = true
                    break
                }
                ZiS.closeEntry()
                ZE = ZiS.nextEntry
            }
            // close last ZipEntry
            ZiS.closeEntry()
            ZiS.close()
            FiS.close()
        } catch (e: IOException) {
            LOG.info("Erro ao processar o arquivo " + origem.name + ". Error: " + e.message)
        }
        return processado
    }
}