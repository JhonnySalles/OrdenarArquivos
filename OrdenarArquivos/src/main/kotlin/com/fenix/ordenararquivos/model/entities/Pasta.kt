package com.fenix.ordenararquivos.model.entities

import java.io.File

data class Pasta(
    var pasta: File,
    var arquivo: String = "",
    var nome: String = "",
    var volume: Float = 0f,
    var capitulo: Float = 0f,
    var capitulos: String = "",
    var scan: String = "",
    var titulo: String = "",
    var isCapa : Boolean = false
) {

    override fun toString(): String {
        return ("Pasta [nome=" + nome + ", volume=" + volume + ", capitulo=" + capitulo + ", arquivo=" + arquivo + ", scan=" + scan + ", titulo=" + titulo + "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pasta

        if (nome != other.nome) return false
        if (volume != other.volume) return false
        if (capitulo != other.capitulo) return false
        if (arquivo != other.arquivo) return false
        if (capitulos != other.capitulos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nome.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + capitulo.hashCode()
        result = 31 * result + arquivo.hashCode()
        result = 31 * result + capitulos.hashCode()
        return result
    }

}