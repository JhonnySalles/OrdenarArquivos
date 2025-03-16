package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.Capa
import com.fenix.ordenararquivos.model.TipoCapa
import org.junit.jupiter.api.Assertions.*


class MockCapa : MockBase<String?, Capa>() {

    override fun mockEntity(): Capa = mockEntity(null)

    override fun randomId(): String? = ""

    override fun updateEntity(input: Capa): Capa = updateEntityById("")

    override fun updateEntityById(lastId: String?): Capa {
        return Capa("Capa" + "---", "Aquivo" + "---", TipoCapa.CAPA_COMPLETA, true)
    }

    override fun mockEntity(id: String?): Capa {
        return Capa("Capa", "Arquivo", TipoCapa.CAPA, false)
    }

    override fun assertsService(input: Capa?) {
        assertNotNull(input)
        assertTrue(input!!.nome.isNotEmpty())
        assertTrue(input.arquivo.isNotEmpty())
        assertTrue(input.tipo == TipoCapa.CAPA)
        assertFalse(input.isDupla)
    }

    override fun assertsService(oldObj: Capa?, newObj: Capa?) {
        assertsService(oldObj)
        assertsService(newObj)

        assertEquals(oldObj!!.nome, newObj!!.nome)
        assertEquals(oldObj.arquivo, newObj.arquivo)
        assertEquals(oldObj.tipo, newObj.tipo)
        assertEquals(oldObj.isDupla, newObj.isDupla)
    }

}