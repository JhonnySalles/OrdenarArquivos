package com.fenix.ordenararquivos.model.enums

import javafx.scene.control.Tab

enum class Selecionado(var style : String) {
    SELECIONADO("comic-info-selecionado"), SELECIONAR("comic-info-selecionar"), VAZIO("comic-info-vazio");

    companion object {
        fun setTabColor(tab: Tab, status: Selecionado?) {
            val st = status ?: VAZIO

            for (e in Selecionado.values())
                tab.styleClass.remove(e.style)

            tab.styleClass.add(st.style)
        }
    }
}