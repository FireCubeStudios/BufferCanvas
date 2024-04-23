package com.example.canvastest.osm;

import com.aparapi.Range;
import com.example.canvastest.AWindingPolygonKernel;
import com.example.canvastest.FIXED2POLYGONKERNEL;
import com.example.canvastest.Transform;
import com.example.canvastest.WritableImageView;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.stream.IntStream;

public class WindingPolygonGPUView {
    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int[] BACKGROUND = new int[WIDTH * HEIGHT];
    private int[] BUFFER = new int[WIDTH * HEIGHT];
    private WritableImageView currentBuffer = new WritableImageView(WIDTH, HEIGHT);
    private AWindingPolygonKernel kernel;
    private int[] lines;

    double lastX = 0;
    double lastY = 0;
    private Transform Matrix = new Transform();
    private OSMData mapData;
    private Stage primaryStage;
    private Scene scene;
    public WindingPolygonGPUView(String filename, Stage primaryStage) {
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

        kernel = new AWindingPolygonKernel(BUFFER, BACKGROUND, WIDTH, HEIGHT);
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
        int[] vertices3 = {100, 600, 200, 300, 200, 800, 100, 600};
        int[] vertices2 = {720, 720, 480, 720, 480, 480, 720, 480, 720, 720};
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

        for (int i = 0; i < vertices2.length - 2; i += 2) {
            if(currentIndex >= lines.length) break;

            int x = vertices2[i];
            int y = vertices2[i + 1];
            int x2 = vertices2[i + 2];
            int y2 = vertices2[i + 3];

            currentIndex += 6;
            int iii = currentIndex;
            lines[iii] = x;
            lines[iii + 1] = y;
            lines[iii + 2] = x2;
            lines[iii + 3] = y2;
            lines[iii + 4] = c2;
            lines[iii + 5] = 1; // is polygon
        }

        for (int i = 0; i < vertices3.length - 2; i += 2) {
            if(currentIndex >= lines.length) break;

            int x = vertices3[i];
            int y = vertices3[i + 1];
            int x2 = vertices3[i + 2];
            int y2 = vertices3[i + 3];

            currentIndex += 6;
            int iii = currentIndex;
            lines[iii] = x;
            lines[iii + 1] = y;
            lines[iii + 2] = x2;
            lines[iii + 3] = y2;
            lines[iii + 4] = c3;
            lines[iii + 5] = 1; // is polygon
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
        kernel.execute(Range.create(HEIGHT));

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
