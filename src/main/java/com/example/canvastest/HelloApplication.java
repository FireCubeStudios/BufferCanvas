package com.example.canvastest;

import com.example.canvastest.osm.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public class HelloApplication extends Application {
    GPUBufferUICanvasView view;
    @Override
    public void start(Stage stage) throws IOException, XMLStreamException {
      //  view = new GPUBufferUICanvasView(stage);
      /*  FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();*/

      /*  var filename = "C:\\Users\\Dulu\\IdeaProjects\\BufferCanvas\\østerlars.osm.zip";
        var model = new Model(filename);
        var view = new OldView(model, stage);
        var controller = new Controller(model, view);*/
      //  var filename = "C:\\Users\\Dulu\\IdeaProjects\\BufferCanvas\\østerlars.osm.zip";

    /* var filename = "D:\\Projects\\CanvasTest\\denmark-latest.osm.pbf";
        var view = new PBFGPUView(filename, stage);*/
        /*var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new  LineGPUView(filename, stage);*/
       /*var filename = "D:\\Projects\\CanvasTest\\denmark-latest.osm.pbf";
        var view = new  PBFLineGPUView(filename, stage);*/
      /*  var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new  PolygonGPUView(filename, stage);*/
      /*  var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new  PolygonLineGPUView(filename, stage);*/

      /*  var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new  WindingPolygonGPUView(filename, stage);*/
      /*  var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new CPUBufferCanvas(filename, stage);
*/
        var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new ZIndexCPUBufferCanvas(filename, stage);
    }

    public static void main(String[] args) {
        launch();
    }
}