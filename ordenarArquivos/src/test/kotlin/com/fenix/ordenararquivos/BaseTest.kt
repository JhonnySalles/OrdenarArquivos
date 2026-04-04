package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.database.DataBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestStatusListener::class)
abstract class BaseTest {

    private val mLOG = LoggerFactory.getLogger(BaseTest::class.java)
    private var mKeepAlive : Connection? = null

    companion object {
        init {
            // Silenciando logs verbosos do JavaFX/JFoenix antes do primeiro teste
            System.setProperty("glass.accessible.force", "false")
            System.setProperty("com.sun.javafx.binding.Logging.level", "OFF")
            try {
                // Tenta silenciar o FXMLLoader especificamente
                java.util.logging.Logger.getLogger("javafx.fxml").level = java.util.logging.Level.OFF
                java.util.logging.Logger.getLogger("com.sun.javafx.binding").level = java.util.logging.Level.OFF
            } catch (e: Exception) {
                // Ignora se falhar ao configurar o logger JUL
            }
        }
    }

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
