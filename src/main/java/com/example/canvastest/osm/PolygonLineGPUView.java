package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.*;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

public class PolygonLineGPUView{
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private FIXED2POLYGONKERNEL kernel;
    private int[] lines;

    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private OSMData mapData;
    private Stage primaryStage;
    private Scene scene;
    public PolygonLineGPUView(String filename, Stage primaryStage) {
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

        scene.setOnMouseReleased(e ->{

           /* points();
            Matrix.a = 1;
            Matrix.d = 1;
            Draw();*/
        });
        scene.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
            Draw();
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
        lines = new int[78710 * 6];
        Matrix.a = 20;
        Matrix.d = 20;
        BACKGROUND = new int[2];
        BACKGROUND[0] = toARGB(Color.LIGHTBLUE);
        BACKGROUND[1] = toARGB(Color.RED);
        BUFFER = new int[WIDTH * HEIGHT];
        WIDTH = (int) primaryStage.getWidth();
        HEIGHT = (int) primaryStage.getHeight();

        kernel = new FIXED2POLYGONKERNEL(BUFFER, BACKGROUND, WIDTH, HEIGHT);
        points();
        Resize();

        Draw();
    }
    void Resize()
    {
        WIDTH = (int) primaryStage.getWidth();
        HEIGHT = (int) primaryStage.getHeight();
        BUFFER = new int[WIDTH * HEIGHT];

        currentBuffer = new WritableImageView(WIDTH, HEIGHT);
        scene.setRoot(new BorderPane(currentBuffer));
        IntStream.range(0, BUFFER.length).parallel().forEach(ii -> {
            BUFFER[ii] = toARGB(Color.LIGHTBLUE);
        });
        kernel.resize(BUFFER, WIDTH, HEIGHT);
    }

    private void points()
    {
        long startTime = System.nanoTime();

        double scale = (HEIGHT / (mapData.maxlat - mapData.minlat)) * Matrix.a;
        double ogx = (mapData.ways.getFirst().coords[0] * scale);
        double ogy = (mapData.ways.getFirst().coords[1] * scale);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int currentIndex = 0;
        for (var way : mapData.ways) {
            int c = toARGB(Color.rgb(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)));
            boolean fill = way.coords[0] == way.coords[way.coords.length - 2] && way.coords[1] == way.coords[way.coords.length - 1];
            for (int i = 0; i < way.coords.length - 2; i += 2) {
                if(currentIndex > lines.length - 10) break;

                int x = (int)(((way.coords[i] * scale)) - ogx);
                int y = (int)(((way.coords[i + 1] * scale)) - ogy);
                int x2 = (int)(((way.coords[i + 2] * scale)) - ogx);
                int y2 = (int)(((way.coords[i + 3] * scale)) - ogy);

                currentIndex += 6;
                int iii = currentIndex;
                lines[iii] = x;
                lines[iii + 1] = y;
                lines[iii + 2] = x2;
                lines[iii + 3] = y2;
                lines[iii + 4] = c;
                if(fill)
                    lines[iii + 5] = 1;
                else
                    lines[iii + 5] = 0;
            }
        }

        kernel.setLines(lines);
        System.out.println("Points Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");

        System.out.println(currentIndex);
        System.out.println(lines.length);
    }

    void Draw(){
        long startTime = System.nanoTime();

        kernel.setTransform(Matrix);
        kernel.setMode(0);
        kernel.execute(Range.create(BUFFER.length));

        kernel.setMode(1);
        kernel.execute(Range.create(lines.length / 6));

        kernel.setMode(2);
        kernel.execute(Range.create(WIDTH));

        kernel.setMode(3);
        kernel.execute(Range.create(WIDTH * 100));

        kernel.setMode(4);
        kernel.execute(Range.create(HEIGHT));

        kernel.setMode(5);
        kernel.execute(Range.create(BUFFER.length));

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
