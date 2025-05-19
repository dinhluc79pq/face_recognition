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
                int len = reader.readInt();
                boolean check = reader.readBoolean();
                byte[] encryptedInput = new byte[len];
                reader.readFully(encryptedInput);
                byte[] decryptedBytes = aesCipherDec.doFinal(encryptedInput);

                String response = "";
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decryptedBytes))) {
                    Object obj = ois.readObject();
                    if (obj instanceof User) {
                        response = handleAddToDatabase((User) obj);   
                    }
                } catch (Exception ex) {
                    File file = writeBytesToFile(decryptedBytes, IMAGE_SAVE_PATH + "/image_" + System.currentTimeMillis() + ".jpg");
                    System.out.println("📸 Đã nhận ảnh từ client: " + file.getAbsolutePath());
                    
                    if (check) {
                        response = processData(file); 
                    }
                    else {
                        response = processToDetected(file);
                    }
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
                .uri(URI.create(urlApi))
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
                result += "--> Đường dẫn đến ảnh gốc: " + userFind.getAvata() + "\n";
                result += "--> ĐỘ KHỚP SO VỚI ẢNH: " + String.format("%.2f", obj.getDouble("distance")*100) + " %\n";
                break;
            
            case "fail":
                JSONArray results = obj.getJSONArray("result");
                result += "--> Trạng thái: Không tìm thấy\n";
                result += "--> STT\tUID\t\t\t\tĐỘ KHỚP\n";
                for (int i = 0; i < results.length(); i++) {
                    JSONObject match = results.getJSONObject(i);
                    String uidFace = match.getString("uid");
                    double distance = match.getDouble("distance");

                    result += "--> " + (i + 1) + "\t" + uidFace + "\t" + String.format("%.2f", distance) + "\n";
                }
                break;
        
            default:
                result += "--> LỖI HỆ THỐNG";
                break;
        }
        return result;
    }

    private String formatStringDetected(JSONObject obj) {
        StringBuilder result = new StringBuilder();

        String status = obj.optString("status");
        int faceCount = obj.optInt("faces_detected", 0);

        result.append("--> Trạng thái: ").append(status.toUpperCase()).append("\n");
        result.append("--> Số khuôn mặt phát hiện: ").append(faceCount).append("\n");

        if (obj.has("matches")) {
            JSONArray matches = obj.getJSONArray("matches");
            UserController users = new UserController();
            users.getListUserDatabase();
    
            result.append("✅ Khuôn mặt nhận diện được: ").append(matches.length()).append("\n");
            result.append("--> STT\tUID\t\t\t\tHọ và tên\t\t\t\tĐỘ KHỚP\n");

            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String uid = match.getString("uid");
                User userFind = users.findUserByUid(users.getUserList(), uid); 
                double distance = match.getDouble("distance");

                result.append("--> ").append((i + 1)).append("\t").append(uid).append("\t").append(userFind.getName())
                    .append("\t\t").append(String.format("%.2f", distance)).append("\n");
            }
        } else {
            result.append("⚠️ Không có khuôn mặt nào khớp với dữ liệu.\n");
        }

        if (obj.has("unmatched")) {
            JSONArray unmatched = obj.getJSONArray("unmatched");
            result.append("❌ Khuôn mặt chưa xác định: ").append(unmatched.length()).append("\n");
        }
        return result.toString();
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

    private String processToDetected(File inputImage) {
        String urlApi = "http://localhost:5000/api/detected";
        JSONObject results = this.postImageAPI(urlApi, inputImage);
        return formatStringDetected(results);
    }

    private String processData(File inputImage) {
        String urlApi = "http://localhost:5000/api/recognition";
        JSONObject results = this.postImageAPI(urlApi, inputImage);
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
