package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.SimpleLineKernel;
import com.example.canvastest.Transform;
import com.example.canvastest.WritableImageView;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

public class CPUBufferCanvas {
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);

    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private OSMData mapData;
    private Stage primaryStage;
    private Scene scene;
    public CPUBufferCanvas(String filename, Stage primaryStage) {
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
        Matrix.a = 20;
        Matrix.d = 20;
        WIDTH = (int) primaryStage.getWidth();
        HEIGHT = (int) primaryStage.getHeight();
        BUFFER = new int[WIDTH * HEIGHT];
        currentBuffer = new WritableImageView(WIDTH, HEIGHT);
        scene.setRoot(new BorderPane(currentBuffer));
        IntStream.range(0, BUFFER.length).parallel().forEach(ii -> {
            BUFFER[ii] = toARGB(Color.LIGHTBLUE);
        });

        Draw();
    }

    /** 6ms **/
    private void Draw()
    {
        long startTime = System.nanoTime();

        IntStream.range(0, BUFFER.length).parallel().forEach(ii -> {
            BUFFER[ii] = toARGB(Color.LIGHTBLUE);
        });

        double scale = (HEIGHT / (mapData.maxlat - mapData.minlat)) * Matrix.a;
        double ogx = (mapData.ways.getFirst().coords[0] * scale);
        double ogy = (mapData.ways.getFirst().coords[1] * scale);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        IntStream.range(0, mapData.ways.size()).parallel().forEach(ii -> {
            var way = mapData.ways.get(ii);
            if(way.coords[0] == way.coords[way.coords.length - 2] && way.coords[1] == way.coords[way.coords.length - 1])
            {
                boolean isInside = false;
                int[] converted = new int[way.coords.length];

                // Iterate over the doubles array and apply conversions
                for (int i = 0; i < way.coords.length; i += 2) {
                    converted[i] = (int) (((way.coords[i] * scale) + Matrix.e) - ogx);
                    converted[i + 1] = (int) (((way.coords[i + 1] * scale) + Matrix.f) - ogy);
                    if(converted[i] > 0 && converted[i] < WIDTH && converted[i + 1] > 0 && converted[i + 1] < HEIGHT)
                        isInside = true;
                }
                if(isInside)
                    DrawPolygon(converted, toARGB(Color.RED));
            }
            else {
                for (int i = 0; i < way.coords.length - 2; i += 2) {
                    int x = (int) (((way.coords[i] * scale) + Matrix.e) - ogx);
                    int y = (int) (((way.coords[i + 1] * scale) + Matrix.f) - ogy);
                    int x2 = (int) (((way.coords[i + 2] * scale) + Matrix.e) - ogx);
                    int y2 = (int) (((way.coords[i + 3] * scale) + Matrix.f) - ogy);
                    DrawLine(x, y, x2, y2, toARGB(Color.BLACK));
                }
            }
        });

        System.out.println("Draw Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");


        currentBuffer.updateBuffer();
        currentBuffer.setPixels(BUFFER);
    }

    /** 20 ms**/
    private void DrawOld()
    {
        long startTime = System.nanoTime();

        IntStream.range(0, BUFFER.length).parallel().forEach(ii -> {
            BUFFER[ii] = toARGB(Color.LIGHTBLUE);
        });

        double scale = (HEIGHT / (mapData.maxlat - mapData.minlat)) * Matrix.a;
        double ogx = (mapData.ways.getFirst().coords[0] * scale);
        double ogy = (mapData.ways.getFirst().coords[1] * scale);
        int currentIndex = 0;
        for (var way : mapData.ways) {
            for (int i = 0; i < way.coords.length - 2; i += 2) {

                int x = (int)(((way.coords[i] * scale) + Matrix.e) - ogx);
                int y = (int)(((way.coords[i + 1] * scale) + Matrix.f) - ogy);
                int x2 = (int)(((way.coords[i + 2] * scale) + Matrix.e) - ogx);
                int y2 = (int)(((way.coords[i + 3] * scale) + Matrix.f) - ogy);
                DrawLine(x, y, x2, y2, toARGB(Color.BLACK));
            }
        }

        System.out.println("Draw Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");

        System.out.println(currentIndex);

        currentBuffer.updateBuffer();
        currentBuffer.setPixels(BUFFER);
    }

    void DrawPolygon(int[] vertices, int c){
        // Find the minimum and maximum y-coordinate to determine the scanline range
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 1; i < vertices.length; i += 2) {
            minY = Math.min(minY, vertices[i]);
            maxY = Math.max(maxY, vertices[i]);
        }
    // For each horizontal scanline, check if it intersects with the polygon edges
        for (int y = minY; y <= maxY; y++) {
        int[] horizontalPairs = new int[vertices.length]; // We'll use this array to store pairs of x-values

        // Count the number of intersections with the current scanline
        int count = 0;

        // Scan the edges of the polygon
        for (int i = 0; i < vertices.length; i += 2) {
            int x1 = vertices[i];
            int y1 = vertices[i + 1];
            int j = (i + 2) % vertices.length; // Wrap around to the first vertex
            int x2 = vertices[j];
            int y2 = vertices[j + 1];

            // Check if the current edge intersects with the current scanline
            if ((y1 <= y && y < y2) || (y2 <= y && y < y1)) {
                // Calculate the x-coordinate of the intersection point
                int xIntersect = (int) (x1 + (double) (y - y1) / (y2 - y1) * (x2 - x1));

                // Store the x-coordinate of the intersection point
                horizontalPairs[count++] = xIntersect;
            }
        }

        // Pair up the intersection points to form horizontal line segments
        for (int i = 0; i < count; i += 2) {
            int x1 = horizontalPairs[i];
            int x2 = horizontalPairs[i + 1];
            DrawLine(x1, y, x2, y, c);
        }
    }
}

    void DrawLine(int x1, int y1, int x2, int y2, int c)
    {
        if((x1 > 0 && x1 < WIDTH && y1 > 0 && y1 < HEIGHT) || (x2 > 0 && 2 < WIDTH && y2 > 0 && y2 < HEIGHT)){
            // Bresenham's line algorithm
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int sx = (x1 < x2) ? 1 : -1;
            int sy = (y1 < y2) ? 1 : -1;
            int err = dx - dy;
            int x = x1;
            int y = y1;

            while (x != x2 || y != y2) {
                if (x > 0 && y > 0 && x < WIDTH && y < HEIGHT) {
                    BUFFER[((x % WIDTH) + (y * WIDTH))] = c;
                }

                int err2 = 2 * err;
                if (err2 > -dy) {
                    err -= dy;
                    x += sx;
                }
                if (err2 < dx) {
                    err += dx;
                    y += sy;
                }
            }
        }
    }

  /*  void Draw(){
        long startTime = System.nanoTime();

        kernel.setTransform(Matrix);
        kernel.setMode(0);
        kernel.execute(Range.create(BACKGROUND.length));

        kernel.setMode(1);
        kernel.execute(Range.create(lines.length / 5));
        kernel.get(kernel.buffer);
        currentBuffer.updateBuffer();
        currentBuffer.setPixels(BUFFER);

        System.out.println("Elapsed Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");
    }*/

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}