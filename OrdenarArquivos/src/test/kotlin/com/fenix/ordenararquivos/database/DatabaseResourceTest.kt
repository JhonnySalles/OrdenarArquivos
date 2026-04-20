package com.fenix.ordenararquivos.database

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class DatabaseResourceTest {
    
    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        DataBase.isTeste = true
    }

    @Test
    fun testCloseResultSetSafe() {
        val rs = mock(ResultSet::class.java)
        
        // Deve fechar se não for nulo
        DataBase.closeResultSet(rs)
        verify(rs, times(1)).close()
        
        // Não deve estourar erro se for nulo
        assertDoesNotThrow { DataBase.closeResultSet(null) }
    }

    @Test
    fun testCloseStatementSafe() {
        val st = mock(Statement::class.java)
        
        // Deve fechar se não for nulo
        DataBase.closeStatement(st)
        verify(st, times(1)).close()
        
        // Não deve estourar erro se for nulo
        assertDoesNotThrow { DataBase.closeStatement(null) }
    }

    @Test
    fun testCloseResultSetWithException() {
        val rs = mock(ResultSet::class.java)
        `when`(rs.close()).thenThrow(SQLException("Erro fechar"))
        
        // Deve capturar a exceção e não propagar
        assertDoesNotThrow { DataBase.closeResultSet(rs) }
    }

    @Test
    fun testCloseStatementWithException() {
        val st = mock(Statement::class.java)
        `when`(st.close()).thenThrow(SQLException("Erro fechar"))
        
        // Deve capturar a exceção e não propagar
        assertDoesNotThrow { DataBase.closeStatement(st) }
    }
}
