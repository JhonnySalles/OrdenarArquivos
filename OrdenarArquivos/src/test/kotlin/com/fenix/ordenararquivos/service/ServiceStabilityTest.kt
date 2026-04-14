package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.Caminhos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ServiceStabilityTest : BaseTest() {

    private lateinit var mangaService: MangaServices

    @BeforeEach
    fun setup() {
        mangaService = MangaServices()
        // Limpar banco para garantir testes independentes
        com.fenix.ordenararquivos.database.DataBase.instancia.createStatement().executeUpdate("DELETE FROM Manga")
        com.fenix.ordenararquivos.database.DataBase.instancia.createStatement().executeUpdate("DELETE FROM Caminho")
    }

    @Test
    fun testSaveAndRetrieveManga() {
        val manga = Manga().apply {
            nome = "Bleach"
            volume = "01"
            capitulo = "001"
            arquivo = "bleach_01.cbr"
            quantidade = 20
        }
        
        mangaService.save(manga, isSendCloud = false)
        assertNotEquals(0L, manga.id, "ID do manga deve ser gerado após o save")

        val retrieved = mangaService.find("Bleach", "01", "001")
        assertNotNull(retrieved)
        assertEquals("Bleach", retrieved?.nome)
        assertEquals(20, retrieved?.quantidade)
    }

    @Test
    fun testUpdateExistingManga() {
        val manga = Manga().apply {
            nome = "Naruto"
            volume = "01"
            capitulo = "001"
        }
        mangaService.save(manga, isSendCloud = false)
        val originalId = manga.id

        // Atualizar
        manga.quantidade = 50
        mangaService.save(manga, isSendCloud = false)

        val updated = mangaService.find("Naruto", "01", "001")
        assertEquals(originalId, updated?.id)
        assertEquals(50, updated?.quantidade)
    }

    @Test
    fun testMangaWithCaminhosPersistence() {
        val manga = Manga().apply {
            nome = "Hunter x Hunter"
            volume = "36"
            capitulo = "381"
        }
        val caminhos = arrayListOf(
            Caminhos("381", "1", "folder1", "p01"),
            Caminhos("381", "2", "folder1", "p02")
        )
        manga.caminhos = caminhos
        
        mangaService.save(manga, isSendCloud = false)
        
        val retrieved = mangaService.find("Hunter x Hunter", "36", "381")
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.caminhos?.size, "Deveria ter persistido 2 caminhos")
    }

    @Test
    fun testFindNonExistentManga() {
        val result = mangaService.find("Inexistente", "99", "999")
        assertNull(result)
    }

    @Test
    fun testListarUniqueNames() {
        val m1 = Manga().apply { nome = "One Piece"; volume = "01"; capitulo = "001" }
        val m2 = Manga().apply { nome = "One Piece"; volume = "02"; capitulo = "010" }
        val m3 = Manga().apply { nome = "Dragon Ball"; volume = "01"; capitulo = "001" }
        
        mangaService.save(m1, isSendCloud = false)
        mangaService.save(m2, isSendCloud = false)
        mangaService.save(m3, isSendCloud = false)
        
        val nomes = mangaService.listar()
        assertEquals(2, nomes.size)
        assertTrue(nomes.contains("One Piece"))
        assertTrue(nomes.contains("Dragon Ball"))
    }
}
