package com.fenix.ordenararquivos.model.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArgumentosEnumTest {

    @Test
    fun testArgumentosDescriptions() {
        assertEquals("--opf", Argumentos.OPF.description)
    }

    @Test
    fun testArgumentosValues() {
        // Garantir que os enums esperados existem
        val values = Argumentos.values()
        assertTrue(values.contains(Argumentos.OPF))
    }
}
