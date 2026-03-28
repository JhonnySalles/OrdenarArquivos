package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.entities.Capa
import com.fenix.ordenararquivos.model.enums.TipoCapa
import org.junit.jupiter.api.Assertions.*

class MockCapa {

    fun mockEntity(): Capa {
        return Capa("Capa Teste", "capa.jpg", TipoCapa.CAPA, false)
    }

    fun mockEntities(): List<Capa> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Capa?) {
        assertNotNull(input)
        assertTrue(input!!.nome.isNotEmpty())
        assertTrue(input.arquivo.isNotEmpty())
    }

    fun assertsService(oldObj: Capa?, newObj: Capa?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.nome, newObj!!.nome)
        assertEquals(oldObj.arquivo, newObj.arquivo)
        assertEquals(oldObj.tipo, newObj.tipo)
    }
}
