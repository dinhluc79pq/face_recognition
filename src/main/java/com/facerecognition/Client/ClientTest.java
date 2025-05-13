package com.facerecognition.Client;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;

public class ClientTest {
    private String host;
    private int port;

    public ClientTest(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Đã kết nối đến server " + socket.getRemoteSocketAddress());

            // 1. Nhận public key từ server
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objIn = new ObjectInputStream(inStream);
            byte[] publicKeyBytes = (byte[]) objIn.readObject();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

            // 2. Sinh AES key và gửi cho server sau khi mã hóa bằng RSA
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey aesKey = keyGen.generateKey();

            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.writeObject(encryptedAesKey);
            objOut.flush();

            // 3. Bắt đầu giao tiếp bằng AES
            startCommunication(socket, aesKey);

        } catch (Exception e) {
            System.err.println("Lỗi kết nối đến server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startCommunication(Socket socket, SecretKey aesKey) {
        try (Scanner scanner = new Scanner(System.in);
             DataInputStream reader = new DataInputStream(socket.getInputStream());
             DataOutputStream writer = new DataOutputStream(socket.getOutputStream())) {

            Cipher aesCipherEnc = Cipher.getInstance("AES");
            Cipher aesCipherDec = Cipher.getInstance("AES");
            aesCipherEnc.init(Cipher.ENCRYPT_MODE, aesKey);
            aesCipherDec.init(Cipher.DECRYPT_MODE, aesKey);

            String userInput;
            while (true) {
                System.out.print("Nhập dữ liệu: ");
                userInput = scanner.nextLine();

                // Gửi dữ liệu đã mã hóa
                byte[] encryptedData = aesCipherEnc.doFinal(userInput.getBytes());
                writer.writeInt(encryptedData.length);
                writer.write(encryptedData);

                // Nhận phản hồi
                int len = reader.readInt();
                byte[] responseBytes = new byte[len];
                reader.readFully(responseBytes);
                String response = new String(aesCipherDec.doFinal(responseBytes));
                System.out.println("Server: " + response);

                if (userInput.equalsIgnoreCase("bye")) {
                    System.out.println("Client gửi yêu cầu đóng kết nối.");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi/nhận dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientTest client = new ClientTest("localhost", 12345);
        client.start();
    }
}
