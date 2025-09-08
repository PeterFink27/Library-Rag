package Neon_db.library_rag;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenAiClient {
    private final String apiKey;
    private final String embedModel;
    private final String chatModel;

    public OpenAiClient(
        @Value("${openai.apiKey:}") String apiKey,
        @Value("${openai.embedModel:text-embedding-3-small}") String embedModel,
        @Value("${openai.chatModel:gpt-4o-mini}") String chatModel
    ) {
        this.apiKey = apiKey;
        this.embedModel = embedModel;
        this.chatModel = chatModel;
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing OpenAI API key. Set OPENAI_API_KEY env var or openai.apiKey in application.yml");
        }
        try {
            String body = """
            {
                "model": "%s",
                "input": %s
            }
            """.formatted(embedModel, om.writeValueAsString(text));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI error: " + res.statusCode() + " " + res.body());
            }

            JsonNode root = om.readTree(res.body());
            JsonNode arr = root.get("data").get(0).get("embedding"); // array of numbers
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public String chat(String system, String user) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing OpenAI API key. Set OPENAI_API_KEY env var or openai.apiKey in application.yml");
        }
        try {
            String body = """
            {
                "model": "%s",
                "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                ]
            }
            """.formatted(
                chatModel,
                om.writeValueAsString(system),
                om.writeValueAsString(user)
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI error: " + res.statusCode() + " " + res.body());
            }

            JsonNode root = om.readTree(res.body());
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new RuntimeException("No choices in OpenAI response: " + res.body());
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new RuntimeException("No message content in OpenAI response: " + res.body());
            }
            return message.get("content").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
