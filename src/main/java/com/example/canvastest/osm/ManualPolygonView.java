package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.FIXED2POLYGONKERNEL;
import com.example.canvastest.Transform;
import com.example.canvastest.WritableImageView;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.stream.IntStream;

public class ManualPolygonView {
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
    public ManualPolygonView(String filename, Stage primaryStage) {
        this.primaryStage = primaryStage;
      /*  try {
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
*/
        primaryStage.setTitle("GPU Buffer");
        BorderPane pane = new BorderPane(currentBuffer);
        scene = new Scene(pane);
        scene.setRoot(pane);
        primaryStage.setScene(scene);
        primaryStage.show();

        Setup();

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            Resize();
            Draw();
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            Resize();
            Draw();
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

        int c = toARGB(Color.RED);
        int c2 = toARGB(Color.BLUE);
        int c3 = toARGB(Color.GREEN);
        int currentIndex = 0;
        // int[] vertices = {100, 100, 500, 100, 500, 500, 100, 500, 100, 100};
        int[] vertices = {100, 100, 500, 200, 10, 200, 100, 100};
        for (int i = 0; i < vertices.length - 2; i += 2) {
            if(currentIndex >= lines.length) break;

            int x = vertices[i];
            int y = vertices[i + 1];
            int x2 = vertices[i + 2];
            int y2 = vertices[i + 3];

            currentIndex += 6;
            int iii = currentIndex;
            lines[iii] = x;
            lines[iii + 1] = y;
            lines[iii + 2] = x2;
            lines[iii + 3] = y2;
            lines[iii + 4] = c;
            lines[iii + 5] = 1; // is polygon
        }

        /*
        IDEA: For every polygon store its vertices in kernel.VERTICES together alongside a minY, maxY
        In a mode check if vertices are in the view and if so do something ig
        generate scanlines using minY, maxY


        // Initialize an array to store the intersection points for each scanline LINE
        intersection_points = array[minY...maxY]

        // Iterate over each edge of the polygon (X1, Y1, X2, Y2)
        for each edge in polygon:
            // Determine the Y coordinates of the endpoints of the edge
            startY = min(edge.y1, edge.y2)
            endY = max(edge.y1, edge.y2)

            // Calculate the slope of the edge
            slope = (edge.y2 - edge.y1) / (edge.x2 - edge.x1)

            // Iterate over each scanline the edge intersects
            for y = startY to endY:
                // Calculate the intersection point's X coordinate
                x = edge.x1 + (y - edge.y1) / slope

                // Add the intersection point to the array for this scanline
                intersection_points[y].add(x)


        ANOTHER IDEA: Seperate



         */
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
