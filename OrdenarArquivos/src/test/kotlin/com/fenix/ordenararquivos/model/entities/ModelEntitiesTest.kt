package com.fenix.ordenararquivos.model.entities

import com.fenix.ordenararquivos.model.entities.capitulos.Capitulo
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.entities.comet.CoMet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDateTime

class ModelEntitiesTest {

    @Test
    fun testMangaEquality() {
        val now = LocalDateTime.now()
        // Construtor secundário: id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao
        val m1 = Manga(1L, "One Piece", "01", "001", "op01.cbr", 100, "1-10", now)
        val m2 = Manga(1L, "One Piece", "01", "001", "op01.cbr", 100, "1-10", now)
        val m3 = Manga(2L, "Naruto", "01", "001", "n01.cbr", 100, "1-10", now)

        assertEquals(m1, m2)
        assertNotEquals(m1, m3)
        assertEquals(m1.hashCode(), m2.hashCode())
    }

    @Test
    fun testPastaEquality() {
        val file = File("test.txt")
        // Pasta(pasta, arquivo, nome, volume, capitulo, capitulos, scan, titulo, isCapa)
        val p1 = Pasta(file, "test.txt", "Manga", 1.0f, 1.0f, "1-10", "Scan", "Title", false)
        val p2 = Pasta(file, "test.txt", "Manga", 1.0f, 1.0f, "1-10", "Scan", "Title", false)
        
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun testCapituloVolumeLogic() {
        // Capitulo(capitulo: Double, ingles: String, japones: String)
        val cap = Capitulo(1.0, "One", "Ichi")
        assertEquals(1.0, cap.capitulo)
        assertEquals("One", cap.ingles)

        // Volume(marcado, arquivo, volume, capitulos, tags)
        val vol = Volume(true, "vol01.cbr", 1.0, mutableListOf(), "tag")
        assertEquals(1.0, vol.volume)
    }

    @Test
    fun testCoMetEquality() {
        val c1 = CoMet().apply { title = "A"; series = "S"; volume = 1 }
        val c2 = CoMet().apply { title = "A"; series = "S"; volume = 1 }
        val c3 = CoMet().apply { title = "B"; series = "S"; volume = 1 }

        assertEquals(c1, c2)
        assertNotEquals(c1, c3)
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    @Test
    fun testCaminhosEquality() {
        val manga = Manga().apply { id = 10L }
        // Caminhos(id, manga, capitulo, _numero, _numeroPagina, nomePasta, tag)
        val c1 = Caminhos(1L, manga, "01", 1, "p1", "folder", "tag")
        val c2 = Caminhos(1L, manga, "01", 1, "p1", "folder", "tag")
        
        assertEquals(c1, c2)
        assertEquals(c1.hashCode(), c2.hashCode())
    }
}
