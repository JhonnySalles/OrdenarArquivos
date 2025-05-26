package com.fenix.ordenararquivos.model.entities.capitulos

data class Volume(
    var marcado : Boolean = true,
    var arquivo : String = "",
    val volume : Double,
    val capitulos : MutableList<Capitulo> = mutableListOf(),
    var tags : String = ""
)
