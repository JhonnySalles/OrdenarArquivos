package com.fenix.ordenararquivos.database

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.sqlite.JDBC
import java.sql.*

object DataBase {

    private val LOG = LoggerFactory.getLogger(DataBase::class.java)
    private const val DATABASE = "ordena.db"
    private lateinit var CONN: Connection

    @JvmStatic
    val instancia: Connection
        get() {
            if (!::CONN.isInitialized)
                iniciaBanco()
            return CONN
        }

    private fun iniciaBanco() {
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.registerDriver(JDBC())
            val flyway = Flyway.configure()
                .dataSource("jdbc:sqlite:" + DATABASE, "", "")
                .locations("filesystem:db/migration")
                .load()
            flyway.migrate()
            CONN = DriverManager.getConnection("jdbc:sqlite:" + DATABASE)
        } catch (e: ClassNotFoundException) { // Driver não encontrado
            LOG.error("O driver de conexão expecificado não foi encontrado.", e)
        } catch (e: SQLException) {
            LOG.error("Não foi possivel conectar ao Banco de Dados.", e)
        }
    }

    fun closeConnection() {
        try {
            CONN.close()
        } catch (e: SQLException) {
            e.printStackTrace()
            LOG.error("Erro ao fechar a conexão com o banco.", e)
        }
    }

    @JvmStatic
    fun closeStatement(st: Statement?) {
        if (st != null) {
            try {
                st.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun closeResultSet(rs: ResultSet?) {
        if (rs != null) {
            try {
                rs.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }
}