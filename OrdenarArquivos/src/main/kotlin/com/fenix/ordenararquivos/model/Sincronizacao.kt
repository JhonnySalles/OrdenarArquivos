package com.fenix.ordenararquivos.model

import java.time.LocalDateTime

data class Sincronizacao(
    var envio : LocalDateTime  = LocalDateTime.now(),
    var recebimento : LocalDateTime  = LocalDateTime.now()
) { }