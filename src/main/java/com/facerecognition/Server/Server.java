package com.facerecognition.Server;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;

public class Server {
    private int port;
    private KeyPair rsaKeyPair;

    public Server(int port) {
        this.port = port;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            rsaKeyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Không thể khởi tạo RSA KeyPair: " + e.getMessage());
        }
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                System.out.println("Server đang lắng nghe tại port " + port);
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket)).start(); // Đa luồng để xử lý nhiều client
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo server socket: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        System.out.println("Đã chấp nhận kết nối từ client: " + socket.getRemoteSocketAddress());
        try {
            // 1. Gửi public key cho client
            ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
            byte[] publicKeyBytes = rsaKeyPair.getPublic().getEncoded();
            objOut.writeObject(publicKeyBytes);
            objOut.flush();

            // 2. Nhận AES key đã mã hóa từ client
            ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
            byte[] encryptedAesKey = (byte[]) objIn.readObject();

            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");

            // 3. Bắt đầu giao tiếp bằng AES
            handleEncryptedCommunication(socket, aesKey);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEncryptedCommunication(Socket socket, SecretKey aesKey) {
        try (DataInputStream reader = new DataInputStream(socket.getInputStream());
             DataOutputStream writer = new DataOutputStream(socket.getOutputStream())) {

            Cipher aesCipherEnc = Cipher.getInstance("AES");
            Cipher aesCipherDec = Cipher.getInstance("AES");
            aesCipherEnc.init(Cipher.ENCRYPT_MODE, aesKey);
            aesCipherDec.init(Cipher.DECRYPT_MODE, aesKey);

            while (true) {
                int len = reader.readInt();
                byte[] encryptedInput = new byte[len];
                reader.readFully(encryptedInput);

                String decryptedInput = new String(aesCipherDec.doFinal(encryptedInput));
                System.out.println("Server nhận (giải mã): " + decryptedInput);

                if (decryptedInput.equalsIgnoreCase("bye")) {
                    System.out.println("Server nhận yêu cầu đóng kết nối từ client.");
                    break;
                }

                String response = processData(decryptedInput);
                byte[] encryptedResponse = aesCipherEnc.doFinal(response.getBytes());

                writer.writeInt(encryptedResponse.length);
                writer.write(encryptedResponse);
            }
        } catch (Exception e) {
            System.err.println("Lỗi mã hóa/giao tiếp với client: " + e.getMessage());
        }
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

    private String processData(String input) {
        StringBuilder response = new StringBuilder(input);
        return "Server phản hồi: " + response.toString();
    }

    public static void main(String[] args) {
        Server server = new Server(12345);
        server.start();
    }
}
