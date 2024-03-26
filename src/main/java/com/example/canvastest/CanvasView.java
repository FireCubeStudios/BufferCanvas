package com.example.canvastest;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class CanvasView {

    private static long lastUpdateTime = System.nanoTime();
    private static double fps = 0;

    private static final int WIDTH = 720;
    private static final int HEIGHT = 480;
    private Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private Random random = new Random();
    public CanvasView(Stage primaryStage) {
        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(canvas);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();
        var g = canvas.getGraphicsContext2D();
        // Create a ScheduledService
        ScheduledService<Void> backgroundLoop = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        long now = System.nanoTime();
                        double elapsedTime = (now - lastUpdateTime) / 1_000_000_000.0;
                        lastUpdateTime = now;
                        // Calculate FPS
                        fps = 1 / elapsedTime;
                        Platform.runLater(() -> {

                            long startTime = System.nanoTime();



                            g.setFill(Color.RED); // Change the color here
                            g.fillRect(0, 0, WIDTH, HEIGHT);
                            g.setFill(Color.WHITE);
                            for (int ii = 0; ii < 1000000; ii++) {
                                g.fillRect(random.nextInt(2, WIDTH - 1), random.nextInt(2, HEIGHT - 1), 1, 1);
                            }

                            long endTime = System.nanoTime();

                            // Calculate the elapsed time in milliseconds
                            long elapsedTimeMs = (endTime - startTime) / 1000000; // Convert nanoseconds to milliseconds

                            // Print the elapsed time
                            System.out.println("Elapsed Time: " + elapsedTimeMs + " milliseconds");
                        });
                        return null;
                    }
                };
            }
        };

        // Set the period to 60 fps ig
        backgroundLoop.setPeriod(Duration.seconds(0.016));

        // Start the service
        backgroundLoop.start();



// FPS PRINTING CODE
        ScheduledService<Void> fpsPrinter = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        System.out.println("Current FPS: " + fps);
                        return null;
                    }
                };
            }
        };
        fpsPrinter.setPeriod(Duration.seconds(1));
        fpsPrinter.start();
    }
    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}
