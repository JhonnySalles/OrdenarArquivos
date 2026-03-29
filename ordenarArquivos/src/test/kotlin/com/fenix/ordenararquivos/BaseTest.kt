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

        mKeepAlive = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared")
        DataBase.instancia
        
        try {
            val rs = DataBase.instancia.metaData.getTables(null, null, "Manga", null)
            if (rs.next()) {
                mLOG.info("Tabela Manga validada no banco em memoria.")
            } else {
                mLOG.error("TABELA 'Manga' NÃO ENCONTRADA após migração!")
            }
        } catch (e: Exception) {
            mLOG.error("Erro ao verificar tabelas: ${e.message}")
        }
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
