package com.fenix.ordenararquivos.configuration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConfiguracaoTest {

    @Test
    fun `test get caminhoCommicTagger`() {
        // This test verifies that the property is accessible and returns a string
        val value = Configuracao.caminhoCommicTagger
        assertNotNull(value)
    }

    @Test
    fun `test set and get caminhoCommicTagger`() {
        val originalValue = Configuracao.caminhoCommicTagger
        val testValue = "test/path/commictagger"
        
        Configuracao.caminhoCommicTagger = testValue
        assertEquals(testValue, Configuracao.caminhoCommicTagger)
        
        // Restore original value to avoid side effects on other tests
        Configuracao.caminhoCommicTagger = originalValue
    }

    @Test
    fun `test get registrosConsultaMal`() {
        val value = Configuracao.registrosConsultaMal
        assertTrue(value >= 0)
    }

    @Test
    fun `test set and get registrosConsultaMal`() {
        val originalValue = Configuracao.registrosConsultaMal
        val testValue = 100
        
        Configuracao.registrosConsultaMal = testValue
        assertEquals(testValue, Configuracao.registrosConsultaMal)
        
        // Restore
        Configuracao.registrosConsultaMal = originalValue
    }

    @Test
    fun `test get secrets properties`() {
        // These are read-only in the object
        assertNotNull(Configuracao.myAnimeListClient)
        assertNotNull(Configuracao.geminiModel)
        assertNotNull(Configuracao.geminiKey1)
        assertNotNull(Configuracao.geminiKey2)
    }
}
