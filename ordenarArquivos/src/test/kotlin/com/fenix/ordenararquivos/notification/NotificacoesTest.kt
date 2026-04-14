package com.fenix.ordenararquivos.notification

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.enums.Notificacao
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.scene.layout.AnchorPane
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Tag("UI")
class NotificacoesTest : BaseJfxTest() {

    @Test
    fun `deve inicializar e apresentar notificacao sem erro`() {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        Platform.runLater {
            try {
                val root = AnchorPane()
                
                // Adicionar listener para detectar quando a notificação for inserida
                root.children.addListener(ListChangeListener { change ->
                    while (change.next()) {
                        if (change.wasAdded() && change.addedSubList.contains(Notificacoes.NOTIFICACAO)) {
                            latch.countDown()
                        }
                    }
                })

                Notificacoes.rootAnchorPane = root
                Notificacoes.notificacao(Notificacao.SUCESSO, "Titulo Teste", "Mensagem Teste")
            } catch (e: Throwable) {
                error = e
                latch.countDown()
            }
        }

        // Aguardar na thread de TESTE (não na JFX thread)
        if (!latch.await(5, TimeUnit.SECONDS)) {
            error = AssertionError("Timeout aguardando inserção da notificação no root")
        }

        if (error != null) throw error!!
        
        // Verificação final fora da JFX thread (mas Notificacoes.NOTIFICACAO é seguro ler)
        assertTrue(Notificacoes.NOTIFICACAO.parent != null, "A notificação deveria estar anexada a um parent")
    }

    @Test
    fun `deve alternar tipo de notificacao corretamente`() {
        val latch = CountDownLatch(1)
        Platform.runLater {
            try {
                Notificacoes.rootAnchorPane = AnchorPane()
                Notificacoes.notificacao(Notificacao.ERRO, "Erro", "Mensagem de erro")
            } finally {
                latch.countDown()
            }
        }
        latch.await(5, TimeUnit.SECONDS)
    }
}
