package com.fenix.ordenararquivos.model.enums

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LinguagemTest {

    @Test
    fun testGetEnum() {
        assertEquals(Linguagem.PORTUGUESE, Linguagem.getEnum("pt"))
        assertEquals(Linguagem.ENGLISH, Linguagem.getEnum("en"))
        assertEquals(Linguagem.JAPANESE, Linguagem.getEnum("ja"))
        assertEquals(Linguagem.PORTUGUESE_GOOGLE, Linguagem.getEnum("pt-Glt"))
        
        // Null cases
        assertNull(Linguagem.getEnum("invalid"))
        assertNull(Linguagem.getEnum(""))
    }

    @Test
    fun testSiglaConsistency() {
        for (lang in Linguagem.values()) {
             assertNotNull(lang.sigla)
        }
    }
}
