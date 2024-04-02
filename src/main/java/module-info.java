module com.example.canvastest {
    requires javafx.controls;
    requires javafx.fxml;
    requires aparapi;
    requires java.xml;
    requires parallelpbf;


    opens com.example.canvastest to javafx.fxml;
    exports com.example.canvastest;
}