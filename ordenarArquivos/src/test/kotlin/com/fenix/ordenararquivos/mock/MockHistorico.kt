package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.mock.comicinfo.MockComicInfo
import com.fenix.ordenararquivos.mock.comicinfo.MockMal
import com.fenix.ordenararquivos.model.entities.Historico
import org.junit.jupiter.api.Assertions.*

class MockHistorico {

    private val mockManga = MockManga()
    private val mockComicInfo = MockComicInfo()
    private val mockCaminhos = MockCaminhos()
    private val mockCapa = MockCapa()
    private val mockMal = MockMal()

    fun mockEntity(): Historico {
        return Historico(
            nome = "Historico Teste",
            pastaOrigem = "Origem",
            pastaDestino = "Destino",
            nomeManga = "Manga Teste",
            volume = "01",
            nomeArquivo = "arquivo.cbz",
            pastaCapitulo = "Capitulo",
            inicio = "1",
            fim = "10",
            importar = "Sim",
            selecionado = "Sim",
            manga = mockManga.mockEntity(),
            comicInfo = mockComicInfo.mockEntity(),
            caminhos = mockCaminhos.mockEntities(),
            itens = listOf("item1", "item2"),
            capas = mockCapa.mockEntities(),
            mal = mockMal.mockEntities()
        )
    }

    fun mockEntities(): List<Historico> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Historico?) {
        assertNotNull(input)
        assertEquals("Historico Teste", input!!.nome)
        mockManga.assertsService(input.manga)
        mockComicInfo.assertsService(input.comicInfo)
        mockCaminhos.assertsService(input.caminhos[0])
    }

    fun assertsService(oldObj: Historico?, newObj: Historico?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.nome, newObj!!.nome)
        mockManga.assertsService(oldObj.manga, newObj.manga)
        mockComicInfo.assertsService(oldObj.comicInfo, newObj.comicInfo)
    }
}
