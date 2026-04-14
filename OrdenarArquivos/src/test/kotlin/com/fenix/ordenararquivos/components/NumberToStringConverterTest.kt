package com.fenix.ordenararquivos.components

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class NumberToStringConverterTest {

    private val locale = Locale.US
    private val pattern = "00.##"
    private val converter = NumberToStringConverter(locale, pattern)

    @Test
    fun testFromStringConversion() {
        // "10.50" -> 10.5 * 100 = 1050
        assertEquals(1050L, converter.fromString("10.50"))
        assertEquals(100L, converter.fromString("1.00"))
        
        // "0.01" -> 1
        assertEquals(1L, converter.fromString("0.01"))
        
        // "0" -> 0
        assertEquals(0L, converter.fromString("0"))
    }

    @Test
    fun testToStringConversion() {
        // 1050 -> "10.5" (com pattern "00.##" se torna "10.5")
        // O pattern padrão para o teste vai formatar conforme o super.toString(asDouble)
        val result = converter.toString(1050L)
        assertTrue(result.contains("10.5"), "Deveria conter 10.5, obtido: $result")
        
        assertEquals("01", converter.toString(100L))
        assertEquals("00.01", converter.toString(1L))
        assertEquals("00", converter.toString(0L))
    }

    @Test
    fun testMaxLongLimits() {
        // Se exceder +/- 1 trilhão, retorna vazio
        assertEquals("", converter.toString(1000000000001L))
        assertEquals("", converter.toString(-1000000000001L))
        
        // Valor nulo
        assertEquals("", converter.toString(null))
    }
}
