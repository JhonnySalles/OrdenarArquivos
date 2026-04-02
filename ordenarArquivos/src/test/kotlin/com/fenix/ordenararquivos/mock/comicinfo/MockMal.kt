package com.fenix.ordenararquivos.mock.comicinfo

import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import dev.katsute.mal4j.manga.Manga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.mockito.kotlin.mock

class MockMal {

    fun mockEntity(): Mal {
        val mockManga = mock<Manga>()
        return Mal(12345L, "Manga MAL Teste", "Descricao MAL Teste", null, null, mockManga)
    }

    fun mockEntities(): List<Mal> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Mal?) {
        assertNotNull(input)
        assertEquals(12345L, input!!.id)
        assertEquals("Manga MAL Teste", input.nome)
    }

    fun assertsService(oldObj: Mal?, newObj: Mal?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.id, newObj!!.id)
        assertEquals(oldObj.nome, newObj.nome)
    }
}
