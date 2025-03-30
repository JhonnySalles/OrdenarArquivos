package com.fenix.ordenararquivos.model.enums

/**
 *
 *
 * Enuns utilizado pelo popup de notificações.
 *
 *
 *
 *
 * **ALERTA, AVISO, ERRO, SUCESSO**
 *
 *
 * @author Jhonny de Salles Noschang
 */
enum class Notificacao(val descricao: String) {
    ALERTA("Alerta"), AVISO("Aviso"), ERRO("Erro"), SUCESSO("Sucesso");

    // Necessário para que a escrita do combo seja Ativo e não ATIVO (nome do enum)
    @Override
    override fun toString(): String {
        return descricao
    }
}