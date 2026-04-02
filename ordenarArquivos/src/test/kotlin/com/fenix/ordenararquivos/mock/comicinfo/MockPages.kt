package com.fenix.ordenararquivos.mock.comicinfo

import com.fenix.ordenararquivos.model.entities.comicinfo.ComicPageType
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

class MockPages {

    fun mockEntity(): Pages {
        return Pages(
            bookmark = "Bookmark",
            image = 1,
            imageHeight = 1000,
            imageWidth = 700,
            imageSize = 500000L,
            type = ComicPageType.FrontCover,
            doublePage = false,
            key = "key123"
        )
    }

    fun mockEntities(): List<Pages> {
        return listOf(mockEntity(), mockEntity())
    }

    fun assertsService(input: Pages?) {
        assertNotNull(input)
        assertEquals("Bookmark", input!!.bookmark)
        assertEquals(1, input.image)
    }

    fun assertsService(oldObj: Pages?, newObj: Pages?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.bookmark, newObj!!.bookmark)
        assertEquals(oldObj.image, newObj.image)
    }
}
