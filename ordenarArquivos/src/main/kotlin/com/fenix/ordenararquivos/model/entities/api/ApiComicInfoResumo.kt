package com.fenix.ordenararquivos.model.entities.api

import java.util.UUID

data class ApiComicInfoResumo(
    val id: UUID,
    val series: String,
    val title: String,
    val volume: Float?,
    val number: Float,
    val languageISO: String?,
    val publisher: String?,
    val year: Int? = null,
    val format: String? = null
)
