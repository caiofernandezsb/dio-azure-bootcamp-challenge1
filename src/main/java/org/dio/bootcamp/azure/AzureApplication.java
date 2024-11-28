package org.dio.bootcamp.azure;

import com.google.gson.JsonArray;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@SpringBootApplication
public class AzureApplication {

    private static String subscriptionKey = "";
    private static String location = "brazilsouth";
    private static String endPoint = "https://api.cognitive.microsofttranslator.com";

    private static final String AZURE_GPT_KEY = "";
    private static final String AZURE_GPT_ENDPOINT = "https://caio-m3x3uxgo-swedencentral.cognitiveservices.azure.com/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-08-01-preview";

    public static void main(String[] args) {
        SpringApplication.run(AzureApplication.class, args);

        try {
            String webText = new AzureApplication().extractTextFromWeb("https://www.google.com.br");
            String response = new AzureApplication().translate(webText, "pt-br", "en");
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String webText = new AzureApplication().extractTextFromWeb("https://www.bing.com.br");
            String response = new AzureApplication().translateWithChatGPT(webText, "pt-br", "en");
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String translateWithChatGPT(String text, String sourceLanguage, String targetLanguage) throws IOException {
        URL url = new URL(AZURE_GPT_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("api-key", AZURE_GPT_KEY);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setDoOutput(true);

        String prompt = String.format("Por favor, traduza o seguinte texto de %s para %s:\n\n%s", sourceLanguage, targetLanguage, text);
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", messages);

        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write(requestBody.toString().getBytes());
            wr.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                JsonObject responseObject = new Gson().fromJson(response.toString(), JsonObject.class);
                JsonArray choices = responseObject.getAsJsonArray("choices");
                return choices.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
            }
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }

    public String extractTextFromWeb(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        return doc.text();
    }

    public String translate(String text, String sourceLanguage, String targetLanguage) throws IOException {
        String path = "/translate?api-version=3.0&from=" + sourceLanguage + "&to=" + targetLanguage;
        URL url = new URL(endPoint + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
        connection.setRequestProperty("Ocp-Apim-Subscription-Region", location);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        String payload = String.format("[{\"Text\": \"%s\"}]", text);

        try {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(payload.getBytes());
            wr.flush();
            wr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }

                JsonArray jsonArray = new Gson().fromJson(response.toString(), JsonArray.class);

                return jsonArray.get(0).getAsJsonObject().getAsJsonArray("translations").get(0)
                        .getAsJsonObject().get("text").toString();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }

    }

}
