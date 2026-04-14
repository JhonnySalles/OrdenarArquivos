package com.fenix.ordenararquivos.database

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.sqlite.JDBC
import java.sql.*

object DataBase {

    private val mLOG = LoggerFactory.getLogger(DataBase::class.java)
    private const val mDATABASE = "ordena.db"
    const val mDATABASE_TEST = "ordenatest.db"
    private var mCONN: Connection? = null
    var isTeste : Boolean = false

    @JvmStatic
    val instancia: Connection
        get() {
            if (mCONN == null)
                iniciaBanco()
            return mCONN!!
        }

    private fun iniciaBanco() {
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.registerDriver(JDBC())

            val url = if (isTeste) "jdbc:sqlite:file:testdb?mode=memory&cache=shared" else "jdbc:sqlite:$mDATABASE"

            try {
                val flyway = Flyway.configure()
                    .dataSource(url, "", "")
                    .locations("./db/migration", "classpath:/db/migration", "classpath:db/migration")
                    .load()
                flyway.migrate()
            } catch (e: Exception) {
                mLOG.error("Não foi possivel atualizar o Banco de Dados.", e)
            }
            mCONN = DriverManager.getConnection(url)
        } catch (e: ClassNotFoundException) { // Driver não encontrado
            mLOG.error("O driver de conexão expecificado não foi encontrado.", e)
        } catch (e: SQLException) {
            mLOG.error("Não foi possivel conectar ao Banco de Dados.", e)
        }
    }

    fun closeConnection() {
        try {
            mCONN?.close()
            mCONN = null
        } catch (e: SQLException) {
            if (!isTeste) {
                e.printStackTrace()
                mLOG.error("Erro ao fechar a conexão com o banco.", e)
            }
        }
    }

    @JvmStatic
    fun closeStatement(st: Statement?) {
        if (st != null) {
            try {
                st.close()
            } catch (e: SQLException) {
                if (!isTeste) e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun closeResultSet(rs: ResultSet?) {
        if (rs != null) {
            try {
                rs.close()
            } catch (e: SQLException) {
                if (!isTeste) e.printStackTrace()
            }
        }
    }
}