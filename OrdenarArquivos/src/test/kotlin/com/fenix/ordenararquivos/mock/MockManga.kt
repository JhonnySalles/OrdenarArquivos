package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.Manga
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime


class MockManga : MockBase<Long?, Manga>() {

    override fun mockEntity(): Manga = mockEntity(null)

    override fun randomId(): Long? = (1L..100L).random()

    override fun updateEntity(input: Manga): Manga = updateEntityById(input.id)

    override fun updateEntityById(lastId: Long?): Manga {
        return Manga(lastId ?: 0, "Manga" + "---", "Volume" + "---", "Capitulo" + "---", "Aquivo" + "---", 10, "Capitulos" + "---", LocalDateTime.now())
    }

    override fun mockEntity(id: Long?): Manga {
        return Manga(id ?: 0, "Manga", "Volume", "Capitulo", "Arquivo", "Capitulos", 0)
    }

    override fun assertsService(input: Manga?) {
        assertNotNull(input)
        assertNotNull(input!!.id)

        assertTrue(input.nome.isNotEmpty())
        assertTrue(input.volume.isNotEmpty())
        assertTrue(input.capitulo.isNotEmpty())
        assertTrue(input.arquivo.isNotEmpty())
        assertTrue(input.capitulos.isNotEmpty())
        assertTrue(input.quantidade == 0)
    }

    override fun assertsService(oldObj: Manga?, newObj: Manga?) {
        assertsService(oldObj)
        assertsService(newObj)

        assertEquals(oldObj!!.nome, newObj!!.nome)
        assertEquals(oldObj.volume, newObj.volume)
        assertEquals(oldObj.capitulo, newObj.capitulo)
        assertEquals(oldObj.arquivo, newObj.arquivo)
        assertEquals(oldObj.capitulos, newObj.capitulos)
        assertEquals(oldObj.quantidade, newObj.quantidade)
    }

}