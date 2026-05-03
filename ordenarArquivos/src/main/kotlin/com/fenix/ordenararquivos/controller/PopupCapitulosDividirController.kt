package com.fenix.ordenararquivos.controller

import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXML
import javafx.fxml.Initializable
import java.net.URL
import java.util.*

class PopupCapitulosDividirController : Initializable {

    @FXML
    lateinit var txtInicio: JFXTextField

    @FXML
    lateinit var txtFim: JFXTextField

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // Inicialização básica se necessário
    }

    fun setRange(inicio: Double, fim: Double) {
        txtInicio.text = if (inicio == Double.MIN_VALUE) "" else inicio.toString()
        txtFim.text = if (fim == Double.MAX_VALUE) "" else fim.toString()
    }

    fun getInicio(): Double = txtInicio.text.toDoubleOrNull() ?: Double.MIN_VALUE
    fun getFim(): Double = txtFim.text.toDoubleOrNull() ?: Double.MAX_VALUE
}
