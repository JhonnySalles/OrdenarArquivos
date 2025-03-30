package com.fenix.ordenararquivos.model.entities

import com.fenix.ordenararquivos.model.enums.TipoCapa

data class Capa(
    var nome: String = "",
    var arquivo: String = "",
    var tipo: TipoCapa = TipoCapa.CAPA,
    var isDupla: Boolean = false,
    var direita: Capa? = null
) { }