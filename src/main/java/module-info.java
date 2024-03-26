module com.example.canvastest {
    requires javafx.controls;
    requires javafx.fxml;
    requires aparapi;


    opens com.example.canvastest to javafx.fxml;
    exports com.example.canvastest;
}