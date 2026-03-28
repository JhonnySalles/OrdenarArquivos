package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.database.DataBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseTest {

    private val mLOG = LoggerFactory.getLogger(BaseTest::class.java)
    private var mKeepAlive : Connection? = null

    @BeforeAll
    fun baseSetUp() {
        mLOG.info("Configurando ambiente de teste (:memory:)...")
        DataBase.isTeste = true
        DataBase.closeConnection()
        
        // Mantém pelo menos uma conexão aberta para que o SQLite não delete o banco em memória compartilhado
        mKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared")
        
        // Agora, a primeira chamada a DataBase.instancia rodará o Flyway no mesmo banco em memória
    }

    @AfterAll
    fun baseTearDown() {
        mLOG.info("Limpando ambiente de teste...")
        DataBase.closeConnection()
        mKeepAlive?.close()
        mKeepAlive = null
        DataBase.isTeste = false
    }
}
