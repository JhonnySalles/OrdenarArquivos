package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.entities.Sincronizacao
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class MockSincronizacao {

    fun mockEntity(): Sincronizacao {
        return Sincronizacao(LocalDateTime.now(), LocalDateTime.now())
    }

    fun mockEntities(): List<Sincronizacao> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Sincronizacao?) {
        assertNotNull(input)
        assertNotNull(input!!.envio)
        assertNotNull(input.recebimento)
    }

    fun assertsService(oldObj: Sincronizacao?, newObj: Sincronizacao?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.envio, newObj!!.envio)
        assertEquals(oldObj.recebimento, newObj.recebimento)
    }
}
