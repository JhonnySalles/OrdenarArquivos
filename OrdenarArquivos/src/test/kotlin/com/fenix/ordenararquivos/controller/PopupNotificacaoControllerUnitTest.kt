package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class PopupNotificacaoControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupNotificacaoController

    @BeforeEach
    fun setUp() {
        controller = PopupNotificacaoController()
        setField("lblTitulo", Label())
        setField("lblTexto", Label())
        setField("imgIcone", ImageView())
    }

    private fun setField(name: String, value: Any?) {
        val field = controller.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    @Test
    fun testSetNotificacao() {
        controller.setTitulo("Sucesso")
        controller.setTexto("Operação concluída")
        val lblTitulo = controller.javaClass.getDeclaredField("lblTitulo").apply { isAccessible = true }.get(controller) as Label
        val lblTexto = controller.javaClass.getDeclaredField("lblTexto").apply { isAccessible = true }.get(controller) as Label
        assertEquals("Sucesso", lblTitulo.text)
        assertEquals("Operação concluída", lblTexto.text)
    }
}
