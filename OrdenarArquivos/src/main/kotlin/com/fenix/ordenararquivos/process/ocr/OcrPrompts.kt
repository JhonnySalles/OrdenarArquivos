package com.fenix.ordenararquivos.process.ocr

object OcrPrompts {

    private val TEXTO_PADRAO = """Esta é uma imagem de um sumário. Realize duas tarefas: extração e ordenação.
                1. Extraia os capítulos e páginas.
                2. Gere a saída ESTRITAMENTE ordenada da seguinte forma:
                   - Primeiro: Itens que começam com números, em ordem crescente (ex: 001, 002, 10).
                   - Depois: Itens que começam com letras ou texto, em ordem alfabética (ex: A, B, Apêndice, Glossário).
                   
                Use o formato: 'Número do capítulo %s Número da Página %s Descrição do capítulo'.
                Se não houver número da página, use XXX.
                
                Exemplos de saída ordenada:
                '001%s05%sIntrodução'
                '002%s12%sDesenvolvimento'
                '010%s50%sConclusão'
                'A%s90%sApêndice A'
                
                Não inclua cabeçalhos, Markdown ou explicações, apenas a lista formatada e ordenada.""".trimIndent()

    fun geraPromptSumario(separadorPagina: String, separadorCapitulo: String): String = String.format(
        TEXTO_PADRAO,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo,
        separadorPagina,
        separadorCapitulo
    )
}
