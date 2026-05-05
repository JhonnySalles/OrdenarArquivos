package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.jfoenix.controls.JFXTextField
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class PopupCapitulosDividirControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupCapitulosDividirController

    @BeforeEach
    fun setUp() {
        controller = PopupCapitulosDividirController()
        setField("txtInicio", JFXTextField())
        setField("txtFim", JFXTextField())
    }

    private fun setField(name: String, value: Any?) {
        val field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    @Test
    fun testSetRange() {
        controller.setRange(1.5, 10.0)
        assertEquals("1.5", controller.txtInicio.text)
        assertEquals("10.0", controller.txtFim.text)
        
        controller.setRange(Double.MIN_VALUE, Double.MAX_VALUE)
        assertEquals("", controller.txtInicio.text)
        assertEquals("", controller.txtFim.text)
    }

    @Test
    fun testGetRange() {
        controller.txtInicio.text = "2.0"
        controller.txtFim.text = "5.5"
        
        assertEquals(2.0, controller.getInicio())
        assertEquals(5.5, controller.getFim())
        
        controller.txtInicio.text = "invalid"
        assertEquals(Double.MIN_VALUE, controller.getInicio())
    }
}
