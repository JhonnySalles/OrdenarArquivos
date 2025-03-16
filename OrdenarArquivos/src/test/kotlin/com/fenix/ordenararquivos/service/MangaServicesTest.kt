package com.fenix.ordenararquivos.service

import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MangaServicesTest {

    @Test
    @Order(1)
    fun save() {
    }

    @Test
    @Order(2)
    fun find() {
    }

    @Test
    @Order(3)
    fun findEnvio() {
    }

}