package com.fenix.ordenararquivos.process

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.service.MangaServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GerarBancoDadosTest : BaseTest() {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `deve processar pastas e salvar no banco de dados`() {
        // Preparar estrutura de pastas
        val rootDir = tempDir.toFile()
        val folders = listOf(
            "Manga Teste Volume 1 Capítulo 1 (A)",
            "Manga Teste Volume 1 Capítulo 1 (B)",
            "Manga Teste Volume 1 Capítulo 2"
        )
        folders.forEach { name ->
            val folder = File(rootDir, name)
            folder.mkdirs()
            // Criar arquivos dummy para testar contagem de quantidade
            File(folder, "pag01.jpg").createNewFile()
        }

        // Executar processamento
        GerarBancoDados.processar(rootDir.absolutePath)

        // Verificar resultados no banco (BaseTest usa :memory:)
        val service = MangaServices()
        val mangas = service.findAll("%")
        
        assertFalse(mangas.isEmpty(), "Deveria ter salvo ao menos um manga")
        
        val mangaNoBanco = mangas.find { it.nome.equals("Manga Teste", true) }
        assertNotNull(mangaNoBanco, "Manga 'Manga Teste' não localizado no banco")
        mangaNoBanco!!
        
        println("Debug DB Record: ID=${mangaNoBanco.id}, Nome='${mangaNoBanco.nome}', Vol='${mangaNoBanco.volume}', Cap='${mangaNoBanco.capitulo}', Caminhos=${mangaNoBanco.caminhos.size}")
        
        assertEquals(3, mangaNoBanco.caminhos.size, "Deveria ter 3 caminhos agregados (2 do cap1 + 1 do cap2)")
        assertEquals(3, mangaNoBanco.quantidade, "Deveria ter 3 arquivos no total (1 em cada pasta)")
    }
}
