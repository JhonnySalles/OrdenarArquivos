package com.fenix.ordenararquivos.model.entities

import com.google.firebase.database.Exclude

data class Caminhos(
    var id: Long = 0,
    @Exclude @set:Exclude @get:Exclude var manga: Manga? = null,
    var capitulo: String = "",
    private var _numero: Int = 0,
    private var _numeroPagina: String = _numero.toString(),
    var nomePasta: String = ""
) {

    var numero: Int = _numero
        set(value) {
            numeroPagina = value.toString()
            field = value
        }

    fun addNumero(numero: String) {
        this.numero = if (numero.isEmpty()) 0 else Integer.valueOf(numero)
    }

    var numeroPagina: String = _numeroPagina
        private set

    constructor(capitulo: String, numero: String, nomePasta: String) : this(capitulo = capitulo, nomePasta = nomePasta, _numero = if (numero.isEmpty()) 0 else Integer.valueOf(numero)) { }

    constructor(id: Long, manga: Manga, capitulo: String, pagina: Int, pasta: String) : this(id, manga, capitulo, pagina, pagina.toString(), pasta) {}

    override fun toString(): String {
        return "Caminhos [capitulo=$capitulo, numero=$numero, nomePasta=$nomePasta]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Caminhos

        if (manga != other.manga) return false
        if (capitulo != other.capitulo) return false
        if (nomePasta != other.nomePasta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manga?.hashCode() ?: 0
        result = 31 * result + capitulo.hashCode()
        result = 31 * result + nomePasta.hashCode()
        return result
    }

}