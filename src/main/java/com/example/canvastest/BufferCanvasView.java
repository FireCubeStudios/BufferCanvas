package com.example.canvastest;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.Scene;
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

public class BufferCanvasView {

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

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private Random random = new Random();
    private IDrawable[] points = new IDrawable[1000];

    private Transform Matrix = new Transform();
    public BufferCanvasView(Stage primaryStage) {

        setup();

        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(currentBuffer);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();

        ThreadLocalRandom random = ThreadLocalRandom.current();
        IntStream.range(0, 1000).parallel().forEach(ii -> {
            if(ii % 2 == 0) // add random point
                points[ii] = new Point(random.nextInt(2, WIDTH - 1), random.nextInt(2, HEIGHT - 1), random.nextInt(1, 5),
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
                    xPoints[i] = rand.nextInt(2, WIDTH - 4); // Limiting x-coordinates to 400 for example
                    yPoints[i] = rand.nextInt(2, HEIGHT - 4); // Limiting y-coordinates to 400 for example
                }
                points[ii] = new Polygon(xPoints, yPoints,
                        Color.rgb(
                                random.nextInt(256),
                                random.nextInt(256),
                                random.nextInt(256)));
            }
            else // add random line
                points[ii] = new Line(random.nextInt(2, WIDTH - 4), random.nextInt(2, HEIGHT - 4),
                random.nextInt(2, WIDTH - 300), random.nextInt(2, HEIGHT - 300), random.nextInt(1, 5),
                        Color.rgb(
                                random.nextInt(256),
                                random.nextInt(256),
                                random.nextInt(256)));
        });

        Draw();
        DrawUI(primaryStage);
       // setupFPS();

        ScheduledService<Void> translator = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                    return new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                        Matrix.e +=  random.nextInt(-5, 5);
                     //   Matrix.f +=  random.nextInt(-5, 5);

                       IntStream.range(0, points.length - 1).parallel().forEach(ii -> {
                            points[ii].transform(Matrix);
                        });
                        Draw();
                            Platform.runLater(() -> {
                                // Update UI components or perform other UI-related tasks here
                                // update UI
                                DrawUI(primaryStage);

                            });
                            return null;
                        }
                    };
                }
            };
        translator.setPeriod(Duration.seconds(0.016));
        translator.start();
    }

    void setup()
    {
        // MAKE BACKGROUND BUFFER A RANDOM COLOR
        Arrays.fill(BACKGROUND, toARGB(
                Color.rgb(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256))));

        // FILL EMPTY BUFFERS WITH NEW CANVAS
        for (int i = 0; i < BUFFER_SIZE; i++)
            emptyBuffers.add(new WritableImageView(WIDTH, HEIGHT));
    }
private int count = 0;
    public void Draw()
    {
        long now = System.nanoTime();
        double elapsedTime = (now - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = now;
        fps = 1 / elapsedTime;
        long startTime = System.nanoTime();

        try {
            // add new ui into full buffers
            var newBuffer = emptyBuffers.take();
            newBuffer.setPixels(BACKGROUND);
            // Using parallel streams to parallelize the loop
            IntStream.range(0, points.length - 1).parallel().forEach(ii -> {

                int[] positions = points[ii].getPoints();
                if(positions.length < 1000)
                    for(int i = 0; i < positions.length - 1; i += 2) {
                        count++;
                        newBuffer.setArgb(positions[i], positions[i + 1], toARGB(points[ii].getColor()));
                    }
                else
                    IntStream.range(0, positions.length - 1)
                    .filter(i -> i % 2 == 0)
                    .forEach(i -> {
                        count++;
                        newBuffer.setArgb(positions[i], positions[i + 1], toARGB(points[ii].getColor()));
                    });
            });

            fullBuffers.add(newBuffer);
        }
        catch(InterruptedException e) {
        }

        long endTime = System.nanoTime();
        long elapsedTimeMs = (endTime - startTime) / 1000000;
        System.out.println("Elapsed Time: " + elapsedTimeMs + " milliseconds");
    }

    void DrawUI(Stage primaryStage){
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

    void setupDrawing(Stage primaryStage)
    {
        ScheduledService<Void> backgroundLoop = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        long now = System.nanoTime();
                        double elapsedTime = (now - lastUpdateTime) / 1_000_000_000.0;
                        lastUpdateTime = now;
                        fps = 1 / elapsedTime;


                        long startTime = System.nanoTime();

                        Draw();

                        long endTime = System.nanoTime();
                        long elapsedTimeMs = (endTime - startTime) / 1000000;
                        System.out.println("Elapsed Time: " + elapsedTimeMs + " milliseconds");

                        Platform.runLater(() -> {
                            // Update UI components or perform other UI-related tasks here
                            // update UI

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
    }
    void setupFPS()
    {
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

    void redraw(WritableImageView buffer) {
        // draw background into buffer
        buffer.setPixels(BACKGROUND);
    }

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}
