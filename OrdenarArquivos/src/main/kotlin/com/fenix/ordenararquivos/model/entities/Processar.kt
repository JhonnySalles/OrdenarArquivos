package com.fenix.ordenararquivos.model.entities

import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.jfoenix.controls.JFXButton
import java.io.File

data class Processar(
    var arquivo: String = "",
    var tags: String = "",
    var file: File? = null,
    var comicInfo: ComicInfo? = null,
    var processar: JFXButton? = null,
    var amazon: JFXButton? = null,
    var salvar: JFXButton? = null,
    var isProcessado: Boolean = false
)