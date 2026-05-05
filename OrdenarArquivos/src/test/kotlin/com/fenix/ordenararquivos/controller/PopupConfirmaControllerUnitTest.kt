package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.jfoenix.controls.JFXTextArea
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class PopupConfirmaControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupConfirmaController

    @BeforeEach
    fun setUp() {
        controller = PopupConfirmaController()
        setField("lblTitulo", Label())
        setField("imgIcone", ImageView())
        setField("txtTexto", JFXTextArea())
    }

    private fun setField(name: String, value: Any?) {
        val field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    @Test
    fun testSetTexto() {
        controller.setTexto("Confirmar?", "Tem certeza?")
        val lblTitulo = controller.javaClass.getDeclaredField("lblTitulo").apply { isAccessible = true }.get(controller) as Label
        val txtTexto = controller.javaClass.getDeclaredField("txtTexto").apply { isAccessible = true }.get(controller) as JFXTextArea
        assertEquals("Confirmar?", lblTitulo.text)
        assertEquals("Tem certeza?", txtTexto.text)
    }
}
