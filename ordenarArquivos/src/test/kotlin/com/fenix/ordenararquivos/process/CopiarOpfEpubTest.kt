package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.BaseTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class CopiarOpfEpubTest : BaseTest() {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `deve copiar arquivo opf de um epub para outro com mesmo nome`() {
        val pastaOrigem = File(tempDir.toFile(), "origem").apply { mkdir() }
        val pastaDestino = File(tempDir.toFile(), "destino").apply { mkdir() }

        val nomeArquivo = "livro.epub"
        val arquivoOrigem = File(pastaOrigem, nomeArquivo)
        val arquivoDestino = File(pastaDestino, nomeArquivo)

        // Criar ZIP de origem com metadata.opf
        ZipOutputStream(FileOutputStream(arquivoOrigem)).use { zos ->
            zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zos.write("conteudo original".toByteArray())
            zos.closeEntry()
        }

        // Criar ZIP de destino sem o opf (ou com outro conteudo)
        ZipOutputStream(FileOutputStream(arquivoDestino)).use { zos ->
            zos.putNextEntry(ZipEntry("dummy.txt"))
            zos.write("vazio".toByteArray())
            zos.closeEntry()
        }

        // Processar
        CopiarOpfEpub.processar(pastaOrigem.absolutePath, pastaDestino.absolutePath)

        // Verificar se arquivoDestino agora tem o content.opf
        ZipFile(arquivoDestino).use { zip ->
            val entry = zip.getEntry("OEBPS/content.opf")
            assertNotNull(entry, "O arquivo de destino deveria conter o content.opf agora")
            zip.getInputStream(entry).use { it.bufferedReader().readText() }.also {
                assertEquals("conteudo original", it)
            }
        }
    }
}
