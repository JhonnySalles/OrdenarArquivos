package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.model.entities.Processar
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXSlider
import javafx.animation.Interpolator
import javafx.application.Platform
import javafx.beans.Observable
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Point2D
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.util.Duration
import net.kurobako.gesturefx.GesturePane
import java.io.File
import java.net.URL
import java.util.*

class PopupSumarioController : Initializable {

    @FXML
    private lateinit var apImage: AnchorPane

    @FXML
    private lateinit var sliderZoom: JFXSlider

    private lateinit var mImageView: ImageView
    private lateinit var mGesturePane: GesturePane

    private var mItem: Processar? = null
    private var mImageFile: File? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        mImageView = ImageView()
        mGesturePane = configuraZoom(apImage, mImageView, sliderZoom)
    }

    fun setDados(item: Processar, imageFile: File) {
        this.mItem = item
        this.mImageFile = imageFile
        
        if (imageFile.exists()) {
            mImageView.image = Image(imageFile.toURI().toString())
            Platform.runLater {
                mGesturePane.zoomTo(0.0, mGesturePane.targetPointAtViewportCentre())
            }
        }
    }

    private fun configuraZoom(root: AnchorPane, imageView: ImageView, slider: JFXSlider): GesturePane {
        val pane = GesturePane(imageView)
        root.children.add(0, pane)
        AnchorPane.setTopAnchor(pane, 0.0)
        AnchorPane.setLeftAnchor(pane, 0.0)
        AnchorPane.setRightAnchor(pane, 0.0)
        AnchorPane.setBottomAnchor(pane, 0.0)

        pane.minScale = 0.1
        pane.maxScale = 10.0
        
        var zoomUpdate = false
        slider.valueProperty().addListener { _: Observable? ->
            if (zoomUpdate) return@addListener
            try {
                zoomUpdate = true
                if (slider.value <= 0)
                    pane.zoomTo(0.1, pane.targetPointAtViewportCentre())
                else
                    pane.zoomTo(slider.value, pane.targetPointAtViewportCentre())
            } finally {
                zoomUpdate = false
            }
        }

        pane.currentScaleProperty().addListener { _, _, value ->
            if (zoomUpdate) return@addListener
            try {
                zoomUpdate = true
                slider.value = value.toDouble()
            } finally {
                zoomUpdate = false
            }
        }

        pane.setOnMouseClicked { e ->
            if (e.clickCount >= 2) {
                val pivotOnTarget: Point2D = pane.targetPointAt(Point2D(e.x, e.y)).orElse(pane.targetPointAtViewportCentre())
                if (e.button === MouseButton.PRIMARY) {
                    pane.animate(Duration.millis(200.0))
                        .interpolateWith(Interpolator.EASE_BOTH)
                        .zoomBy(pane.currentScale, pivotOnTarget)
                } else if (e.button === MouseButton.SECONDARY) {
                    pane.animate(Duration.millis(200.0))
                        .interpolateWith(Interpolator.EASE_BOTH)
                        .zoomTo(pane.currentScale * 0.5, pivotOnTarget)
                }
            }
        }

        return pane
    }

}
