package com.fenix.ordenararquivos.mock

import com.fenix.ordenararquivos.mock.comicinfo.MockComicInfo
import com.fenix.ordenararquivos.model.entities.Processar
import org.junit.jupiter.api.Assertions.*
import java.io.File

class MockProcessar {

    private val mockComicInfo = MockComicInfo()

    fun mockEntity(): Processar {
        return Processar(
            arquivo = "arquivo.cbz",
            tags = "tags",
            file = File("test/path"),
            comicInfo = mockComicInfo.mockEntity(),
            isProcessado = false
        )
    }

    fun mockEntities(): List<Processar> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Processar?) {
        assertNotNull(input)
        assertEquals("arquivo.cbz", input!!.arquivo)
        mockComicInfo.assertsService(input.comicInfo)
    }

    fun assertsService(oldObj: Processar?, newObj: Processar?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.arquivo, newObj!!.arquivo)
        mockComicInfo.assertsService(oldObj.comicInfo, newObj.comicInfo)
    }
}
