package com.fenix.ordenararquivos.animation

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.util.Duration

class Animacao {

    val tmSincronizacao = Timeline()

    @Synchronized
    fun animaSincronizacao(img: ImageView, img1: Image?, img2: Image?) {
        if (img1 == null || img2 == null) return
        tmSincronizacao.keyFrames.clear()
        tmSincronizacao.keyFrames.addAll(KeyFrame(Duration.millis(250.0), { t: ActionEvent? -> img.image = img1 }),
            KeyFrame(Duration.millis(500.0), { t: ActionEvent? -> img.image = img2 })
        )
        tmSincronizacao.cycleCount = Animation.INDEFINITE
    }
}