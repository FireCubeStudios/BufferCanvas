package com.example.canvastest.osm;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Light;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;

public class OldView {

    Canvas canvas = new Canvas(640, 480);
    GraphicsContext gc = canvas.getGraphicsContext2D();

    Affine trans = new Affine();

    Model model;

    public OldView(Model model, Stage primaryStage) {
        this.model = model;
        primaryStage.setTitle("Draw Lines");
        BorderPane pane = new BorderPane(canvas);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();
        if(model.mapData == null) return;
        redraw();
        pan(-0.56 * model.mapData.minlon, model.mapData.maxlat);
        zoom(0, 0, canvas.getHeight() / (model.mapData.maxlat - model.mapData.minlat));
    }

    public void draw(double[] coords, String colour) {
        gc.beginPath();
        gc.setStroke(Paint.valueOf(colour));
        gc.moveTo(coords[0], coords[1]);
        for (int i = 2 ; i < coords.length ; i += 2) {
            gc.lineTo(coords[i], coords[i+1]);
        }
        gc.stroke();
    }

    void redraw() {
        long startTime = System.nanoTime();
        if(model.mapData == null) return;
        gc.setTransform(new Affine());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setTransform(trans);
        for (var way : model.mapData.ways) {
            gc.setLineWidth(1/Math.sqrt(trans.determinant()));
            gc.setStroke(Paint.valueOf("GREY"));
            boolean highWay = false;
            for (Tag tag : way.tags) {
                if (tag.Type.equalsIgnoreCase("highway")) {
                    highWay = true;
                }
            }

            if (highWay) {
                draw(way.coords, "GREEN");
            }
            else {
                draw(way.coords, "BLACK");
            }

            //Try to color between ways...
            if (model.mapData.ways.indexOf(way) < model.mapData.ways.size()) {
                gc.setFill(Color.WHITE);
                ArrayList<Double> xCoords = new ArrayList<>();
                ArrayList<Double> yCoords = new ArrayList<>();
                for (int i = 0; i<way.coords.length; i++){
                    if (i % 2 == 0) {
                        xCoords.add(way.coords[i]);
                    }
                    else{
                        yCoords.add(way.coords[i]);
                    }
                }

                double[] xCoordArray = toCoordArray(xCoords);
                double[] yCoordArray = toCoordArray(yCoords);
                int nPoints = Math.max(xCoordArray.length, yCoordArray.length);
                gc.fillPolygon(xCoordArray, yCoordArray, nPoints);
            }
        }
        System.out.println("Elapsed Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");
    }

    private double[] toCoordArray(ArrayList<Double> list){
        double[] array = new double[list.size()];
        for (int i = 0; i<array.length; i++)
            array[i] = list.get(i);
        return array;
    }

    void pan(double dx, double dy) {
        trans.prependTranslation(dx, dy);
        redraw();
    }

    void zoom(double dx, double dy, double factor) {
        pan(-dx, -dy);
        trans.prependScale(factor, factor);
        pan(dx, dy);
        redraw();
    }
}
