package com.example.canvastest;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class GPUBufferUICanvasView {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;


    private int[] BACKGROUND = new int[WIDTH * HEIGHT];

    private int[] BUFFER = new int[WIDTH * HEIGHT];

    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);

    // THE IMPORTANT GPU ACCELERATION
    private NEWPixelKernel kernel;

    private int shapeCount = 5000;
    private IDrawable[] shapes = new IDrawable[shapeCount];
    private int[] points;

    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();

    public GPUBufferUICanvasView(Stage primaryStage) {
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
            Matrix.e += dx;
            Matrix.f += dy;
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
        drawShapes();

        int totalPoints = 0;
        for (IDrawable shape : shapes)
            totalPoints += shape.getPoints().length;
        System.out.println(totalPoints);
        points = new int[totalPoints];

        int currentIndex = 0;
        for (IDrawable shape : shapes)
            for (int shapePoint : shape.getPoints())
                    points[currentIndex++] = shapePoint;
        kernel.setPoints(points);
        Draw();
    }

    void Draw(){
        long startTime = System.nanoTime();

            kernel.setTransform(Matrix);
            kernel.setMode(0);
            kernel.put(kernel.mode);
            kernel.execute(BACKGROUND.length);

            kernel.setMode(1);
            kernel.execute(points.length / 2);
            kernel.get(kernel.buffer);
            //currentBuffer = new WritableImageView(WIDTH, HEIGHT);
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

    void drawShapes()
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        IntStream.range(0, shapeCount).parallel().forEach(ii -> {
            if(ii % 2 == 0) // add random point
                shapes[ii] = new Point(random.nextInt(2, WIDTH), random.nextInt(2, HEIGHT), random.nextInt(1, 5),
                        Color.rgb(
                                random.nextInt(256),
                                random.nextInt(256),
                                random.nextInt(256)));
            else if(ii % 5 == 0) {
                // Generate random vertices for the polygon
                int numVertices = 5; // Number of vertices
                int[] xPoints = new int[numVertices];
                int[] yPoints = new int[numVertices];
                Random rand = new Random();
                for (int i = 0; i < numVertices; i++) {
                    xPoints[i] = rand.nextInt(2, WIDTH); // Limiting x-coordinates to 400 for example
                    yPoints[i] = rand.nextInt(2, HEIGHT); // Limiting y-coordinates to 400 for example
                }
                shapes[ii] = new Polygon(xPoints, yPoints,
                        Color.rgb(
                                random.nextInt(256),
                                random.nextInt(256),
                                random.nextInt(256)));
            }
            else {
                shapes[ii] = new Line(random.nextInt(2, WIDTH * 2), random.nextInt(2, HEIGHT * 2),
                        random.nextInt(2, WIDTH * 2), random.nextInt(2, HEIGHT * 2), random.nextInt(1, 5),
                        Color.rgb(
                                random.nextInt(256),
                                random.nextInt(256),
                                random.nextInt(256)));
            }
        });
    }
}
