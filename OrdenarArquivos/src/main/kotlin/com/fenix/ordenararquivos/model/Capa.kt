package com.fenix.ordenararquivos.model

data class Capa(
    var nome: String = "",
    var arquivo: String = "",
    var tipo: TipoCapa = TipoCapa.CAPA,
    var isDupla: Boolean = false,
    var direita: Capa? = null
) { }