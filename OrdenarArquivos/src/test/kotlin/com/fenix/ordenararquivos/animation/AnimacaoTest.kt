package com.fenix.ordenararquivos.animation

import javafx.animation.Animation
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class AnimacaoTest {

    @Test
    fun testAnimaSincronizacaoSetup(robot: FxRobot) {
        val animacao = Animacao()
        val imageView = ImageView()
        val img1 = mock(Image::class.java)
        val img2 = mock(Image::class.java)

        robot.interact {
            animacao.animaSincronizacao(imageView, img1, img2)
        }

        val timeline = animacao.tmSincronizacao
        
        // Verifica se os keyframes foram adicionados (esperado 2)
        assertEquals(2, timeline.keyFrames.size, "Deveria ter 2 keyframes para animação")
        
        // Verifica se o ciclo é indefinido
        assertEquals(Animation.INDEFINITE, timeline.cycleCount, "Ciclo de animação deve ser indefinido")
    }

    @Test
    fun testAnimaSincronizacaoWithNullImages(robot: FxRobot) {
        val animacao = Animacao()
        val imageView = ImageView()

        robot.interact {
            animacao.animaSincronizacao(imageView, null, null)
        }

        val timeline = animacao.tmSincronizacao
        assertTrue(timeline.keyFrames.isEmpty(), "Keyframes deveriam estar vazios se imagens forem nulas")
    }
}
