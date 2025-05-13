module com.facerecognition {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires java.sql;
    requires opencv;
    requires org.json;
    requires java.net.http;
    requires okhttp3;
    requires webcam.capture;

    opens com.facerecognition to javafx.fxml;
    exports com.facerecognition;
    exports com.facerecognition.Client;
    exports com.facerecognition.Server;
}
