package com.facerecognition;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class CameraTest extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private VideoCapture capture;
    private Timeline timeline;
    private ImageView imageView = new ImageView();
    private Mat frame = new Mat();

    @Override
    public void start(Stage primaryStage) {
        // UI
        Button btnStop = new Button("⛔ Dừng camera");
        VBox root = new VBox(10, imageView, btnStop);
        Scene scene = new Scene(root, 640, 520);

        // ImageView setup
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);

        // Open camera (index 0)
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.err.println("❌ Không thể mở webcam!");
            return;
        }

        // Timeline: lặp lại mỗi 33ms (~30fps)
        timeline = new Timeline(new KeyFrame(Duration.millis(33), e -> updateFrame()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Stop button
        btnStop.setOnAction(e -> stopCamera());

        // Show stage
        primaryStage.setTitle("🧪 Test Camera JavaFX + OpenCV");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void updateFrame() {
        if (capture.read(frame)) {
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
            Image fxImage = matToImage(frame);
            Platform.runLater(() -> imageView.setImage(fxImage));
        }
    }

    private Image matToImage(Mat mat) {
        int width = mat.width(), height = mat.height(), channels = mat.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return SwingFXUtils.toFXImage(image, null);
    }

    private void stopCamera() {
        if (timeline != null) {
            timeline.stop();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        System.out.println("✅ Đã dừng webcam.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}


