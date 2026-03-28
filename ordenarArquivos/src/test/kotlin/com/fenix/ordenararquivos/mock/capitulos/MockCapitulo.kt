package com.fenix.ordenararquivos.mock.capitulos

import com.fenix.ordenararquivos.model.entities.capitulos.Capitulo
import org.junit.jupiter.api.Assertions.*

class MockCapitulo {

    fun mockEntity(): Capitulo {
        return Capitulo(1.0, "Chapter", "Capitulo")
    }

    fun mockEntities(): List<Capitulo> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Capitulo?) {
        assertNotNull(input)
        assertEquals(1.0, input!!.capitulo)
    }

    fun assertsService(oldObj: Capitulo?, newObj: Capitulo?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.capitulo, newObj!!.capitulo)
    }
}
