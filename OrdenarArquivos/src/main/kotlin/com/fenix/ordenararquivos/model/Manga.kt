package com.fenix.ordenararquivos.model

import java.time.LocalDateTime

data class Manga(
    var id: Long = 0,
    var nome: String = "",
    var volume: String = "",
    var capitulo: String = "",
    var arquivo: String = "",
    var capitulos: String = "",
    var quantidade: Int = 0,
    var atualizacao: LocalDateTime = LocalDateTime.now(),
    var caminhos: MutableList<Caminhos> = arrayListOf()
) {
    constructor(
        id: Long,
        nome: String,
        volume: String,
        capitulo: String,
        arquivo: String,
        quantidade: Int,
        capitulos: String,
        atualizacao: LocalDateTime
    ) : this(
        id,
        nome,
        volume,
        capitulo,
        arquivo,
        capitulos,
        quantidade,
        atualizacao
    ) {

    }

    fun addCaminhos(caminhos: Caminhos) {
        this.caminhos.add(caminhos)
    }

    override fun toString(): String {
        return ("Manga [id=" + id + ", nome=" + nome + ", volume=" + volume + ", capitulo=" + capitulo + ", arquivo="
                + arquivo + "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Manga

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