package com.facerecognition.Client;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
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
    private Webcam webcam;
    private Button btnSnap;

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
        btnSnap = new Button("📸 Chụp ảnh");
        btnSnap.setVisible(false);
        btnSnap.setManaged(false);

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
        btnCaptureWebcam.setOnAction(e -> handleCaptureFromWebcam());
        btnSendToServer.setOnAction(e -> handleSendToServer());
        btnAddFace.setOnAction(e -> handleAddToDatabase());
        btnSnap.setOnAction(e -> captureWebcam());
        btnToggleForm.setOnAction(e -> {statusProperty.set(!statusProperty.get());});

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
        btnSnap.setVisible(false);
        btnSnap.setManaged(false);
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

    private void captureWebcam() {
        if (webcam != null && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            if (image != null) {
                try {
                    String filename = "img_" + System.currentTimeMillis() + ".jpg";
                    File outputFile = new File("src/main/java/com/facerecognition/Client/captured_images/" + filename);
                    outputFile.getParentFile().mkdirs();

                    ImageIO.write(image, "JPG", outputFile);
                    selectedImageFile = outputFile;

                    Image fxImage = SwingFXUtils.toFXImage(image, null);
                    imageView.setImage(fxImage);

                    resultArea.setText("✅ Đã chụp ảnh: " + filename);
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                    resultArea.setText("❌ Lỗi khi lưu ảnh.");
                }
            } else {
                resultArea.setText("❌ Không lấy được frame từ webcam.");
            }
        } else {
            resultArea.setText("❌ Webcam chưa sẵn sàng.");
        }
    }

    private void handleCaptureFromWebcam() {
        webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();
        
        btnSnap.setVisible(true);
        btnSnap.setManaged(true);

        // Timeline timeline = new Timeline(new KeyFrame(Duration.millis(33), e -> {
        //     BufferedImage image = webcam.getImage();
        //     if (image != null) {
        //         Image fxImage = SwingFXUtils.toFXImage(image, null);
        //         Platform.runLater(() -> imageView.setImage(fxImage));
        //     }
        // }));
        // timeline.setCycleCount(Timeline.INDEFINITE);
        // timeline.play();
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

