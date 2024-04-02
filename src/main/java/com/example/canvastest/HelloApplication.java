package com.example.canvastest;

import com.example.canvastest.osm.Controller;
import com.example.canvastest.osm.GPUView;
import com.example.canvastest.osm.Model;
import com.example.canvastest.osm.OldView;
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
        var filename = "D:\\Projects\\CanvasTest\\map.osm";
        var view = new GPUView(filename, stage);
    }

    public static void main(String[] args) {
        launch();
    }
}