package com.facerecognition.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceRecognitionServer {

    private static final int PORT = 9999;
    private static final String IMAGE_SAVE_PATH = "src/main/java/com/facerecognition/Server/received_images/";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🔌 Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("📥 Client connected: " + socket.getInetAddress());

                new Thread(() -> handleClient(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
        ) {
            // Step 1: Nhận ảnh từ client
            DataInputStream dis = new DataInputStream(input);
            int imageSize = dis.readInt();
            byte[] imageBytes = new byte[imageSize];
            dis.readFully(imageBytes);

            // Step 2: Lưu ảnh tạm ra file
            String fileName = UUID.randomUUID() + ".jpg";
            Path imagePath = Paths.get(IMAGE_SAVE_PATH + fileName);
            // if (!Files.exists(imagePath)) {
            //     Files.createDirectories(imagePath);
            // }
            Files.write(imagePath, imageBytes);
            System.out.println("🖼 Ảnh đã lưu tại: " + imagePath.toString());

            String urlApi = "http://localhost:5000/api/recognition";
            postImageAPI(urlApi, imagePath.toString());
            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject postImageAPI(String urlApi, String imagePath) {
        OkHttpClient client = new OkHttpClient();

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("✘ File không tồn tại: " + imagePath);
            return null;
        }

        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(urlApi)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println(responseBody);
            // System.out.println("→ Status: " + response.code());
            // System.out.println("→ Response: " + responseBody);

            return new JSONObject(responseBody);
        } catch (IOException e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    // Hàm gọi Python để nhận diện khuôn mặt
    private static String[] runPythonFaceRecognitionScript(String imagePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "face_match.py", imagePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();  // Nhận kết quả từ stdout

            int exitCode = process.waitFor();
            if (exitCode != 0 || line == null) {
                System.err.println("❌ Python process failed.");
                return null;
            }

            // Kết quả trả về dạng: John_Doe|85.6
            String[] parts = line.split("\\|");
            return parts;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

