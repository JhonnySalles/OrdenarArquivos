package com.fenix.ordenararquivos.model.entities

import java.time.LocalDateTime

data class Sincronizacao(
    var envio : LocalDateTime  = LocalDateTime.now(),
    var recebimento : LocalDateTime  = LocalDateTime.now()
) { }