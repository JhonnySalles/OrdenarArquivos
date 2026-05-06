package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.model.entities.capitulos.Capitulo
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PopupCapitulosSplitLogicTest {

    @Test
    fun `test logica de divisao de capitulos`() {
        // 1. Preparar ambiente com capitulos
        val cap1 = Capitulo(capitulo = 1.0, ingles = "Cap 1", japones = "")
        val cap2 = Capitulo(capitulo = 2.0, ingles = "Cap 2", japones = "")
        val cap3 = Capitulo(capitulo = 3.0, ingles = "Cap 3", japones = "")
        val cap4 = Capitulo(capitulo = 4.0, ingles = "Cap 4", japones = "")
        val cap5 = Capitulo(capitulo = 5.0, ingles = "Cap 5", japones = "")
        
        val capitulos = mutableListOf(cap1, cap2, cap3, cap4, cap5)
        val volumeOriginal = Volume(volume = 1.0, capitulos = capitulos)
        val listaVolumes = mutableListOf(volumeOriginal)

        // 2. Simular o que o PopupCapitulosController faz no botão confirmar
        val inicio = 2.0
        val fim = 4.0

        // Filtro manual 
        val extraidos = mutableListOf<Capitulo>()
        for (c in volumeOriginal.capitulos) {
            if (c.capitulo >= inicio && c.capitulo <= fim) {
                extraidos.add(c)
            }
        }
        
        if (extraidos.isNotEmpty()) {
            volumeOriginal.capitulos.removeAll(extraidos)
            val novoVolume = Volume(
                marcado = volumeOriginal.marcado,
                arquivo = "Não Localizados",
                volume = 0.0,
                capitulos = extraidos.toMutableList()
            )
            
            // Simular a inserção na lista (como o controller faz)
            val index = listaVolumes.indexOf(volumeOriginal)
            listaVolumes.add(index + 1, novoVolume)
        }

        // 3. Verificações
        assertEquals(2, listaVolumes.size, "Deveria ter criado um novo volume")
        assertEquals(2, volumeOriginal.capitulos.size, "Volume original deveria ter 2 capítulos restantes")
        assertEquals(3, listaVolumes[1].capitulos.size, "Novo volume deveria ter 3 capítulos")
        
        assertTrue(volumeOriginal.capitulos.contains(cap1))
        assertTrue(volumeOriginal.capitulos.contains(cap5))
        
        assertTrue(listaVolumes[1].capitulos.contains(cap2))
        assertTrue(listaVolumes[1].capitulos.contains(cap3))
        assertTrue(listaVolumes[1].capitulos.contains(cap4))
    }
}
