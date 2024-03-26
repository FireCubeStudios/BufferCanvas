package com.example.canvastest;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
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

public class NEWGPUBufferCanvasView {
    // ATLEAST 2
    private static final int BUFFER_SIZE = 2;
    /*Stores fully drawn buffers.*/
    private BlockingQueue<WritableImageView> fullBuffers = new ArrayBlockingQueue<>(BUFFER_SIZE);

    /* Stores buffers that can be drawn into.*/
    private BlockingQueue<WritableImageView> emptyBuffers = new ArrayBlockingQueue<>(BUFFER_SIZE);

    private List<WritableImageView> gpuBuffers = new ArrayList<>();

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;


    private int[] BACKGROUND = new int[WIDTH * HEIGHT];

    private int[] BUFFER = new int[WIDTH * HEIGHT];

    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private Random random = new Random();



    // THE IMPORTANT GPU ACCELERATION
    private NEWPixelKernel kernel;

    private int[] points;

    private int count = 7069512;
    public NEWGPUBufferCanvasView(Stage primaryStage) {
        Setup();

        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(currentBuffer);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();

        ThreadLocalRandom random = ThreadLocalRandom.current();
        ScheduledService<Void> backgroundLoop = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        long startTime = System.nanoTime();

                        IntStream.range(0, count).parallel().filter(i -> i % 2 == 0).forEach(ii -> {
                            points[ii * 2] = random.nextInt(0, WIDTH);
                            points[(ii * 2) + 1] = random.nextInt(0, HEIGHT);
                        });
                        Draw();

                        long endTime = System.nanoTime();
                        long elapsedTimeMs = (endTime - startTime) / 1000000; // Convert nanoseconds to milliseconds
                        System.out.println("Elapsed Time: " + elapsedTimeMs + " milliseconds");

                        Platform.runLater(() -> {
                            // Update UI components
                            DrawUI(primaryStage);
                        });
                        return null;
                    }
                };
            }
        };

        // Set the period to 60 fps
        backgroundLoop.setPeriod(Duration.seconds(0.16));
        backgroundLoop.start();
    }

    void Setup(){
        IntStream.range(0, BACKGROUND.length).parallel().forEach(ii -> {
            BACKGROUND[ii] = toARGB(Color.RED);
            BUFFER[ii] = toARGB(Color.RED);
        });

        for (int i = 0; i < BUFFER_SIZE; i++)
            emptyBuffers.add(new WritableImageView(WIDTH, HEIGHT));

        kernel = new NEWPixelKernel(BUFFER, BACKGROUND, toARGB(Color.WHITE), WIDTH, HEIGHT);
        points = new int[count * 2];
    }

    void Draw(){
        try{
            // add new graphics into full buffers
            var newBuffer = emptyBuffers.take();

            kernel.setPoints(points);
            kernel.mode[0] = 0;
            kernel.put(kernel.mode);
            kernel.execute(BACKGROUND.length);

            kernel.mode[0] = 1;
            kernel.put(kernel.mode);
            kernel.execute(count);
            kernel.get(kernel.buffer);
            newBuffer.setPixels(BUFFER);

           fullBuffers.add(newBuffer);
        }
        catch (InterruptedException e) {}
    }

    private void DrawUI(Stage primaryStage){
        Platform.runLater(() -> {
            try {
                WritableImageView buffer = fullBuffers.take();


                // add code to show this new full buffer
                primaryStage.getScene().setRoot(new BorderPane(buffer));

                // add code to clear current buffer
                currentBuffer.updateBuffer();
                emptyBuffers.add(currentBuffer);

                buffer.updateBuffer();
                currentBuffer = buffer;
            }
            catch (InterruptedException e) {}
        });
    }

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}
