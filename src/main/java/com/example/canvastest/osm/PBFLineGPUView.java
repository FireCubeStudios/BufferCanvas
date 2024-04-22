package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.SimpleLineKernel;
import com.example.canvastest.Transform;
import com.example.canvastest.WritableImageView;
import com.wolt.osm.parallelpbf.ParallelBinaryParser;
import com.wolt.osm.parallelpbf.entity.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.xml.xpath.XPathNodes;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

public class PBFLineGPUView {
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private SimpleLineKernel kernel;
    private int[] lines = new int[48194627 * 5];

    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private Stage primaryStage;
    private Scene scene;

    private final double scale = 82.62484915943543 * 100;
    private final double ogx = 55.713486499999945 * scale;
    private final double ogy = 11.723541300000003 * scale;
    private int toPoint(double coordinate, boolean isY)
    {
        return (int)(((coordinate * scale) - (isY ? ogy : ogx)) * (isY ? 1 : -1));
    }
    private final Object lock = new Object();
    private int nodesIndex = 0;
    private int linesIndex = 0;
    private int coordsIndex = 0;
    private double[] coords = new double[58190 * 2];
    private long[] nodes = new long[58190 * 2];

    private void processNodes(Node node) {
        synchronized (lock) {
            if(coordsIndex < 48190)
            {
                nodes[nodesIndex] = node.getId();
                nodes[nodesIndex + 1] = coordsIndex;
                nodesIndex += 2;
                coords[coordsIndex] = node.getLon();
                coords[coordsIndex + 1] = node.getLat();
                coordsIndex += 2;
            }
        }
       /* synchronized (lock) {
            lines[nodesIndex] = toPoint(node.getLon(), true);
             lines[nodesIndex + 1] = toPoint(node.getLat(), false);
            lines[nodesIndex + 2] = toPoint(node.getLon(), true) + 30;
            lines[nodesIndex + 3] = toPoint(node.getLat(), false) + 30;
            lines[nodesIndex + 4] = toARGB(Color.BLACK);
             nodesIndex += 5;
        }*/
    }

    private void processWays(com.wolt.osm.parallelpbf.entity.Way way) {
       synchronized (lock) {
           if(linesIndex < 48190) {
               var node = way.getNodes();
               for (int i = 0; i < node.size(); i += 2) {
                   double lon = 0;
                   double lat = 0;
                   double lon1 = 0;
                   double lat1 = 0;
                   boolean found = false;
                   boolean found1 = false;
                   if (linesIndex < 48190) {
                       for (int ii = 0; ii < nodes.length; ii++) {
                           if (nodes[ii] == node.get(i)) {
                               found = true;
                               lon = coords[(int)nodes[ii + 1]];
                               lat = coords[(int)nodes[ii + 1] + 1];
                               break;
                           }
                       }
                       if(found) {
                           for (int ii = 0; ii < nodes.length; ii++) {
                               if (nodes[ii] == node.get(i + 1)) {
                                   found1 = true;
                                   lon1 = coords[(int) nodes[ii + 1]];
                                   lat1 = coords[(int) nodes[ii + 1] + 1];
                                   break;
                               }
                           }
                       }
                       if (linesIndex < 48190 && found && found1) {
                           lines[linesIndex] = toPoint(lon, true);
                           lines[linesIndex + 1] = toPoint(lat, false);
                           lines[linesIndex + 2] = toPoint(lon1, true) + 30;
                           lines[linesIndex + 3] = toPoint(lat1, false) + 30;
                           lines[linesIndex + 4] = toARGB(Color.BLACK);
                           linesIndex += 5;
                       }
                   }
               }
           }
        }
    }
    private FileInputStream stream;
    public PBFLineGPUView(String filename, Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            stream = new FileInputStream(filename);
            ParallelBinaryParser b = new ParallelBinaryParser(stream, 100)
                    .onComplete(this::completenodes)
                    .onNode(this::processNodes);
                    b.parse();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void completenodes()
    {
        try {
            new ParallelBinaryParser(stream, 100)
                    .onComplete(this::complete)
                    .onWay(this::processWays)
                    .parse();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void complete()
    {
        try {
                stream.close();
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

    void Setup() {
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
        kernel = new SimpleLineKernel(BUFFER, BACKGROUND, WIDTH, HEIGHT);
        points();

        Draw();
    }

    private void points() {
        long startTime = System.nanoTime();

        kernel.setLines(lines);
        System.out.println("Points Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");

        System.out.println(lines.length);
    }

    void Draw() {
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
    }

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) << 8
                | (int) (color.getBlue() * 255);
    }
}