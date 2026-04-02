package com.fenix.ordenararquivos.notification

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.controller.PopupAlertaController
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class AlertasTest : BaseJfxTest() {

    @Test
    fun `deve ser capaz de carregar o FXML do PopupAlertaController`() {
        val latch = CountDownLatch(1)
        Platform.runLater {
            try {
                val loader = FXMLLoader(PopupAlertaController.fxmlLocate)
                assertNotNull(loader.location, "O local do FXML não deve ser nulo")
                
                // Tenta carregar apenas a estrutura para validar que o FXML é válido e recursos existem
                val root = loader.load<Any>()
                assertNotNull(root, "Deveria ter carregado o FXML")
                
                val controller = loader.getController<PopupAlertaController>()
                assertNotNull(controller, "O controller deveria ter sido inicializado")
            } finally {
                latch.countDown()
            }
        }
        latch.await()
    }
}
