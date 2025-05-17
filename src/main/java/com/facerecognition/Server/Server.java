package com.facerecognition.Server;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONObject;

import com.facerecognition.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Server {
    private int port;
    private KeyPair rsaKeyPair;
    static JSONObject dataUserJson;

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

            System.out.println("Đã kết nối, sẵn sàng!");

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

            Cipher aesCipherEnc = Cipher.getInstance("AES/ECB/PKCS5Padding");
            Cipher aesCipherDec = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipherEnc.init(Cipher.ENCRYPT_MODE, aesKey);
            aesCipherDec.init(Cipher.DECRYPT_MODE, aesKey);

            while (true) {
                String IMAGE_SAVE_PATH = "src/main/java/com/facerecognition/Server/received_images";
                int len = reader.readInt(); // Nếu client đóng -> lỗi tại đây
                byte[] encryptedInput = new byte[len];
                reader.readFully(encryptedInput);

                byte[] decryptedBytes = aesCipherDec.doFinal(encryptedInput);

                // boolean isUserObject = false;
                String response = "";

                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decryptedBytes))) {
                    Object obj = ois.readObject();
                    if (obj instanceof User) {
                        // isUserObject = true;

                        response = handleAddToDatabase((User) obj);

                        // Ghi ảnh người dùng
                        // String userImagePath = "src/main/java/com/facerecognition/Server/received_images/" + user.getUid() + ".jpg";
                        
                        // File faceImageFile = writeBytesToFile(, userImagePath);
                        // System.out.println("🖼️ Lưu ảnh user tại: " + faceImageFile.getAbsolutePath());

                        // Gửi tới API Flask hoặc xử lý gì đó
                        // response = processData(faceImageFile); // xử lý nhận diện từ ảnh

                        
                    }
                } catch (Exception ex) {
                    // Không phải object, xử lý như ảnh thường
                    // isUserObject = false;

                    // Ghi ảnh ra file
                    File file = writeBytesToFile(decryptedBytes, IMAGE_SAVE_PATH + "/image_" + System.currentTimeMillis() + ".jpg");
                    System.out.println("📸 Đã nhận ảnh từ client: " + file.getAbsolutePath());

                    // Gọi xử lý dữ liệu (ví dụ: gửi ảnh tới API Flask)
                    response = processData(file); // có thể là kết quả nhận diện khuôn mặt
                    System.out.println(response);
                }

                // Gửi phản hồi đã mã hóa lại cho client
                byte[] encryptedResponse = aesCipherEnc.doFinal(response.getBytes());
                writer.writeInt(encryptedResponse.length);
                writer.write(encryptedResponse);
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println("Lỗi mã hóa/giao tiếp với client: " + e.getMessage());
        }
    }

    private JSONObject postImageAPI(String urlApi, File imageFile) {
        // File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return null;
        }

        byte[] imageBytes = null;
        try {
            imageBytes = Files.readAllBytes(imageFile.toPath());
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

        dataUserJson = new JSONObject(responseBody);

        return dataUserJson;
    }

    private String formatStringDataJson(JSONObject obj) {
        
        String result = "";
        result += "--> FACE RECOGNITION\n";
        switch (obj.getString("status")) {
            case "success":
                String uid = obj.getString("uid");
                UserController users = new UserController();
                users.getListUserDatabase();
                User userFind = users.findUserByUid(users.getUserList(), uid); 
                result += "--> Trạng thái: Tìm thấy\n";
                result += "--> UID: " + uid + "\n";
                result += "--> Họ và tên: " + userFind.getName() + "\n";
                result += "--> Ngày sinh: " + userFind.getDob() + "\n";
                result += "--> ĐỘ KHỚP SO VỚI ẢNH: " + obj.getDouble("distance")*100 + " %\n";
                break;
            
            case "fail":
                result += "111";
                result += "\n--> Trạng thái: Không tìm thấy\n";
                break;
        
            default:
                break;
        }
        return result;
    }

    private String handleAddToDatabase(User user) {
        JSONArray jsonArray = dataUserJson.getJSONArray("face_encoding");
        List<Double> face_encoding = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            face_encoding.add(jsonArray.getDouble(i));
        }

        user.setFaceEncoding(face_encoding);

        // Log thông tin
        System.out.println("👤 Nhận user:");
        System.out.println("  - UID: " + user.getUid());
        System.out.println("  - Tên: " + user.getName());
        System.out.println("  - Ngày sinh: " + user.getDob());
        System.out.println("  - Avatar: " + user.getAvata());

        UserController uc = new UserController();
        if (uc.insertUser(user)) {
            return "Đã thêm vào CSDL!";
        }
        return "Lỗi!";
    
    }

    private String processData(File inputImage) {
        // StringBuilder response = new StringBuilder(input);
        String urlApi = "http://localhost:5000/api/recognition";
        JSONObject results = this.postImageAPI(urlApi, inputImage);
        // return "Server phản hồi: " + response.toString();
        return formatStringDataJson(results);
    }

    private File writeBytesToFile(byte[] data, String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
        return file;
    }

    public static void main(String[] args) {
        Server server = new Server(12345);
        server.start();
    }
}
