package com.fenix.ordenararquivos.model.entities.comicinfo

import com.jfoenix.controls.JFXButton
import javafx.scene.image.ImageView

class Mal(
    var id: Long,
    var nome: String,
    var descricao: String,
    var site: JFXButton? = null,
    var imagem: ImageView? = null
) {
    val idVisual: String
        get() = if (id > 0) id.toString() else ""

    fun setButton(site: JFXButton) {
        this.site = site
    }
}