package com.fenix.ordenararquivos.model.entities.comicinfo

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.io.StringWriter
import java.util.*

class ComicInfoXmlTest {

    @Test
    fun `test serializacao completa para XML`() {
        val comic = ComicInfo().apply {
            series = "One Piece"
            title = "Romance Dawn"
            number = 1.0f
            volume = 1
            publisher = "Shueisha"
            genre = "Action, Adventure"
            languageISO = "pt"
            manga = Manga.Yes
            pages = listOf(
                Pages(image = 0, type = ComicPageType.FrontCover, doublePage = false),
                Pages(image = 1, type = ComicPageType.Story, doublePage = true)
            )
            summary = "The beginning of a legend!"
            writer = "Eiichiro Oda"
        }

        val context = JAXBContext.newInstance(ComicInfo::class.java)
        val marshaller = context.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val writer = StringWriter()
        marshaller.marshal(comic, writer)
        val xml = writer.toString()
        println("XML Gerado:\n$xml")

        // Verificacoes basicas no XML gerado (Pages usa atributos)
        assertTrue(xml.contains("<Series>One Piece</Series>"))
        assertTrue(xml.contains("<Title>Romance Dawn</Title>"))
        assertTrue(xml.contains("<Manga>Yes</Manga>"))
        assertTrue(xml.contains("<Pages>"))
        assertTrue(xml.contains("Image=\"0\""))
        assertTrue(xml.contains("Type=\"FrontCover\""))
    }

    @Test
    fun `test desserializacao de XML para objeto`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <ComicInfo>
                <Series>Naruto</Series>
                <Number>12</Number>
                <Volume>2</Volume>
                <Writer>Masashi Kishimoto</Writer>
                <Manga>Yes</Manga>
                <Pages>
                    <Page Image="0" Type="FrontCover" DoublePage="false" Bookmark="Cover" />
                </Pages>
            </ComicInfo>
        """.trimIndent()

        val context = JAXBContext.newInstance(ComicInfo::class.java)
        val unmarshaller = context.createUnmarshaller()
        
        val comic = unmarshaller.unmarshal(StringReader(xml)) as ComicInfo

        assertEquals("Naruto", comic.series)
        assertEquals(12.0f, comic.number)
        assertEquals(2, comic.volume)
        assertEquals("Masashi Kishimoto", comic.writer)
        assertEquals(Manga.Yes, comic.manga)
    }

    @Test
    fun `test tratamento de caracteres especiais no XML`() {
        val comic = ComicInfo().apply {
            series = "Hunter x Hunter"
            notes = "Special & character test < > \" '"
        }

        val context = JAXBContext.newInstance(ComicInfo::class.java)
        val marshaller = context.createMarshaller()
        
        val writer = StringWriter()
        marshaller.marshal(comic, writer)
        val xml = writer.toString()
        println("XML com especiais:\n$xml")

        // JAXB escapa os obrigatorios em nos de texto
        assertTrue(xml.contains("Special &amp; character test &lt; &gt;"))
        
        // Testar volta (unmarshal)
        val unmarshaller = context.createUnmarshaller()
        val decoded = unmarshaller.unmarshal(StringReader(xml)) as ComicInfo
        assertEquals(comic.notes, decoded.notes)
    }

    @Test
    fun `test campos opcionais nulos nao devem aparecer no XML`() {
        val comic = ComicInfo().apply {
            series = "Minimalist"
            // Deixar outros campos nulos
        }

        val context = JAXBContext.newInstance(ComicInfo::class.java)
        val marshaller = context.createMarshaller()
        
        val writer = StringWriter()
        marshaller.marshal(comic, writer)
        val xml = writer.toString()

        assertFalse(xml.contains("<Writer>"))
        assertFalse(xml.contains("<Summary>"))
        assertTrue(xml.contains("<Series>Minimalist</Series>"))
    }
}
