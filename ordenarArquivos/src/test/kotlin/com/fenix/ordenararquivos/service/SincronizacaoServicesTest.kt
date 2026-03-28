package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.BaseTest
import com.fenix.ordenararquivos.controller.AbaArquivoController
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class SincronizacaoServicesTest : BaseTest() {

    @Mock
    lateinit var controller: AbaArquivoController

    private lateinit var sincronizacaoServices: SincronizacaoServices

    @BeforeEach
    fun setUp() {
        sincronizacaoServices = SincronizacaoServices(controller)
    }

    @Test
    fun testInitializationWithoutFirebase() {
        // Garantindo que o serviço foi instanciado
        assertNotNull(sincronizacaoServices)
        
        // Em ambiente de teste, isConfigurado deve ser false (pois o Firebase não iniciou) 
        // ou pelo menos não deve disparar exceções de conexão.
        assertFalse(sincronizacaoServices.isSincronizando())
    }
}
