package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.mock.Mock
import com.fenix.ordenararquivos.mock.MockManga
import com.fenix.ordenararquivos.model.entities.Manga
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension::class)
internal class MangaServicesTest {

    private val mLOG = LoggerFactory.getLogger(MangaServicesTest::class.java)

    private var mService : MangaServices

    init {
        mLOG.info("Preparando a base de teste...")

        DataBase.isTeste = true
        val db = File(System.getProperty("user.dir"), DataBase.mDATABASE_TEST)
        if (db.exists())
            db.delete()

        mService = MangaServices()
    }

    lateinit var input: Mock<Long?, Manga>
    var lastId: Long? = 0
    lateinit var lastEntity: Manga

    @BeforeEach
    @Throws(Exception::class)
    fun setUpMocks() {
        input = MockManga()
    }

    @Test
    @Order(1)
    fun save() {
        lastEntity = input.mockEntity(0)
        mService.save(lastEntity)
        lastId = lastEntity.id
        assertNotEquals(0, lastId)
    }

    @Test
    @Order(2)
    fun find() {
        input.assertsService(lastEntity, mService.find(lastEntity))
    }

    @Test
    @Order(3)
    fun update() {
        lastEntity = input.updateEntity(lastEntity)
        mService.save(lastEntity)

        val entity = mService.find(lastEntity)
        input.assertsService(lastEntity, entity)
    }

    @Test
    @Order(4)
    fun findAnterior() {
        lastEntity = input.mockEntity()
        lastEntity.nome = "Manga Anterior"
        lastEntity.volume = "01"
        mService.save(lastEntity)

        var entity = input.mockEntity()
        entity.nome = "Manga Anterior"
        entity.volume = "02"
        entity = mService.find(entity, anterior = true)!!
        input.assertsService(lastEntity, entity)
    }

    @Test
    @Order(5)
    fun findEnvio() {
        val entities = mService.findEnvio(LocalDateTime.MIN)
        assertNotNull(entities)
        assertTrue(entities.isNotEmpty())
    }

    @AfterAll
    fun close() {
        DataBase.closeConnection()
    }

}