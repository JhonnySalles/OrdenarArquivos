package com.fenix.ordenararquivos.database

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.sqlite.JDBC
import java.sql.*

object DataBase {

    private val mLOG = LoggerFactory.getLogger(DataBase::class.java)
    private const val mDATABASE = "ordena.db"
    private lateinit var mCONN: Connection

    @JvmStatic
    val instancia: Connection
        get() {
            if (!::mCONN.isInitialized)
                iniciaBanco()
            return mCONN
        }

    private fun iniciaBanco() {
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.registerDriver(JDBC())
            val flyway = Flyway.configure()
                .dataSource("jdbc:sqlite:" + mDATABASE, "", "")
                .locations("classpath:db/migration")
                .load()
            flyway.migrate()
            mCONN = DriverManager.getConnection("jdbc:sqlite:" + mDATABASE)
        } catch (e: ClassNotFoundException) { // Driver não encontrado
            mLOG.error("O driver de conexão expecificado não foi encontrado.", e)
        } catch (e: SQLException) {
            mLOG.error("Não foi possivel conectar ao Banco de Dados.", e)
        }
    }

    fun closeConnection() {
        try {
            mCONN.close()
        } catch (e: SQLException) {
            e.printStackTrace()
            mLOG.error("Erro ao fechar a conexão com o banco.", e)
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