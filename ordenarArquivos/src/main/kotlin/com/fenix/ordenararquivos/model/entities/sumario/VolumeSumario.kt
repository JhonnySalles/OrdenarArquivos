package com.fenix.ordenararquivos.model.entities.sumario

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty

class VolumeSumario(volumeExtraido: Double = 0.0, resumo: String = "", var tituloOriginal: String = "") {
    val volumeExtraidoProperty = SimpleDoubleProperty(volumeExtraido)
    var volumeExtraido: Double
        get() = volumeExtraidoProperty.get()
        set(value) = volumeExtraidoProperty.set(value)

    val volumeProperty = SimpleObjectProperty<Double?>(null)
    var volume: Double?
        get() = volumeProperty.get()
        set(value) = volumeProperty.set(value)

    val resumoProperty = SimpleStringProperty(resumo)
    var resumo: String
        get() = resumoProperty.get()
        set(value) = resumoProperty.set(value)
}
