package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.Caminhos
import org.junit.jupiter.api.Assertions.*


class MockCaminhos : MockBase<Long?, Caminhos>() {

    override fun mockEntity(): Caminhos = mockEntity(null)

    override fun randomId(): Long? = (1L..100L).random()

    override fun updateEntity(input: Caminhos): Caminhos = updateEntityById(input.id)

    override fun updateEntityById(lastId: Long?): Caminhos {
        return Caminhos(lastId ?: 0, null, "Capitulo" + "---", 20, "20", "Pasta" + "---")
    }

    override fun mockEntity(id: Long?): Caminhos {
        return Caminhos(id ?: 0, null, "Capitulo", 10, "10", "Pasta")
    }

    override fun assertsService(input: Caminhos?) {
        assertNotNull(input)
        assertNotNull(input!!.id)

        assertTrue(input.capitulo.isNotEmpty())
        assertTrue(input.numero == 10)
        assertTrue(input.numeroPagina == "10")
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