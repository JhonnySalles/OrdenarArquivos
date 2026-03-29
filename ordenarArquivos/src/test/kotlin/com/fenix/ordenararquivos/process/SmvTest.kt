package com.fenix.ordenararquivos.process

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmvTest {

    @Test
    fun `deve converter numero para array binario de 8 bits`() {
        val binary0 = Smv.convertToBinary(0)
        assertArrayEquals(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0), binary0)

        val binary255 = Smv.convertToBinary(255)
        assertArrayEquals(intArrayOf(1, 1, 1, 1, 1, 1, 1, 1), binary255)

        val binary170 = Smv.convertToBinary(170) // 10101010
        assertArrayEquals(intArrayOf(1, 0, 1, 0, 1, 0, 1, 0), binary170)
    }

    @Test
    fun `deve identificar se um padrao binario e uniforme`() {
        // Uniforme: no maximo 2 transicoes (0->1 ou 1->0)
        assertTrue(Smv.isBinaryUniform(intArrayOf(1, 1, 1, 1, 1, 1, 1, 1)), "Todo 1 deve ser uniforme")
        assertTrue(Smv.isBinaryUniform(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)), "Todo 0 deve ser uniforme")
        assertTrue(Smv.isBinaryUniform(intArrayOf(0, 0, 1, 1, 1, 0, 0, 0)), "00111000 tem 2 transicoes")
        
        assertFalse(Smv.isBinaryUniform(intArrayOf(1, 0, 1, 0, 1, 0, 1, 0)), "10101010 tem muitas transicoes")
    }

    @Test
    fun `deve normalizar um array de doubles entre 0 e 1`() {
        val array = doubleArrayOf(10.0, 20.0, 30.0, 40.0, 50.0)
        Smv.normalizeArray(array)
        
        assertEquals(0.0, array[0], 0.0001)
        assertEquals(0.25, array[1], 0.0001)
        assertEquals(0.5, array[2], 0.0001)
        assertEquals(0.75, array[3], 0.0001)
        assertEquals(1.0, array[4], 0.0001)
    }
}
