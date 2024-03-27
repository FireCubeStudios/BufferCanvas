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
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private NEWPixelKernel kernel;
    private int[] points;
    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private OSMData mapData;
    public GPUView(String filename, Stage primaryStage) {
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

        Setup();

        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(currentBuffer);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();

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

        scene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double scaleFactor = Math.pow(1.1, delta / 100.0); // Adjust this factor as needed
            Matrix.a *= (float) scaleFactor;
            Matrix.d *= (float) scaleFactor;
            Draw();
        });
    }

    void Setup(){
        IntStream.range(0, BACKGROUND.length).parallel().forEach(ii -> {
            BACKGROUND[ii] = toARGB(Color.RED);
            BUFFER[ii] = toARGB(Color.RED);
        });

        kernel = new NEWPixelKernel(BUFFER, BACKGROUND, toARGB(Color.WHITE), WIDTH, HEIGHT);

        int totalPoints = 0;
        for (var way : mapData.ways)
            totalPoints += way.coords.length;

        points = new int[totalPoints * 100];
        int currentIndex = 0;
        var scale = HEIGHT / (mapData.maxlat - mapData.minlat);
        int ogx = (int)(mapData.ways.getFirst().coords[0] * scale);
        int ogy = (int)(mapData.ways.getFirst().coords[1] * scale);
        for (var way : mapData.ways) {
            for(int i = 0; i < way.coords.length - 4; i += 4)
            {
                int x = ((int)((way.coords[i] * scale))) - ogx;
                int y = ((int)((way.coords[i + 1] * scale))) - ogy;
                int x2 = ((int)((way.coords[ + 2] * scale))) - ogx;
                int y2 = ((int)((way.coords[i + 3] * scale))) - ogy;
                var l = new Line(x* 2, y* 2, x2 * 2, y2* 2, 1, Color.WHITE);
                for (int shapePoint : l.getPoints())
                    points[currentIndex++] = shapePoint;
            }
        }
       /* for (var way : model.mapData.ways) {
            for(int i = 0; i < way.coords.length - 4; i += 4)
            {
                var l = new Line((int) way.coords[i], (int) way.coords[i + 1], (int) way.coords[i + 2], (int) way.coords[i + 3], 1, Color.WHITE);
                for (int shapePoint : l.getPoints())
                    points[currentIndex++] = shapePoint;
            }
        }*/

        kernel.setPoints(points);
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
