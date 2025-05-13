package com.facerecognition.Client;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class Client {
    private String host;
    private int port;
    private SecretKey aesKey;
    private Socket socket;
    private DataOutputStream writer;
    private DataInputStream reader;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);

            // 1. Nhận public key từ server
            ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
            byte[] publicKeyBytes = (byte[]) objIn.readObject();
            PublicKey serverPublicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            // 2. Sinh khóa AES và gửi (mã hóa bằng RSA)
            aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.writeObject(encryptedAesKey);
            objOut.flush();

            // 3. Gắn stream
            writer = new DataOutputStream(socket.getOutputStream());
            reader = new DataInputStream(socket.getInputStream());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String sendImage(File imageFile) {
        try {
            // Đọc ảnh thành byte[]
            byte[] imageBytes = readFileToBytes(imageFile);

            // Mã hóa ảnh
            Cipher aesCipherEnc = Cipher.getInstance("AES");
            aesCipherEnc.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedData = aesCipherEnc.doFinal(imageBytes);

            // Gửi
            writer.writeInt(encryptedData.length);
            writer.write(encryptedData);

            // Nhận phản hồi
            int len = reader.readInt();
            byte[] responseBytes = new byte[len];
            reader.readFully(responseBytes);

            Cipher aesCipherDec = Cipher.getInstance("AES");
            aesCipherDec.init(Cipher.DECRYPT_MODE, aesKey);
            String response = new String(aesCipherDec.doFinal(responseBytes));
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi gửi ảnh: " + e.getMessage();
        }
    }

    

    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

