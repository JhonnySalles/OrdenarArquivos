package com.fenix.ordenararquivos.componentes;

import com.jfoenix.controls.JFXSlider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageViewZoom {

	private static final int MAX_ZOOM = 5;

	public static void limpa(ImageView imageView, JFXSlider slider) {
		if (imageView == null || slider == null)
			return;

		imageView.setOnMousePressed(null);
		imageView.setOnMouseDragged(null);
		imageView.setOnMouseClicked(null);
		slider.valueProperty().addListener(e -> {});
	}

	public static void configura(Image image, ImageView imageView, JFXSlider slider) {
		imageView.setImage(image);

		if (image == null) {
			limpa(imageView, slider);
			return;
		}

		double width = image.getWidth();
		double height = image.getHeight();

		imageView.setPreserveRatio(true);
		reset(imageView, width, height);
		ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

		imageView.setOnMousePressed(e -> {
			Point2D mousePress = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
			mouseDown.set(mousePress);
		});

		imageView.setOnMouseDragged(e -> {
			Point2D dragPoint = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
			shift(imageView, dragPoint.subtract(mouseDown.get()));
			mouseDown.set(imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
		});

		imageView.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				slider.valueProperty().set(1);
				reset(imageView, width, height);
			}
		});

		imageView.setOnScroll(e -> {
			if (slider.getValue() >= slider.getMin() || slider.getValue() <= slider.getMax()) {
				if (e.getDeltaY() > 0)
					slider.setValue(slider.getValue() * 1.1);
				else if (e.getDeltaY() < 0)
					slider.setValue(slider.getValue() / 1.1);
			}
		});

		slider.setMin(1);
		slider.setMax(10);
		slider.setBlockIncrement(0.1);
		slider.setValue(1);

		slider.valueProperty().addListener(e -> {
			double zoom = slider.getValue();
			Rectangle2D viewport = imageView.getViewport();
			if (zoom != 1.0) {
				Point2D mouse = imageViewToImage(imageView,
						new Point2D(image.getWidth() / MAX_ZOOM, image.getHeight() / MAX_ZOOM));

				double newWidth = image.getWidth();
				double newHeight = image.getHeight();
				double imageViewRatio = (imageView.getFitWidth() / imageView.getFitHeight());
				double viewportRatio = (newWidth / newHeight);
				if (viewportRatio < imageViewRatio) {
					newHeight = newHeight / zoom;
					newWidth = newHeight / imageViewRatio;
					if (newWidth > image.getWidth()) {
						newWidth = image.getWidth();
					}
				} else {
					newWidth = newWidth / zoom;
					newHeight = newWidth / imageViewRatio;
					if (newHeight > image.getHeight()) {
						newHeight = image.getHeight();
					}
				}

				double newMinX = 0;
				if (newWidth < image.getWidth()) {
					newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) / zoom, 0, width - newWidth);
				}
				double newMinY = 0;
				if (newHeight < image.getHeight()) {
					newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) / zoom, 0, height - newHeight);
				}

				imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
			} else
				reset(imageView, width, height);
		});

	}

	private static void reset(ImageView imageView, double width, double height) {
		imageView.setViewport(new Rectangle2D(0, 0, width, height));
	}

	private static void shift(ImageView imageView, Point2D delta) {
		Rectangle2D viewport = imageView.getViewport();

		double width = imageView.getImage().getWidth();
		double height = imageView.getImage().getHeight();

		double maxX = width - viewport.getWidth();
		double maxY = height - viewport.getHeight();

		double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
		double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

		imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
	}

	private static double clamp(double value, double min, double max) {

		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}

	private static Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
		double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
		double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

		Rectangle2D viewport = imageView.getViewport();
		return new Point2D(viewport.getMinX() + xProportion * viewport.getWidth(),
				viewport.getMinY() + yProportion * viewport.getHeight());
	}

}
