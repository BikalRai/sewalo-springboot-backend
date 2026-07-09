package raicod3.example.com.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.service.JobProcessingNotifier;

import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JobProcessingWorker {
    private final RestClient restClient;
    private final JobProcessingNotifier notifier;

    public JobProcessingWorker(JobProcessingNotifier notifier) {
        this.notifier = notifier;

        // Create a factory with explicit timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds to connect to Ollama
        factory.setReadTimeout(180000);  // 3 minutes max for the AI to generate a response

        // Build the RestClient with the factory bound to Ollama's local address
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("http://localhost:11434")
                .build();
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_ANALYSIS_QUEUE)
    public void processJobImage(JobAnalysisEvent event) {
        log.info("Started processing image for Job ID: {}", event.jobId());

        try {
            // 1. Extract the URL string
            String imageUrl = event.imageUrls().get(0);
            byte[] imageBytes;

            // 2. Open an HTTP connection to the URL and stream the bytes into memory
            // Using try-with-resources ensures the network stream is closed automatically to prevent memory leaks
            try (InputStream in = new URI(imageUrl).toURL().openStream()) {
                imageBytes = in.readAllBytes();
            }

            // 3. Convert the in-memory bytes to Base64 for Ollama
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 4. Send to AI and notify
            String difficulty = analyzeImageWithGemma(base64Image);

            log.info("AI Analysis completed successfully for Job ID: {}. Resulting Difficulty: {}", event.jobId(), difficulty);

            notifier.notifyUserJobActive(event.userId(), event.jobId(), difficulty);

        } catch (Exception e) {
            log.error("Failed to process image for Job {}", event.jobId(), e);
            notifier.notifyUserJobFailed(event.userId(), event.jobId(), "AI processing failed. Please set difficulty manually.");
        }
    }

    private String analyzeImageWithGemma(String base64Image) {
        // 1. DIAGNOSTIC LOG: How big is this image actually?
        log.info("Base64 Image length is: {} characters", base64Image.length());

        if (base64Image.length() > 5_000_000) {
            log.warn("WARNING: This image is extremely large. The HTTP request might choke!");
        }

        Map<String, Object> requestPayload = Map.of(
                "model", "llava",
                "prompt", "Analyze this image and determine the difficulty of the service job. Reply with ONLY one word: LOW, MEDIUM, or HIGH.",
                "images", List.of(base64Image),
                "stream", false
        );

        // 2. DIAGNOSTIC LOG: Right before the network call
        log.info("Executing POST request to Ollama at http://localhost:11434/api/generate...");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/generate")
                .body(requestPayload)
                .retrieve()
                .body(Map.class);

        // 3. DIAGNOSTIC LOG: Right after the network call returns
        log.info("Successfully received HTTP response from Ollama API!");

        if (response != null && response.containsKey("response")) {
            String aiResponse = String.valueOf(response.get("response"));
            return aiResponse.trim().toUpperCase();
        }

        throw new RuntimeException("Invalid response from Ollama API");
    }
}