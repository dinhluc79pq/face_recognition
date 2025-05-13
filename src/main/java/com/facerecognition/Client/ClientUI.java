package com.facerecognition.Client;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

import com.facerecognition.Server.User;
import com.facerecognition.Server.UserController;
import com.github.sarxos.webcam.Webcam;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ClientUI extends Application {

    private ImageView imageView;
    private TextArea resultArea;
    private File selectedImageFile;
    private WebcamCaptureHelper webcamHelper;

    static JSONObject dataJson;

    @Override
    public void start(Stage primaryStage) {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        primaryStage.setTitle("Face Recognition Client");

        // Image display
        imageView = new ImageView();
        imageView.setFitHeight(300);
        imageView.setFitWidth(300);
        imageView.setPreserveRatio(true);

        // Buttons
        Button btnSelectImage = new Button("📁 Chọn ảnh từ máy");
        Button btnCaptureWebcam = new Button("📷 Chụp từ webcam");
        Button btnSendToServer = new Button("📤 Gửi ảnh đến server");
        Button btnAddFace = new Button("➕ Thêm vào CSDL");

        // Result area
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(100);

        // Layout
        HBox buttonBox = new HBox(10, btnSelectImage, btnCaptureWebcam, btnSendToServer, btnAddFace);
        VBox root = new VBox(15, imageView, buttonBox, new Label("Kết quả:"), resultArea);
        root.setPadding(new Insets(20));

        webcamHelper = new WebcamCaptureHelper(imageView);

        // Set actions
        btnSelectImage.setOnAction(e -> handleSelectImage());
        btnCaptureWebcam.setOnAction(e -> handleCaptureFromWebcam()); // bạn cần tích hợp OpenCV phần này
        btnSendToServer.setOnAction(e -> handleSendToServer());
        btnAddFace.setOnAction(e -> handleAddToDatabase());
        // btnCaptureWebcam.setOnAction(e -> {
        //     if (webcamHelper != null) webcamHelper.startCamera();
        // });
        // // Nút "📸 Chụp ảnh"
        // Button btnSnap = new Button("📸 Chụp ảnh");
        // btnSnap.setOnAction(e -> {
        //     File captured = webcamHelper.captureImage();
        //     if (captured != null) {
        //         selectedImageFile = captured; // gán ảnh chụp làm ảnh gửi
        //         resultArea.setText("✅ Đã chụp ảnh: " + captured.getName());
        //     } else {
        //         resultArea.setText("❌ Chưa chụp được ảnh.");
        //     }
        // });

        // Show stage
        Scene scene = new Scene(root, 700, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static JSONObject postImageAPI(String urlApi, String imagePath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("✘ File không tồn tại: " + imagePath);
            return null;
        }

        byte[] imageBytes = null;
        try {
            imageBytes = Files.readAllBytes(Paths.get(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Encode base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Tạo JSON
        JSONObject json = new JSONObject();
        json.put("filename", "unknown.jpg");
        json.put("image_data", base64Image);

        HttpClient clientResponse = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:5000/api/recognition"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = null;
        try {
            response = clientResponse.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String responseBody = response.body();
        System.out.println(responseBody);
        // System.out.println("→ Status: " + response.code());
        // System.out.println("→ Response: " + responseBody);

        return new JSONObject(responseBody);
    }

    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh khuôn mặt");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.png", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            imageView.setImage(image);
            resultArea.setText("Đã chọn ảnh: " + file.getName());
            
            String urlApi = "http://localhost:5000/api/recognition";
            dataJson = postImageAPI(urlApi, file.getPath());
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
    }

    private void handleAddToDatabase() {
        JSONArray jsonArray = dataJson.getJSONArray("face_encoding");
        List<Double> face_encoding = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            face_encoding.add(jsonArray.getDouble(i));
        }
        User u = new User("12345", "Dương Đình Lực", null, null, face_encoding);
        UserController uc = new UserController();
        uc.insertUser(u);
        resultArea.setText("Đang gửi ảnh để thêm vào CSDL...");
    }

    private void handleSendToServer() {
        if (selectedImageFile == null) {
            resultArea.setText("❗ Vui lòng chọn hoặc chụp ảnh trước.");
            return;
        }

        try (Socket socket = new Socket("localhost", 9999)) {
            resultArea.setText("🔄 Đang gửi ảnh đến server...");

            // 1. Đọc file ảnh vào byte[]
            byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());

            // 2. Gửi size và dữ liệu ảnh
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(imageBytes.length); // gửi trước kích thước
            dos.write(imageBytes);           // gửi dữ liệu ảnh
            dos.flush();

            // 3. Nhận phản hồi từ server
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder responseJson = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseJson.append(line);
            }

            // 4. Parse JSON (dùng thư viện hoặc thủ công)
            parseAndDisplayResult(responseJson.toString());

        } catch (IOException e) {
            e.printStackTrace();
            resultArea.setText("❌ Lỗi khi gửi ảnh đến server.");
        }
    }

    private void parseAndDisplayResult(String json) {
        try {
            // Bản đơn giản, bạn có thể thay bằng thư viện JSON nếu cần
            String name = json.split("\"name\":")[1].split(",")[0].replace("\"", "").trim();
            String similarity = json.split("\"similarity\":")[1].split(",")[0].trim();
            String status = json.split("\"status\":")[1].replace("\"", "").replace("}", "").trim();
    
            String result = "👤 Đối tượng: " + name +
                            "\n🎯 Độ khớp: " + similarity + "%" +
                            "\n📌 Trạng thái: " + (status.equals("matched") ? "Khớp" : "Không khớp");
    
            resultArea.setText(result);
        } catch (Exception e) {
            resultArea.setText("❌ Lỗi khi phân tích kết quả từ server.");
        }
    }    


    public static void main(String[] args) {
        launch(args);
    }
}

