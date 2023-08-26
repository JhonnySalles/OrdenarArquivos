package com.fenix.ordenararquivos.componentes

import com.jfoenix.controls.JFXSlider
import javafx.beans.Observable
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent

object ImageViewZoom {
    private const val MAX_ZOOM = 5
    fun limpa(imageView: ImageView, slider: JFXSlider) {
        imageView.onMousePressed = null
        imageView.onMouseDragged = null
        imageView.onMouseClicked = null
        slider.valueProperty().addListener { e: Observable? -> }
    }

    fun configura(image: Image?, imageView: ImageView, slider: JFXSlider) {
        imageView.image = image
        if (image == null) {
            limpa(imageView, slider)
            return
        }
        val width = image.width
        val height = image.height
        imageView.isPreserveRatio = true
        reset(imageView, width, height)
        val mouseDown: ObjectProperty<Point2D> = SimpleObjectProperty()
        imageView.onMousePressed = EventHandler { e: MouseEvent ->
            val mousePress = imageViewToImage(imageView, Point2D(e.x, e.y))
            mouseDown.set(mousePress)
        }
        imageView.onMouseDragged = EventHandler { e: MouseEvent ->
            val dragPoint = imageViewToImage(imageView, Point2D(e.x, e.y))
            shift(imageView, dragPoint.subtract(mouseDown.get()))
            mouseDown.set(imageViewToImage(imageView, Point2D(e.x, e.y)))
        }
        imageView.onMouseClicked = EventHandler { e: MouseEvent ->
            if (e.clickCount == 2) {
                slider.valueProperty().set(1.0)
                reset(imageView, width, height)
            }
        }
        imageView.onScroll = EventHandler { e: ScrollEvent ->
            if (slider.value >= slider.min || slider.value <= slider.max) {
                if (e.deltaY > 0) slider.value = slider.value * 1.1 else if (e.deltaY < 0) slider.value =
                    slider.value / 1.1
            }
        }
        slider.min = 1.0
        slider.max = 10.0
        slider.blockIncrement = 0.1
        slider.value = 1.0
        slider.valueProperty().addListener { e: Observable? ->
            val zoom = slider.value
            val viewport = imageView.viewport
            if (zoom != 1.0) {
                val mouse = imageViewToImage(
                    imageView,
                    Point2D(image.width / MAX_ZOOM, image.height / MAX_ZOOM)
                )
                var newWidth = image.width
                var newHeight = image.height
                val imageViewRatio = imageView.fitWidth / imageView.fitHeight
                val viewportRatio = newWidth / newHeight
                if (viewportRatio < imageViewRatio) {
                    newHeight = newHeight / zoom
                    newWidth = newHeight / imageViewRatio
                    if (newWidth > image.width) {
                        newWidth = image.width
                    }
                } else {
                    newWidth = newWidth / zoom
                    newHeight = newWidth / imageViewRatio
                    if (newHeight > image.height) {
                        newHeight = image.height
                    }
                }
                var newMinX = 0.0
                if (newWidth < image.width) newMinX =
                    clamp(mouse.x - (mouse.x - viewport.minX) / zoom, 0.0, width - newWidth)
                var newMinY = 0.0
                if (newHeight < image.height) newMinY =
                    clamp(mouse.y - (mouse.y - viewport.minY) / zoom, 0.0, height - newHeight)
                imageView.viewport = Rectangle2D(newMinX, newMinY, newWidth, newHeight)
            } else reset(imageView, width, height)
        }
    }

    private fun reset(imageView: ImageView, width: Double, height: Double) {
        imageView.viewport = Rectangle2D(0.0, 0.0, width, height)
    }

    private fun shift(imageView: ImageView, delta: Point2D) {
        val viewport = imageView.viewport
        val width = imageView.image.width
        val height = imageView.image.height
        val maxX = width - viewport.width
        val maxY = height - viewport.height
        val minX = clamp(viewport.minX - delta.x, 0.0, maxX)
        val minY = clamp(viewport.minY - delta.y, 0.0, maxY)
        imageView.viewport = Rectangle2D(minX, minY, viewport.width, viewport.height)
    }

    private fun clamp(value: Double, min: Double, max: Double): Double {
        if (value < min) return min
        return if (value > max) max else value
    }

    private fun imageViewToImage(imageView: ImageView, imageViewCoordinates: Point2D): Point2D {
        val xProportion = imageViewCoordinates.x / imageView.boundsInLocal.width
        val yProportion = imageViewCoordinates.y / imageView.boundsInLocal.height
        val viewport = imageView.viewport
        return Point2D(
            viewport.minX + xProportion * viewport.width,
            viewport.minY + yProportion * viewport.height
        )
    }
}