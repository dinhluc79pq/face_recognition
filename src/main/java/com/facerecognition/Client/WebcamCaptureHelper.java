package com.facerecognition.Client;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.UUID;

public class WebcamCaptureHelper {

    private VideoCapture capture;
    private boolean cameraActive = false;
    private Timeline timer;
    private Mat frame = new Mat();
    private ImageView imageView;
    private String lastCapturedPath;

    public WebcamCaptureHelper(ImageView imageView) {
        this.imageView = imageView;
        this.capture = new VideoCapture();
    }

    public void startCamera() {
        if (!cameraActive) {
            capture.open(0);

            if (capture.isOpened()) {
                cameraActive = true;

                timer = new Timeline(
                        new KeyFrame(Duration.millis(33), event -> updateFrame())
                );
                timer.setCycleCount(Timeline.INDEFINITE);
                timer.play();
            } else {
                System.err.println("❌ Không thể mở webcam.");
            }
        }
    }

    public void stopCamera() {
        cameraActive = false;
        if (timer != null) timer.stop();
        if (capture.isOpened()) capture.release();
    }

    private void updateFrame() {
        if (capture.read(frame)) {
            Image imageToShow = matToImage(frame);
            Platform.runLater(() -> imageView.setImage(imageToShow));
        } else {
            System.err.println("⚠️ Không đọc được frame từ camera.");
        }
    }
    

    public File captureImage() {
        if (!frame.empty()) {
            String filename = "captured_" + UUID.randomUUID() + ".jpg";
            File outFile = new File("captured_images/" + filename);

            // Tạo thư mục nếu chưa có
            outFile.getParentFile().mkdirs();

            Imgcodecs.imwrite(outFile.getAbsolutePath(), frame);
            lastCapturedPath = outFile.getAbsolutePath();
            return outFile;
        }
        return null;
    }

    private Image matToImage(Mat originalMat) {
        // 1. Chuyển BGR → RGB (OpenCV lưu ảnh theo BGR, JavaFX hiển thị RGB)
        Mat mat = new Mat();
        Imgproc.cvtColor(originalMat, mat, Imgproc.COLOR_BGR2RGB);
    
        // 2. Chuyển Mat → byte[]
        int width = mat.width(), height = mat.height(), channels = mat.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);
    
        // 3. Tạo BufferedImage từ byte[]
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
    
        // 4. Chuyển BufferedImage → JavaFX Image
        return SwingFXUtils.toFXImage(image, null);
    }
    
    public String getLastCapturedPath() {
        return lastCapturedPath;
    }
}
