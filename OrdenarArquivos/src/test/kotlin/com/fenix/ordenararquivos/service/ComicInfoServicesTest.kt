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
    fun updateTracker() {
        assertNotNull(lastEntity)
        val trackerResult = mMockMal.mockEntity()
        
        mService.updateTracker(lastEntity!!, trackerResult, Linguagem.PORTUGUESE)

        assertEquals(12345L, lastEntity!!.idMal)
        assertTrue(lastEntity!!.characters?.contains("Test Char") == true)
    }
}
