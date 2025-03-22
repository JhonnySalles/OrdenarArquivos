package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.Caminhos
import org.junit.jupiter.api.Assertions.*


class MockCaminhos : MockBase<Long?, Caminhos>() {

    override fun mockEntity(): Caminhos = mockEntity(null)

    override fun randomId(): Long? = (1L..100L).random()

    override fun updateEntity(input: Caminhos): Caminhos {
        return Caminhos(input.id, null, "002", 20, "20", "Capitulo Teste 002")
    }

    override fun mockEntity(id: Long?): Caminhos {
        return Caminhos(id ?: 0, null, "001", 10, "10", "Capitulo Teste 001")
    }

    fun mockEntities(): MutableList<Caminhos> {
        return mutableListOf(mockEntity(0), updateEntity(mockEntity(0)))
    }

    override fun assertsService(input: Caminhos?) {
        assertNotNull(input)
        assertNotNull(input!!.id)

        assertTrue(input.capitulo.isNotEmpty())
        assertTrue(input.numero == 10 || input.numero == 20)
        assertTrue(input.numeroPagina == "10" || input.numeroPagina == "20")
        assertTrue(input.nomePasta.isNotEmpty())
    }

    override fun assertsService(oldObj: Caminhos?, newObj: Caminhos?) {
        assertsService(oldObj)
        assertsService(newObj)

        assertEquals(oldObj!!.capitulo, newObj!!.capitulo)
        assertEquals(oldObj.numero, newObj.numero)
        assertEquals(oldObj.numeroPagina, newObj.numeroPagina)
        assertEquals(oldObj.nomePasta, newObj.nomePasta)
    }

}