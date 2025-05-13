package com.facerecognition;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import java.awt.image.BufferedImage;

public class App extends Application {

    private VideoCapture capture;
    private volatile boolean stopCamera = false;
    private ImageView imageView;

    @Override
    public void start(Stage stage) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load thư viện OpenCV

        imageView = new ImageView();
        Button startButton = new Button("Mở webcam");
        Button stopButton = new Button("Tắt");

        startButton.setOnAction(e -> startCamera());
        stopButton.setOnAction(e -> stopCamera());

        VBox root = new VBox(10, imageView, startButton, stopButton);
        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Webcam với JavaFX & OpenCV (OpenPnP)");
        stage.show();
    }

    private void startCamera() {
        capture = new VideoCapture(0); // Webcam mặc định
        stopCamera = false;

        Thread cameraThread = new Thread(() -> {
            Mat frame = new Mat();
            while (!stopCamera && capture.isOpened()) {
                capture.read(frame);
                if (!frame.empty()) {
                    Image fxImage = mat2Image(frame);
                    Platform.runLater(() -> imageView.setImage(fxImage));
                }
                try {
                    Thread.sleep(33); // ~30fps
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            frame.release();
        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void stopCamera() {
        stopCamera = true;
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }

    private Image mat2Image(Mat frame) {
        int width = frame.width(), height = frame.height(), channels = frame.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        frame.get(0, 0, sourcePixels);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        int widthImg = image.getWidth();
        int heightImg = image.getHeight();

        WritableImage wImage = new WritableImage(width, height);
        PixelWriter pixelWriter = wImage.getPixelWriter();

        int[] pixels = new int[widthImg * heightImg];
        image.getRGB(0, 0, widthImg, heightImg, pixels, 0, widthImg);

        pixelWriter.setPixels(0, 0, widthImg, heightImg, PixelFormat.getIntArgbInstance(), pixels, 0, widthImg);
        return wImage;
    }

    @Override
    public void stop() {
        stopCamera();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
