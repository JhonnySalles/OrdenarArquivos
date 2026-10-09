package com.fenix.ordenararquivos.model.entities.comicinfo

import com.fenix.ordenararquivos.model.enums.TrackerType
import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

data class TrackerMetadata(
    val id: Long,
    val title: String,
    val alternativeTitles: List<String>, // includes english, japanese, synonyms
    val genres: List<String>,
    val authors: List<TrackerAuthor>,
    val serialization: List<String>,
    val url: String,
    val type: String, // e.g. manga, novel
    val characters: String? = null
)

data class TrackerAuthor(
    val role: String,
    val firstName: String,
    val lastName: String
)

class TrackerResult(
    var tracker: TrackerType,
    var id: Long,
    var nome: String,
    var descricao: String,
    var site: JFXButton? = null,
    var imagem: ImageView? = null,
    var dados: TrackerMetadata
) {
    val idVisual: String get() = if (id > 0) id.toString() else ""
    val tipo: String get() = dados.type.replace("_", " ").replaceFirstChar { it.uppercase() }
    
    // UI representation of Tracker type with badge-like HBox
    val trackerBadge: HBox get() {
        val label = Label(tracker.label)
        label.style = "-fx-background-color: " + (if (tracker == TrackerType.ANILIST) "#02A9FF" else "#2E51A2") + "; -fx-text-fill: white; -fx-padding: 2 4 2 4; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;"
        val box = HBox(label)
        box.alignment = Pos.CENTER
        return box
    }

    val siteBox: VBox get() {
        val label = Label(tracker.label)
        label.style = "-fx-background-color: " + (if (tracker == TrackerType.ANILIST) "#02A9FF" else "#2E51A2") + "; -fx-text-fill: white; -fx-padding: 2 4 2 4; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;"
        val box = VBox(4.0)
        box.alignment = Pos.CENTER
        if (site != null) {
            box.children.add(site)
        }
        box.children.add(label)
        return box
    }

    fun setButton(site: JFXButton) {
        this.site = site
    }
}
