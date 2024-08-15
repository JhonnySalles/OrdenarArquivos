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
                    if (copiaOpf(arquivo, destino))
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
            else if (arquivo.name.lowercase() == nome.lowercase())
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
            var ZE: ZipEntry? = ZiS.nextEntry
            while (ZE != null) {
                if (ZE.name.lowercase().endsWith(".opf")) {
                    val opf = File(origem.parent + File.separator + ZE.name.substringAfterLast('/'))
                    if (opf.exists())
                        opf.delete()

                    var FoS = FileOutputStream(opf)
                    var len: Int
                    while (ZiS.read(buffer).also { len = it } > 0) {
                        FoS.write(buffer, 0, len)
                    }
                    FoS.close()

                    compress(destino, FileInputStream(opf), ZipEntry(ZE.name))

                    if (opf.exists())
                        opf.delete()
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

    private fun compress(zip: File, opf: FileInputStream, newEntry : ZipEntry) {
        val tempFile = File.createTempFile(zip.name, null)
        tempFile.delete()
        zip.renameTo(tempFile)

        val buf = ByteArray(1024)
        val zin = ZipInputStream(FileInputStream(tempFile))
        val out = ZipOutputStream(FileOutputStream(zip))

        var locate = false
        var entry = zin.nextEntry
        while (entry != null) {
            val name = entry.name
            if (newEntry.name == name) {
                out.putNextEntry(newEntry)
                var len: Int
                while (opf.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
                locate = true
            } else {
                out.putNextEntry(ZipEntry(name))
                var len: Int
                while (zin.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            }
            entry = zin.nextEntry
        }

        if (!locate) {
            out.putNextEntry(newEntry)
            var len: Int
            while (opf.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        }

        zin.close()
        out.close()
        tempFile.delete()
        opf.close()
    }
}