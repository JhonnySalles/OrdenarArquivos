package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.mock.comicinfo.MockComicInfo
import com.fenix.ordenararquivos.mock.comicinfo.MockMal
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.time.LocalDateTime

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(MockitoExtension::class)
internal class ComicInfoServicesTest : BaseTest() {

    private val mLOG = LoggerFactory.getLogger(ComicInfoServicesTest::class.java)

    private lateinit var mService: ComicInfoServices
    private lateinit var mMockComicInfo: MockComicInfo
    private lateinit var mMockMal: MockMal

    private var lastEntity: ComicInfo? = null

    @BeforeEach
    fun setUp() {
        mService = ComicInfoServices()
        mMockComicInfo = MockComicInfo()
        mMockMal = MockMal()
    }

    @Test
    @Order(1)
    fun save() {
        val entity = mMockComicInfo.mockEntity()
        entity.id = null // Garantir que chame o 'insert'
        mService.save(entity, isSendCloud = false)
        lastEntity = entity
        assertNotNull(entity.id)
        mLOG.info("ComicInfo inserido com sucesso: ${entity.id}")
    }

    @Test
    @Order(2)
    fun find() {
        assertNotNull(lastEntity, "lastEntity deve estar preenchido do teste anterior")
        val found = mService.find(lastEntity!!.comic, lastEntity!!.languageISO)
        assertNotNull(found, "ComicInfo não encontrado no banco")
        assertEquals(lastEntity!!.comic, found?.comic)
    }

    @Test
    @Order(3)
    fun selectEnvio() {
        val list = mService.findEnvio(LocalDateTime.now().minusDays(1))
        assertNotNull(list)
        assertTrue(list.isNotEmpty(), "A lista de envio deve conter o registro salvo")
    }

    @Test
    @Order(4)
    fun update() {
        assertNotNull(lastEntity)
        lastEntity!!.comic = "Atualizado"
        mService.save(lastEntity!!, isSendCloud = false)
        
        val updated = mService.find("Atualizado", lastEntity!!.languageISO)
        assertNotNull(updated, "Registro atualizado não encontrado")
        assertEquals("Atualizado", updated?.comic)
    }

    @Test
    @Order(5)
    fun updateMal() {
        assertNotNull(lastEntity)
        val mal = mMockMal.mockEntity()
        val mockAuthor = mock(dev.katsute.mal4j.manga.property.Author::class.java)
        `when`(mockAuthor.role).thenReturn("story")
        `when`(mockAuthor.firstName).thenReturn("Eichiro")
        `when`(mockAuthor.lastName).thenReturn("Oda")
        
        val mockTitles = mock(dev.katsute.mal4j.property.AlternativeTitles::class.java)
        lenient().`when`(mockTitles.english).thenReturn("One Piece")
        lenient().`when`(mockTitles.japanese).thenReturn("ワンピース")
        lenient().`when`(mockTitles.synonyms).thenReturn(arrayOf("OP"))

        lenient().`when`(mal.mal.authors).thenReturn(arrayOf(mockAuthor))
        lenient().`when`(mal.mal.alternativeTitles).thenReturn(mockTitles)
        lenient().`when`(mal.mal.genres).thenReturn(arrayOf())
        lenient().`when`(mal.mal.serialization).thenReturn(arrayOf())
        lenient().`when`(mal.mal.title).thenReturn("One Piece Title")
        lenient().`when`(mal.mal.id).thenReturn(123L)
        
        // Mocking HttpClient static calls for character list fetching
        mockStatic(HttpClient::class.java).use { httpClientMock ->
            val mockClient = mock(HttpClient::class.java)
            val mockBuilder = mock(HttpClient.Builder::class.java)
            val mockResponse = mock(HttpResponse::class.java) as HttpResponse<String>

            `when`(HttpClient.newBuilder()).thenReturn(mockBuilder)
            `when`(mockBuilder.build()).thenReturn(mockClient)
            `when`(mockResponse.body()).thenReturn("{\"data\": [{\"character\": {\"name\": \"Test Character\"}, \"role\": \"main\"}]}")
            `when`(mockClient.send(any(), any<HttpResponse.BodyHandler<String>>())).thenReturn(mockResponse)

            mService.updateMal(lastEntity!!, mal, Linguagem.PORTUGUESE)

            assertEquals(123L, lastEntity!!.idMal)
            assertTrue(lastEntity!!.characters?.contains("Test Character") == true)
        }
    }
}
