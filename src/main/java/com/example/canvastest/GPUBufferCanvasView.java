package com.example.canvastest;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.aparapi.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class GPUBufferCanvasView {

    private static long lastUpdateTime = System.nanoTime();
    private static double fps = 0;
    // ATLEAST 2
    private static final int BUFFER_SIZE = 2;
    /**
     * Stores fully drawn buffers.
     */
    private BlockingQueue<WritableImageView> fullBuffers = new ArrayBlockingQueue<>(BUFFER_SIZE);

    /**
     * Stores buffers that can be drawn into.
     */
    private BlockingQueue<WritableImageView> emptyBuffers = new ArrayBlockingQueue<>(BUFFER_SIZE);

    private List<WritableImageView> gpuBuffers = new ArrayList<>();

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private Random random = new Random();



    // THE IMPORTANT GPU ACCELERATION
    private PixelKernel kernel;

    private int[] points;
    public GPUBufferCanvasView(Stage primaryStage) {

        for (int i = 0; i < BUFFER_SIZE; i++)
            emptyBuffers.add(new WritableImageView(WIDTH, HEIGHT));

        kernel = new PixelKernel(BACKGROUND, toARGB(Color.RED), toARGB(Color.WHITE));
        kernel.execute(BACKGROUND.length);
        currentBuffer.setPixels(BACKGROUND);
       // kernel.setBackground(BACKGROUND, toARGB(Color.BLUE));
        kernel.dispose();

        points = new int[100000000 * 2];
        for (int ii = 0; ii < 100000000; ii+=2) {
            points[ii * 2] = random.nextInt(2, WIDTH - 1);
            points[(ii * 2) + 1] = random.nextInt(2, HEIGHT - 1);
        }


        kernel.setPoints(points, toARGB(Color.WHITE));
        kernel.setMode(1);

        long startTime = System.nanoTime();

        kernel.execute(100000000);
        currentBuffer.setPixels(BACKGROUND);

        long endTime = System.nanoTime();
        long elapsedTimeMs = (endTime - startTime) / 1000000; // Convert nanoseconds to milliseconds
        System.out.println("Elapsed Time: " + elapsedTimeMs + " milliseconds");


        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(currentBuffer);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();




        // Create a ScheduledService
        ScheduledService<Void> backgroundLoop = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        // Calculate elapsed time since the last update
                        long now = System.nanoTime();
                        double elapsedTime = (now - lastUpdateTime) / 1_000_000_000.0;
                        lastUpdateTime = now;
                        // Calculate FPS
                        fps = 1 / elapsedTime;

                        long startTime = System.nanoTime();
                        // add new graphics into full buffers
                        var newBuffer = emptyBuffers.take();
                       /* kernel.setPoints(BACKGROUND, toARGB(
                                Color.rgb(
                                        random.nextInt(256),
                                        random.nextInt(256),
                                        random.nextInt(256))));*/
                        kernel.execute(BACKGROUND.length);
                        newBuffer.setPixels(BACKGROUND);
                        kernel.dispose();

                        fullBuffers.add(newBuffer);

                        long endTime = System.nanoTime();
                        long elapsedTimeMs = (endTime - startTime) / 1000000; // Convert nanoseconds to milliseconds
                        System.out.println("Elapsed Time: " + elapsedTimeMs + " milliseconds");
                        Platform.runLater(() -> {
                            // Update UI components or perform other UI-related tasks here
                            // update UI
                            try {
                                WritableImageView buffer = fullBuffers.take();

                                // add code to show this new full buffer
                                BorderPane pane = new BorderPane(buffer);
                                Scene scene = new Scene(pane);
                                primaryStage.setScene(scene);

                                // add code to clear current buffer
                                currentBuffer.updateBuffer();
                                emptyBuffers.add(currentBuffer);

                                buffer.updateBuffer();
                                currentBuffer = buffer;
                            }
                            catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return null;
                    }
                };
            }
        };

        // Set the period to 60 fps ig
        backgroundLoop.setPeriod(Duration.seconds(0.016));

        // Start the service
       // backgroundLoop.start();



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
       // fpsPrinter.start();
    }

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}
