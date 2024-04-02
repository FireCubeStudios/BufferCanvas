package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.NEWERPIXELKERNEL;
import com.example.canvastest.Point;
import com.example.canvastest.Transform;
import com.example.canvastest.WritableImageView;
import com.wolt.osm.parallelpbf.ParallelBinaryParser;
import com.wolt.osm.parallelpbf.entity.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

public class PBFGPUView {
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private NEWERPIXELKERNEL kernel;

    private int[] pointsARGB;
    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private Stage primaryStage;
    private Scene scene;
    private final Object lock = new Object();
    private int[] points = new int[48194627 * 2];
    private int nodesIndex = 0;

    private double scale = 82.62484915943543 * 10;
    private double ogx = 55.713486499999945 * scale;
    private double ogy = 11.723541300000003 * scale;

    private void processNodes(Node node) {
        synchronized (lock) {
            points[nodesIndex] = (int) (((node.getLon() * scale) - ogy));
            points[nodesIndex + 1] = (int) -(((node.getLat() * scale) - ogx));
            nodesIndex += 2;
        }
    }
    public PBFGPUView(String filename, Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            new ParallelBinaryParser(new FileInputStream(filename), 100)
                    .onComplete(this::complete)
                    .onNode(this::processNodes)
                    .parse();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void complete()
    {
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

        pointsARGB = new int[nodesIndex * 2];
        for (int i = 0; i < (nodesIndex * 2); i++)
            pointsARGB[i] = toARGB(Color.BLACK);
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
