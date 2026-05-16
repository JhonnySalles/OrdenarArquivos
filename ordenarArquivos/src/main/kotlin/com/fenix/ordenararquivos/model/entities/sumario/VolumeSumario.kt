package com.fenix.ordenararquivos.model.entities.sumario

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty

class VolumeSumario(volume: Double = 0.0, resumo: String = "", var tituloOriginal: String = "") {
    val volumeProperty = SimpleDoubleProperty(volume)
    var volume: Double
        get() = volumeProperty.get()
        set(value) = volumeProperty.set(value)

    val resumoProperty = SimpleStringProperty(resumo)
    var resumo: String
        get() = resumoProperty.get()
        set(value) = resumoProperty.set(value)
}
