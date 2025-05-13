package com.facerecognition.Server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class test {

    public JSONObject getDataAPI(String urlApi, JSONObject data) {
        JSONObject jsonResponse;
        System.out.println(urlApi);
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlApi))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            jsonResponse = new JSONObject(response.body());
        return jsonResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public JSONObject postImageAPI(String urlApi, String imagePath) {
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

    public static void main(String[] args) {
        String urlApi = "http://localhost:5000/api/recognition";
        String relativePath = "received_images//unknown.jpg";
        Path path = Paths.get(relativePath).toAbsolutePath();
        File imageFile = path.toFile();
        test t = new test();
        t.postImageAPI(urlApi, imageFile.getAbsolutePath());
    }
}
