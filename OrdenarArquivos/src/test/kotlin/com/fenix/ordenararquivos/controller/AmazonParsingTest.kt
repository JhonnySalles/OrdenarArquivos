package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.enums.Linguagem
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CountDownLatch

class AmazonParsingTest : BaseJfxTest() {

    @Test
    fun testObtemData() {
        val latch = CountDownLatch(1)
        Platform.runLater {
            val controller = PopupAmazon()
            
            // Japanese format
            assertEquals("2024-01-01", controller.obtemData("2024/01/01", Linguagem.JAPANESE))
            
            // English format
            assertEquals("2024-01-01", controller.obtemData("January 1, 2024", Linguagem.ENGLISH))
            assertEquals("2024-12-25", controller.obtemData("December 25, 2024", Linguagem.ENGLISH))
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun testParseAmazonEn() {
        val latch = CountDownLatch(1)
        Platform.runLater {
            val loader = FXMLLoader(PopupAmazon.fxmlLocate)
            loader.load<Any>()
            val controller = loader.getController<PopupAmazon>()
            
            val htmlFile = File("src/test/resources/fixtures/amazon_en.html")
            val doc = Jsoup.parse(htmlFile, "UTF-8")
            
            controller.parse(doc, Linguagem.ENGLISH)
            
            assertEquals("Manga Title EN", controller.txtTitulo.text)
            assertEquals("Description EN", controller.txtAreaComentario.text)
            assertEquals("Publisher EN", controller.txtEditora.text)
            assertEquals("Series EN", controller.txtSerie.text)
            assertEquals("2024-01-01", controller.dpPublicacao.value.toString())
            
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun testParseAmazonJp() {
        val latch = CountDownLatch(1)
        Platform.runLater {
            val loader = FXMLLoader(PopupAmazon.fxmlLocate)
            loader.load<Any>()
            val controller = loader.getController<PopupAmazon>()
            
            val htmlFile = File("src/test/resources/fixtures/amazon_jp.html")
            val doc = Jsoup.parse(htmlFile, "UTF-8")
            
            controller.parse(doc, Linguagem.JAPANESE)
            
            assertEquals("Manga Title JP", controller.txtTitulo.text)
            assertEquals("Description JP", controller.txtAreaComentario.text)
            assertEquals("Publisher JP", controller.txtEditora.text)
            assertEquals("2024-01-01", controller.dpPublicacao.value.toString())
            
            latch.countDown()
        }
        latch.await()
    }
}
