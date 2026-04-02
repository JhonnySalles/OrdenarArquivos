package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.model.entities.Pasta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import java.io.File

class MockPasta {

    fun mockEntity(): Pasta {
        return Pasta(File("test/path"), "arquivo.cbz", "Test Manga", 1.0f, 1.0f, "1-5", "Scan", "Title", false)
    }

    fun mockEntities(): List<Pasta> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Pasta?) {
        assertNotNull(input)
        assertEquals("Test Manga", input!!.nome)
        assertEquals(1.0f, input.volume)
    }

    fun assertsService(oldObj: Pasta?, newObj: Pasta?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.nome, newObj!!.nome)
        assertEquals(oldObj.volume, newObj.volume)
    }
}
