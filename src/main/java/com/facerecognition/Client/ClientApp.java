package com.facerecognition.Client;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import javax.imageio.ImageIO;

import com.facerecognition.User;
import com.github.sarxos.webcam.Webcam;

public class ClientApp extends Application {

    private ImageView imageView;
    private TextArea resultArea;
    private File selectedImageFile;
    private TextField nameField;
    private DatePicker dobPicker;
    private TextField avatarPathField;
    private WebcamCaptureHelper webcamHelper;

    private Client client;
    public static BooleanProperty statusProperty = new SimpleBooleanProperty(false);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Face Recognition Client");

        // Image display
        imageView = new ImageView();
        imageView.setFitHeight(250);
        imageView.setFitWidth(300);
        imageView.setPreserveRatio(true);

        VBox imageBoxView = new VBox(imageView);
        imageBoxView.setAlignment(Pos.CENTER); // căn giữa theo trục ngang
        imageBoxView.setPrefWidth(320);

        // Buttons
        Button btnSelectImage = new Button("📁 Chọn ảnh từ máy");
        Button btnCaptureWebcam = new Button("📷 Chụp từ webcam");
        Button btnSendToServer = new Button("📤 Gửi ảnh đến server");
        Button btnAddFace = new Button("➕ Thêm vào CSDL");
        Button btnToggleForm = new Button("- Hiện/Ẩn Form -");
        btnCaptureWebcam.setOnAction(e -> {
            if (webcamHelper != null) webcamHelper.startCamera();
        });
        // Nút "📸 Chụp ảnh"
        Button btnSnap = new Button("📸 Chụp ảnh");
        btnSnap.setOnAction(e -> {
            File captured = webcamHelper.captureImage();
            if (captured != null) {
                selectedImageFile = captured; // gán ảnh chụp làm ảnh gửi
                resultArea.setText("✅ Đã chụp ảnh: " + captured.getName());
            } else {
                resultArea.setText("❌ Chưa chụp được ảnh.");
            }
        });

        // Result area
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(200);

        // === Form bên phải ===
        nameField = new TextField();
        dobPicker = new DatePicker();
        avatarPathField = new TextField();

        // Bọc imageView để nó không bị đẩy
        VBox imageBox = new VBox(imageBoxView);
        imageBox.setPadding(new Insets(10));
        imageBox.setPrefWidth(320); // fix chiều rộng khung ảnh

        // Form bên phải (căn lề phải)
        VBox formBox = new VBox(10,
                new Label("Tên người dùng:"), nameField = new TextField(),
                new Label("Ngày sinh:"), dobPicker = new DatePicker(),
                new Label("Đường dẫn ảnh avatar:"), avatarPathField = new TextField(),
                btnAddFace
        );
        formBox.setPadding(new Insets(10));
        formBox.setMaxWidth(300);
        formBox.setStyle("-fx-alignment: top-right;");

        // HBox chính chứa ảnh và form
        HBox imageAndFormBox = new HBox(20, imageBox, formBox);
        HBox.setHgrow(formBox, javafx.scene.layout.Priority.ALWAYS);
        imageAndFormBox.setPrefWidth(700);

        HBox buttonBox = new HBox(10, btnSelectImage, btnCaptureWebcam, btnSnap, btnSendToServer, btnToggleForm);
        VBox root = new VBox(15, imageAndFormBox, buttonBox, new Label("Kết quả:"), resultArea);
        root.setPadding(new Insets(20));

        // Actions
        btnSelectImage.setOnAction(e -> handleSelectImage());
        // btnCaptureWebcam.setOnAction(e -> handleCaptureFromWebcam());
        btnSendToServer.setOnAction(e -> handleSendToServer());
        btnAddFace.setOnAction(e -> handleAddToDatabase());
        btnToggleForm.setOnAction(e -> {
            statusProperty.set(!statusProperty.get());
        });

        formBox.visibleProperty().bind(statusProperty);
        formBox.managedProperty().bind(statusProperty);

        client = new Client("localhost", 12345);
        if (!client.connect()) {
            resultArea.setText("❌ Không thể kết nối đến server.");
        }

        primaryStage.setScene(new Scene(root, 560, 550));
        primaryStage.show();
    }

    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hình ảnh", "*.jpg", "*.png", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedImageFile = file;
            imageView.setImage(new Image(file.toURI().toString()));
            resultArea.setText("Đã chọn ảnh: " + file.getName());
        }
    }

    private void handleCaptureFromWebcam() {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        System.out.println("hello");
        try {
            ImageIO.write(webcam.getImage(), "PNG", new File("hello-world.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // resultArea.setText("⚠️ Chức năng chụp ảnh chưa được tích hợp.");
        // Bạn có thể dùng OpenCV + JavaFX (OpenCVFrameGrabber hoặc VideoCapture) để tích hợp webcam
    }

    private void handleSendToServer() {
        if (selectedImageFile == null) {
            resultArea.setText("⚠️ Vui lòng chọn ảnh trước.");
            return;
        }

        String response = client.sendImage(selectedImageFile);
        resultArea.setText("📥 Phản hồi từ server:\n" + response);
    }

    private void handleAddToDatabase() {
        if (selectedImageFile == null) {
            resultArea.setText("⚠️ Vui lòng chọn ảnh trước.");
            return;
        }
        if (nameField.getText().isEmpty() || dobPicker.getValue() == null || avatarPathField.getText().isEmpty()) {
            resultArea.setText("⚠️ Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            String uid = "" + System.currentTimeMillis(); // sinh mã ngẫu nhiên
            String name = nameField.getText();
            LocalDate dob = dobPicker.getValue();
            String avatarPath = avatarPathField.getText();

            User user = new User(uid, name, dob, avatarPath, null);
            String response = client.sendUser(user);

            resultArea.setText("📤 Đã gửi thông tin người dùng.\n📥 Phản hồi từ server:\n" + response);

        } catch (Exception e) {
            e.printStackTrace();
            resultArea.setText("❌ Lỗi đọc file ảnh: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

