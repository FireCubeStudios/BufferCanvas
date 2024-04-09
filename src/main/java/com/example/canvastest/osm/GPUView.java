package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.*;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javafx.animation.AnimationTimer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

public class GPUView {
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private NEWERPIXELKERNEL kernel;
    private int[] xPoints;
    private int[] yPoints;
    private int[] cPoints;
    double lastX = 0;
    double a = 4;
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
        scene.setOnMouseReleased(e -> {
            double dx = e.getX() - lastX;
            double dy = e.getY() - lastY;
            Matrix.e += (float) dx;
            Matrix.f += (float) dy;

            points();
            Draw();
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


        scene.setOnScrollFinished(e -> {
            /*   Matrix.a = 1;
              Matrix.d = 1;*/
           /* points();
            Draw();*/
        });
        scene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double scaleFactor = Math.pow(1.1, delta / 100.0); // Adjust this factor as needed
            a *= (float) scaleFactor;
            if (a > 1) {
                points();
            }
            Draw();
            /*if(a * scaleFactor < a)
            {
                Matrix.a *= (float) scaleFactor;
                Matrix.d *= (float) scaleFactor;
                Draw();
            }
            else {
                a *= (float) scaleFactor;
                if (a > 1) {
                    points();
                }
                Matrix.a = 1;
                Matrix.d = 1;
                Draw();
            }*/
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
            BACKGROUND[ii] = toARGB(Color.WHITESMOKE);
            BUFFER[ii] = toARGB(Color.WHITESMOKE);
        });
        kernel.resize(BUFFER, BACKGROUND, WIDTH, HEIGHT);
    }

    void Setup(){

       // System.out.print(mapData.ways.stream().count()); // 78710 ways
        kernel = new NEWERPIXELKERNEL(BUFFER, BACKGROUND, WIDTH, HEIGHT);

        points();
        Resize();

        Draw();
    }

    private void points()
    {
        long startTime = System.nanoTime();
        xPoints = new int[10446880];
        yPoints = new int[10446880];
        cPoints = new int[10446880];

        double scale = (HEIGHT / (mapData.maxlat - mapData.minlat)) * a;
        double ogx = (mapData.ways.getFirst().coords[0] * scale);
        double ogy = (mapData.ways.getFirst().coords[1] * scale);
        // 22ms
       // int currentIndex = 0;
        final AtomicInteger[] currentIndex = {new AtomicInteger(0)};
        IntStream.range(0, mapData.ways.size()).parallel().forEach(ex -> {
            var way = mapData.ways.get(ex);
            for (int i = 0; i < way.coords.length - 2; i += 2) {
                int x = (int)(((way.coords[i] * scale)) - ogx);
                int y = (int)(((way.coords[i + 1] * scale)) - ogy);
                int x2 = (int)(((way.coords[i + 2] * scale)) - ogx);
                int y2 = (int)(((way.coords[i + 3] * scale)) - ogy);

                if(currentIndex[0].get() > xPoints.length - 100) break;
                if(((((x * Matrix.a) + Matrix.e) > -WIDTH) && (((x * Matrix.a) + Matrix.e) < (WIDTH * 2)))
                        || ((((y * Matrix.d) + Matrix.f) > -HEIGHT) && (((y * Matrix.d) + Matrix.f) < (HEIGHT * 2)))
                        || ((((x2 * Matrix.a) + Matrix.e) > -WIDTH) && (((x2 * Matrix.a) + Matrix.e) < (WIDTH * 2)))
                        || ((((y2 * Matrix.d) + Matrix.f) > -HEIGHT) && (((y2 * Matrix.d) + Matrix.f) < (HEIGHT * 2))))
                {
                    var pointsX = LineHelper.line(x, y, x2, y2);
                    for (int ii = 0; ii < pointsX.length - 1; ii += 2) {
                        int iii = currentIndex[0].getAndAdd(2);
                        if(currentIndex[0].get() > xPoints.length - 1) break;

                        xPoints[iii] = pointsX[ii];
                        yPoints[iii] = pointsX[ii + 1];
                        cPoints[iii] = (ii > 2 && ii < pointsX.length - 4) ? toARGB(Color.RED) : toARGB(Color.BLACK);
                    }
                    // currentIndex = LineHelper.line(x, y, x2, y2, currentIndex, xPoints, yPoints, cPoints, toARGB(Color.BLACK));
                }
            }
        });
       /* for (var way : mapData.ways) {
            for (int i = 0; i < way.coords.length - 2; i += 2) {
                int x = (int)(((way.coords[i] * scale)) - ogx);
                int y = (int)(((way.coords[i + 1] * scale)) - ogy);
                int x2 = (int)(((way.coords[i + 2] * scale)) - ogx);
                int y2 = (int)(((way.coords[i + 3] * scale)) - ogy);

                if(currentIndex > xPoints.length - 100) break;
                if(((((x * Matrix.a) + Matrix.e) > -WIDTH) && (((x * Matrix.a) + Matrix.e) < (WIDTH * 2)))
                        || ((((y * Matrix.d) + Matrix.f) > -HEIGHT) && (((y * Matrix.d) + Matrix.f) < (HEIGHT * 2)))
                        || ((((x2 * Matrix.a) + Matrix.e) > -WIDTH) && (((x2 * Matrix.a) + Matrix.e) < (WIDTH * 2)))
                        || ((((y2 * Matrix.d) + Matrix.f) > -HEIGHT) && (((y2 * Matrix.d) + Matrix.f) < (HEIGHT * 2))))
               {
                   var pointsX = LineHelper.line(x, y, x2, y2);
                   for (int ii = 0; ii < pointsX.length - 1; ii += 2) {
                       int iii = currentIndex += 2;
                       if(currentIndex > xPoints.length - 1) break;

                       xPoints[iii] = pointsX[ii];
                       yPoints[iii] = pointsX[ii + 1];
                       cPoints[iii] = (ii > 2 && ii < pointsX.length - 4) ? toARGB(Color.RED) : toARGB(Color.BLACK);
                   }
                  // currentIndex = LineHelper.line(x, y, x2, y2, currentIndex, xPoints, yPoints, cPoints, toARGB(Color.BLACK));
               }
            }
        }*/

        kernel.setPoints(xPoints, yPoints, cPoints); // 0ms
        System.out.println("Points Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");
        System.out.println(currentIndex[0]);
    }

    void Draw(){
        long startTime = System.nanoTime();

        kernel.setTransform(Matrix);
        kernel.setMode(0);
        kernel.execute(Range.create(BACKGROUND.length));

        kernel.setMode(1);
        kernel.execute(Range.create(xPoints.length));
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

                     /*  var pointsX = LineHelper.line(x, y, x2, y2);
                   for (int ii = 0; ii < pointsX.length - 1; ii += 2) {
                       int iii = currentIndex += 2;
                       if(currentIndex > xPoints.length - 1) break;

                       xPoints[iii] = pointsX[ii];
                       yPoints[iii] = pointsX[ii + 1];
                       cPoints[iii] = (ii > 2 && ii < pointsX.length - 4) ? toARGB(Color.RED) : toARGB(Color.BLACK);
                   }*/


       /*     Set<String> valid = new HashSet<>(
                Arrays.asList("highway", "waterway", "natural", "parking", "leisure", "building", "landuse"
                ,"farmland","service", "tertiary", "primary", "residential", "footway", "path", "stream", "tree_row"
                ,"surface", "park", "water", "scrub", "industrial", "yes", "recreation_ground", "industrial"
                ,"grass", "forest", "farmyard", "meadow"));
                   /*   boolean validx = false;
           for(var tag : way.tags)
           {
               if(valid.contains(tag.Type)) validx = true;
           }
           if(!validx) break;
*/
}
