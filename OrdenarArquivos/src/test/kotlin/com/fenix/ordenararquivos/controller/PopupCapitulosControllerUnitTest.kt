package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import javafx.collections.FXCollections
import javafx.scene.control.TableView
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class PopupCapitulosControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: PopupCapitulosController

    @BeforeEach
    fun setUp() {
        controller = PopupCapitulosController()
        
        // Injetar campos FXML via reflexão
        setField("txtEndereco", JFXTextField())
        setField("cbLinguagem", JFXComboBox<Linguagem>())
        setField("cbMarcarTodos", JFXCheckBox())
        setField("tbViewTabela", TableView<Volume>())
        
        (getField("cbLinguagem") as JFXComboBox<Linguagem>).items.setAll(*Linguagem.values())
        (getField("cbLinguagem") as JFXComboBox<Linguagem>).selectionModel.select(Linguagem.JAPANESE)
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = findField(controller.javaClass, name)
        field.isAccessible = true
        field.set(controller, value)
    }

    private fun getField(name: String): Any? {
        val field: Field = findField(controller.javaClass, name)
        field.isAccessible = true
        return field.get(controller)
    }

    private fun findField(clazz: Class<*>, name: String): Field {
        return try {
            clazz.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            if (clazz.superclass != null) findField(clazz.superclass, name)
            else throw e
        }
    }

    @Test
    fun testExtractManualText() {
        val text = """
            Volume 1
            Chapter 1: The Beginning
            Chapter 2: The End
            
            Volume 2
            Chapter 3: More
        """.trimIndent()
        
        // extractManualText é privado, acessamos via reflexão
        val method = controller.javaClass.getDeclaredMethod("extractManualText", String::class.java)
        method.isAccessible = true
        method.invoke(controller, text)
        
        val lista = getField("mLista") as List<Volume>
        assertEquals(2, lista.size)
        assertEquals(1.0, lista[0].volume)
        assertEquals(2, lista[0].capitulos.size)
        assertEquals(2.0, lista[0].capitulos[1].capitulo)
        assertEquals("The End", lista[0].capitulos[1].ingles)
        
        assertEquals(2.0, lista[1].volume)
        assertEquals(1, lista[1].capitulos.size)
    }

    @Test
    fun testPrepararSemProcessar() {
        val volumes = listOf(
            Volume(volume = 1.0).apply {
                capitulos.add(com.fenix.ordenararquivos.model.entities.capitulos.Capitulo(capitulo = 1.0, ingles = "Title 1", japones = ""))
            }
        )
        
        val method = controller.javaClass.getDeclaredMethod("preparar", List::class.java)
        method.isAccessible = true
        method.invoke(controller, volumes)
        
        val lista = getField("mLista") as List<Volume>
        assertEquals(1, lista.size)
        // O separador de imagem é : pelo que vi no código (ou algo similar)
        // Na verdade é mDecimal.format(it.capitulo) + separador + title
        assertTrue(lista[0].tags.contains("Title 1"))
    }
}
