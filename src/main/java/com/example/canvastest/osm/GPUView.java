package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.*;
import com.example.canvastest.Line;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

public class GPUView {
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private NEWERPIXELKERNEL kernel;
    private int[] points;

    private int[] pointsARGB;
    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private OSMData mapData;
    private Stage primaryStage;
    private Scene scene;
    public GPUView(String filename, Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            InputStream osmInputStream = null;
            if (filename.endsWith(".osm.zip")) {
                ZipInputStream input  = new ZipInputStream(new FileInputStream(filename));
                input.getNextEntry();
                osmInputStream = input;
            } else if (filename.endsWith(".osm")) {
                osmInputStream = new FileInputStream(filename);
            }
            OSMParser osmParser = new OSMParser();
            mapData = osmParser.getMapData(osmInputStream);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(currentBuffer);
        scene = new Scene(pane);
        scene.setRoot(pane);
        primaryStage.setScene(scene);
        primaryStage.show();

        Setup();

        scene.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
        });
        scene.setOnMouseDragged(e -> {
            double dx = e.getX() - lastX;
            double dy = e.getY() - lastY;
            Matrix.e += (float) dx;
            Matrix.f += (float) dy;
            Draw();
            lastX = e.getX();
            lastY = e.getY();
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            Resize();
            Draw();
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            Resize();
            Draw();
        });
        scene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double scaleFactor = Math.pow(1.1, delta / 100.0); // Adjust this factor as needed
            Matrix.a *= (float) scaleFactor;
            Matrix.d *= (float) scaleFactor;
            Draw();
        });
    }

    void Resize()
    {
        WIDTH = (int) primaryStage.getWidth();
        HEIGHT = (int) primaryStage.getHeight();
        BUFFER = new int[WIDTH * HEIGHT];
        BACKGROUND = new int[WIDTH * HEIGHT];
        currentBuffer = new WritableImageView(WIDTH, HEIGHT);
        scene.setRoot(new BorderPane(currentBuffer));
        IntStream.range(0, BACKGROUND.length).parallel().forEach(ii -> {
            BACKGROUND[ii] = toARGB(Color.LIGHTBLUE);
            BUFFER[ii] = toARGB(Color.LIGHTBLUE);
        });
        kernel.resize(BUFFER, BACKGROUND, WIDTH, HEIGHT);
    }

    void Setup(){

        kernel = new NEWERPIXELKERNEL(BUFFER, BACKGROUND, WIDTH, HEIGHT);

        long startTime = System.nanoTime();


var t = mapData.minlat;
var g = mapData.maxlat;
        var scale = (HEIGHT / (mapData.maxlat - mapData.minlat));
        int ogx = (int)(mapData.ways.getFirst().coords[0] * scale);
        int ogy = (int)(mapData.ways.getFirst().coords[1] * scale);

        int totalPoints = 0;
        int scaleO = 1;
        for (var way : mapData.ways){
            for (int i = 0; i < way.coords.length - 2; i += 2) {
                int x = ((int) ((way.coords[i] * scale))) - ogx;
                int y = ((int) ((way.coords[i + 1] * scale))) - ogy;
                var l = new Point(x * scaleO, y * scaleO, 1, Color.WHITE);
                totalPoints += l.getPoints().length;
            }
        }


        points = new int[totalPoints];
        pointsARGB = new int[totalPoints];

        int currentIndex = 0;
        for (var way : mapData.ways) {
            for (int i = 0; i < way.coords.length - 2; i += 2) {
                int x = ((int) ((way.coords[i] * scale))) - ogx;
                int y = ((int) ((way.coords[i + 1] * scale))) - ogy;
                var l = new Point(x * scaleO, y * scaleO, 1, Color.WHITE);
                var pointsX = l.getPoints();
                for (int ii = 0; i < pointsX.length - 1; i += 2) {
                    int iii = currentIndex += 2;
                    points[iii] = pointsX[ii];
                    points[iii + 1] = pointsX[ii + 1];
                    pointsARGB[iii] = toARGB(Color.BLACK);
                    pointsARGB[iii + 1] = toARGB(Color.BLACK);
                }
            }
        }
        System.out.println(totalPoints);
        System.out.println(currentIndex);
        System.out.println(points.length);

        Resize();
        kernel.setPoints(points, pointsARGB);
        System.out.println("Elapsed Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");

        Draw();
    }

    void pan(double dx, double dy) {
        Matrix.e += (float) dx;
        Matrix.f += (float) dy;
    }

    void zoom(double dx, double dy, double scaleFactor) {
        pan(-dx, -dy);
        Matrix.a *= (float) scaleFactor;
        Matrix.d *= (float) scaleFactor;
        pan(dx, dy);
    }

    void Draw(){
        long startTime = System.nanoTime();

        kernel.setTransform(Matrix);
        kernel.setMode(0);
        kernel.execute(Range.create(BACKGROUND.length));

        kernel.setMode(1);
        kernel.execute(Range.create(points.length / 2));
        kernel.get(kernel.buffer);
        currentBuffer.updateBuffer();
        currentBuffer.setPixels(BUFFER);

        System.out.println("Elapsed Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");
    }

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}
