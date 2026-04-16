package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Manga
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MangaServicesTest : BaseTest() {

    private lateinit var mService: MangaServices

    @BeforeEach
    fun setUp() {
        mService = MangaServices()
        // Limpar tabelas antes de cada teste para garantir isolamento
        val conn = com.fenix.ordenararquivos.database.DataBase.instancia
        conn.createStatement().execute("DELETE FROM Caminho")
        conn.createStatement().execute("DELETE FROM Manga")
    }

    @Test
    fun testSaveAndFind() {
        val manga = Manga(
            nome = "Beserk",
            volume = "01",
            capitulo = "001",
            arquivo = "berserk_v01.zip",
            quantidade = 200,
            capitulos = "1-3"
        )
        manga.addCaminhos(Caminhos(capitulo = "001", numero = "1", nomePasta = "paginas", tag = "raw"))

        val initialSize = SincronizacaoServices.sincManga.size
        mService.save(manga)
        
        assertTrue(manga.id > 0, "ID do manga deve ser gerado após save")
        assertEquals(initialSize + 1, SincronizacaoServices.sincManga.size, "Deve ter adicionado o manga na lista de sincronização")

        val found = mService.find("Beserk", "01", "001")
        assertNotNull(found, "Deve encontrar o manga salvo")
        assertEquals("Beserk", found?.nome)
        assertEquals(1, found?.caminhos?.size, "Deve carregar os caminhos associados")
        assertEquals("paginas", found?.caminhos?.get(0)?.nomePasta)
    }

    @Test
    fun testUpdate() {
        val manga = Manga(nome = "One Piece", volume = "01", capitulo = "001")
        mService.save(manga, isSendCloud = false)
        val originalId = manga.id

        manga.quantidade = 100
        manga.arquivo = "onepiece_new.rar"
        mService.save(manga, isSendCloud = false)

        val updated = mService.find(manga)
        assertEquals(originalId, updated?.id)
        assertEquals(100, updated?.quantidade)
        assertEquals("onepiece_new.rar", updated?.arquivo)
    }

    @Test
    fun testFindAnterior() {
        // Salvar volume 1 e volume 2
        mService.save(Manga(nome = "Bleach", volume = "01", capitulo = "001"), isSendCloud = false)
        mService.save(Manga(nome = "Bleach", volume = "02", capitulo = "010"), isSendCloud = false)

        // Buscar anterior ao "Bleach" sem especificar volume (pega o maior volume disponível para aquele nome)
        // A lógica do selectAnterior é: nome LIKE ? AND capitulo LIKE ? ORDER BY volume DESC LIMIT 1
        val anterior = mService.find("Bleach", "", "010", anterior = true)
        assertNotNull(anterior)
        assertEquals("02", anterior?.volume)
    }

    @Test
    fun testFindAll() {
        mService.save(Manga(nome = "Naruto", volume = "01", capitulo = "001"), isSendCloud = false)
        mService.save(Manga(nome = "Naruto", volume = "02", capitulo = "010"), isSendCloud = false)
        mService.save(Manga(nome = "Boruto", volume = "01", capitulo = "001"), isSendCloud = false)

        val narutos = mService.findAll("Naruto")
        assertEquals(2, narutos.size)
        
        val all = mService.findAll("%")
        assertEquals(3, all.size)
    }

    @Test
    fun testFindEnvio() {
        val oldDate = LocalDateTime.now().minusDays(1)
        val now = LocalDateTime.now()
        
        mService.save(Manga(nome = "Recent"), isSendCloud = false, atualizacao = now)
        
        val list = mService.findEnvio(now.minusSeconds(10))
        assertEquals(1, list.size)
        assertEquals("Recent", list[0].nome)

        val emptyList = mService.findEnvio(now.plusMinutes(1))
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun testListar() {
        mService.save(Manga(nome = "Zelda"), isSendCloud = false)
        mService.save(Manga(nome = "Zelda"), isSendCloud = false) // Mesmo nome, volume diferente no banco real
        mService.save(Manga(nome = "Metroid"), isSendCloud = false)

        val nomes = mService.listar()
        assertEquals(2, nomes.size, "Deve agrupar por nome")
        assertTrue(nomes.contains("Zelda"))
        assertTrue(nomes.contains("Metroid"))
        assertEquals("Metroid", nomes[0], "Deve estar ordenado")
    }

    @Test
    fun testFindAllPaginated() {
        for (i in 1..10) {
            val name = "Manga ${String.format("%02d", i)}"
            mService.save(Manga(nome = name, volume = "$i", capitulo = "001"), isSendCloud = false)
        }

        // Limit 3, Offset 0
        val page1 = mService.findAll("%", 3, 0)
        assertEquals(3, page1.size)
        assertEquals("Manga 01", page1[0].nome)

        // Limit 3, Offset 3
        val page2 = mService.findAll("%", 3, 3)
        assertEquals(3, page2.size)
        assertEquals("Manga 04", page2[0].nome)
    }

    @Test
    fun testFindWithComicJoin() {
        val manga = Manga(nome = "Dragon Ball", volume = "01", capitulo = "001")
        mService.save(manga, isSendCloud = false)

        val comicInfo = com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo().apply {
            series = "Dragon Ball"
            comic = "DB-Z"
        }
        ComicInfoServices().save(comicInfo)

        val found = mService.findAll("Dragon Ball", 1, 0)
        assertEquals(1, found.size)
        assertEquals("DB-Z", found[0].comic, "Deve carregar o nome do comic via JOIN")
    }

    @Test
    fun testDeleteMangaCascade() {
        val manga1 = Manga(nome = "Bleach", volume = "01", capitulo = "001")
        val manga2 = Manga(nome = "Bleach", volume = "02", capitulo = "002")
        mService.save(manga1, isSendCloud = false)
        mService.save(manga2, isSendCloud = false)

        val comicInfo = com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo().apply {
            series = "Bleach"
            comic = "Shiniga-mi"
            languageISO = "ja"
        }
        val ciService = ComicInfoServices()
        ciService.save(comicInfo)

        // Caso 1: Deletar um volume, mas outro existe. ComicInfo deve permanecer.
        mService.deleteManga(manga1)
        assertNull(mService.find(manga1.id), "Manga 1 deve ser deletado")
        assertNotNull(ciService.find("Bleach", "ja"), "ComicInfo deve permanecer pois manga 2 ainda existe")

        // Caso 2: Deletar o último volume. ComicInfo deve ser deletado.
        mService.deleteManga(manga2)
        assertNull(mService.find(manga2.id), "Manga 2 deve ser deletado")
        assertNull(ciService.find("Bleach", "ja"), "ComicInfo deve ser deletado pois era o último manga")
    }

    @Test
    fun testSugestao() {
        mService.save(Manga(nome = "Attack on Titan"), isSendCloud = false)
        mService.save(Manga(nome = "Solo Leveling"), isSendCloud = false)

        val sugestoes = mService.sugestao("attack")
        assertEquals(1, sugestoes.size)
        assertEquals("Attack on Titan", sugestoes[0])
        
        val multi = mService.sugestao("o")
        assertEquals(2, multi.size, "Ambos contém 'o'")
    }
}