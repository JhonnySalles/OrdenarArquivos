package com.fenix.ordenararquivos.mock.comicinfo

import com.fenix.ordenararquivos.mock.MockBase
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Manga
import org.junit.jupiter.api.Assertions.*
import java.util.*

class MockComicInfo : MockBase<UUID?, ComicInfo>() {

    private val mockPages = MockPages()

    override fun mockEntity(): ComicInfo = mockEntity(null)

    override fun randomId(): UUID? = UUID.randomUUID()

    override fun updateEntity(input: ComicInfo): ComicInfo {
        return ComicInfo(input.id, 123L, "Comic Updated", "Title Updated", "Series Updated").apply {
            this.pages = mockPages.mockEntities()
            this.publisher = "Publisher Updated"
            this.genre = "Genre Updated"
            this.languageISO = "en"
        }
    }

    override fun mockEntity(id: UUID?): ComicInfo {
        return ComicInfo(id ?: UUID.randomUUID(), null, "Comic Test", "Title Test", "Series Test").apply {
            this.pages = mockPages.mockEntities()
            this.manga = Manga.Yes
            this.ageRating = AgeRating.Pending
            this.publisher = "Publisher Test"
            this.genre = "Genre Test"
            this.languageISO = "pt"
        }
    }

    override fun assertsService(input: ComicInfo?) {
        assertNotNull(input)
        assertNotNull(input!!.id)
        assertTrue(input.comic.isNotEmpty())
        assertTrue(input.title.isNotEmpty())
    }

    override fun assertsService(oldObj: ComicInfo?, newObj: ComicInfo?) {
        assertsService(oldObj)
        assertsService(newObj)
        assertEquals(oldObj!!.comic, newObj!!.comic)
        assertEquals(oldObj.title, newObj.title)
    }
}
