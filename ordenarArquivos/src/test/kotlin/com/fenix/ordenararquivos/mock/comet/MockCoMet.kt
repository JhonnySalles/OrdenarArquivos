package com.fenix.ordenararquivos.mock.comet

import com.fenix.ordenararquivos.model.entities.comet.CoMet
import org.junit.jupiter.api.Assertions.*

class MockCoMet {

    fun mockEntity(): CoMet {
        return CoMet(
            title = "Title CoMet Test",
            description = "Description CoMet Test",
            series = "Series CoMet Test",
            issue = 1,
            volume = 1,
            publisher = "Publisher CoMet Test",
            language = "en"
        )
    }

    fun mockEntities(): List<CoMet> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: CoMet?) {
        assertNotNull(input)
        assertEquals("Title CoMet Test", input!!.title)
    }

    fun assertsService(oldObj: CoMet?, newObj: CoMet?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.title, newObj!!.title)
        assertEquals(oldObj.series, newObj.series)
    }
}
