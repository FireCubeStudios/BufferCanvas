package com.example.canvastest.osm;

import javafx.geometry.Point2D;

public class Controller {

    double lastX;
    double lastY;

    public Controller(Model model, OldView view) {
        view.canvas.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
        });
        view.canvas.setOnMouseDragged(e -> {
                double dx = e.getX() - lastX;
                double dy = e.getY() - lastY;
                view.pan(dx, dy);
                view.redraw();

            lastX = e.getX();
            lastY = e.getY();
        });
        view.canvas.setOnScroll(e -> {
            double factor = e.getDeltaY();
            view.zoom(e.getX(), e.getY(), Math.pow(1.01, factor));
        });
    }



}
