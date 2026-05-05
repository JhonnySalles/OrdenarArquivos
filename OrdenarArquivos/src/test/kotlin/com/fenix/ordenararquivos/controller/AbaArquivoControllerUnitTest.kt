package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXTextField
import javafx.scene.control.Label
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.File
import java.lang.reflect.Field

class AbaArquivoControllerUnitTest : BaseJfxTest() {

    private lateinit var controller: AbaArquivoController
    private val mangaService: MangaServices = mock()
    private val comicInfoService: ComicInfoServices = mock()
    private val winrarService: WinrarServices = mock()
    private val telaInicialController: TelaInicialController = mock()

    @BeforeEach
    fun setUp() {
        controller = AbaArquivoController()
        controller.controllerPai = telaInicialController

        // Injetar mocks
        setField("mServiceManga", mangaService)
        setField("mServiceComicInfo", comicInfoService)
        setField("mRarService", winrarService)

        // Injetar campos FXML necessários para os testes de volume
        setField("txtVolume", JFXTextField())
        setField("txtNomeArquivo", JFXTextField())
        setField("lblAviso", Label())
        setField("lblAlerta", Label())
        setField("txtNomePastaManga", JFXTextField().apply { text = "Manga -" })
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
    fun testOnBtnVolumeMais() {
        val txtVolume = getField("txtVolume") as JFXTextField
        txtVolume.text = "Volume 01"

        val method = controller.javaClass.getDeclaredMethod("onBtnVolumeMais")
        method.isAccessible = true
        method.invoke(controller)

        assertEquals("Volume 02", txtVolume.text)

        txtVolume.text = "Volume 09"
        method.invoke(controller)
        assertEquals("Volume 10", txtVolume.text)
    }

    @Test
    fun testOnBtnVolumeMenos() {
        val txtVolume = getField("txtVolume") as JFXTextField
        txtVolume.text = "Volume 05"

        val method = controller.javaClass.getDeclaredMethod("onBtnVolumeMenos")
        method.isAccessible = true
        method.invoke(controller)

        assertEquals("Volume 04", txtVolume.text)

        txtVolume.text = "Volume 00"
        method.invoke(controller)
        assertEquals("Volume 00", txtVolume.text)
    }

    @Test
    fun testNaturalSort() {
        val files = listOf(
            File("file1.jpg"),
            File("file10.jpg"),
            File("file2.jpg"),
            File("file02.jpg")
        )

        // O método sortedNaturally é uma extensão privada em AbaArquivoController
        // Como é uma extensão de List<File>, podemos tentar acessá-la via reflexão no controller ou apenas replicar a lógica se for complexo.
        // No Kotlin, extensões são compiladas como métodos estáticos que recebem o receiver como primeiro argumento.
        
        val method = controller.javaClass.getDeclaredMethod("sortedNaturally", List::class.java)
        method.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val sorted = method.invoke(controller, files) as List<File>
        
        assertEquals("file1.jpg", sorted[0].name)
        // file02 e file2 podem variar dependendo da implementação exata, mas file2 deve vir antes de file10
        assertTrue(sorted.indexOf(File("file2.jpg")) < sorted.indexOf(File("file10.jpg")))
        assertTrue(sorted.indexOf(File("file02.jpg")) < sorted.indexOf(File("file10.jpg")))
    }
}
