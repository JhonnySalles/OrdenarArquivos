package com.fenix.ordenararquivos.mock.comicinfo

import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerAuthor
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerMetadata
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerResult
import com.fenix.ordenararquivos.model.enums.TrackerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

class MockMal {

    fun mockEntity(): TrackerResult {
        val metadata = TrackerMetadata(
            id = 12345L,
            title = "Manga MAL Teste",
            alternativeTitles = listOf("Manga MAL Alt"),
            genres = listOf("Action"),
            authors = listOf(TrackerAuthor("Story & Art", "Test", "Author")),
            serialization = listOf("Jump"),
            url = "https://myanimelist.net/manga/12345",
            type = "manga",
            characters = "Test Char"
        )
        return TrackerResult(
            tracker = TrackerType.MYANIMELIST,
            id = 12345L,
            nome = "Manga MAL Teste",
            descricao = "Descricao MAL Teste",
            site = null,
            imagem = null,
            dados = metadata
        )
    }

    fun mockEntities(): List<TrackerResult> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: TrackerResult?) {
        assertNotNull(input)
        assertEquals(12345L, input!!.id)
        assertEquals("Manga MAL Teste", input.nome)
    }

    fun assertsService(oldObj: TrackerResult?, newObj: TrackerResult?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.id, newObj!!.id)
        assertEquals(oldObj.nome, newObj.nome)
    }
}
