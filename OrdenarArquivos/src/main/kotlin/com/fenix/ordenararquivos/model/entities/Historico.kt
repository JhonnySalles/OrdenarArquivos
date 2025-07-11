package com.fenix.ordenararquivos.model.entities

import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal

data class Historico(
    val nome: String,
    val pastaOrigem: String,
    val pastaDestino: String,
    val nomeManga: String,
    val volume: String,
    val nomeArquivo: String,
    val pastaCapitulo: String,
    val inicio: String,
    val fim: String,
    val importar: String,
    val selecionado: String,
    val manga: Manga?,
    val comicInfo: ComicInfo,
    val caminhos: List<Caminhos>,
    val itens: List<String>,
    val capas: List<Capa>,
    val mal: List<Mal>
)