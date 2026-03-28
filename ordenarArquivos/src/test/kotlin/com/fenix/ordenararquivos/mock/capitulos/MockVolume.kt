package com.fenix.ordenararquivos.mock.capitulos

import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import org.junit.jupiter.api.Assertions.*

class MockVolume {

    private val mockCapitulo = MockCapitulo()

    fun mockEntity(): Volume {
        return Volume(true, "volume1.cbz", 1.0, mockCapitulo.mockEntities().toMutableList(), "tags")
    }

    fun mockEntities(): List<Volume> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Volume?) {
        assertNotNull(input)
        assertEquals(1.0, input!!.volume)
        assertTrue(input.capitulos.isNotEmpty())
    }

    fun assertsService(oldObj: Volume?, newObj: Volume?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.volume, newObj!!.volume)
        assertEquals(oldObj.arquivo, newObj.arquivo)
    }
}
