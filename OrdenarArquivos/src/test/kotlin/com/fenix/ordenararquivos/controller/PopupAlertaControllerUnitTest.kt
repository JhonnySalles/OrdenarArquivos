package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.jfoenix.controls.JFXTextArea
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class PopupAlertaControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupAlertaController

    @BeforeEach
    fun setUp() {
        controller = PopupAlertaController()
        setField("lblTitulo", Label())
        setField("imgIcone", ImageView())
        setField("txtTexto", JFXTextArea())
    }

    private fun setField(name: String, value: Any?) {
        val field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    private fun getField(name: String): Any? {
        val field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(controller)
    }

    @Test
    fun testSetTexto() {
        controller.setTexto("Novo Titulo", "Novo Texto")
        assertEquals("Novo Titulo", (getField("lblTitulo") as Label).text)
        assertEquals("Novo Texto", (getField("txtTexto") as JFXTextArea).text)
    }

    @Test
    fun testAlerta() {
        controller.alerta()
        val lblTitulo = getField("lblTitulo") as Label
        assertTrue(lblTitulo.styleClass.contains("titulo-alerta"))
        assertNotNull((getField("imgIcone") as ImageView).image)
    }

    @Test
    fun testErro() {
        controller.erro()
        val lblTitulo = getField("lblTitulo") as Label
        assertTrue(lblTitulo.styleClass.contains("titulo-erro"))
    }
}
