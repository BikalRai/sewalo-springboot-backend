package raicod3.example.com.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.payload.ImageProcessingTask;
import raicod3.example.com.service.JobProcessingNotifier;

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

        // Build the RestClient with the factory
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("http://localhost:11434")
                .build();
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_ANALYSIS_QUEUE)
    public void processJobImage(ImageProcessingTask task) {
        log.info("Started processing image for Job ID: {}", task.jobId());

        try {
            // 1. Call Gemma 4 via Ollama
            String difficulty = analyzeImageWithGemma(task.base64Image());

            // 2. Update database to ACTIVE
            // jobService.updateJobStatus(task.jobId(), "ACTIVE", difficulty);

            // 3. Notify frontend of SUCCESS
            notifier.notifyUserJobActive(task.userId(), task.jobId(), difficulty);

        } catch (Exception e) {
            log.error("Failed to process image for Job {}", task.jobId(), e);

            // 1. Update database to FAILED so the system knows it's broken
            // jobService.updateJobStatus(task.jobId(), "FAILED", null);

            // 2. Notify frontend of FAILURE to stop the loading bar
            notifier.notifyUserJobFailed(task.userId(), task.jobId(), "AI processing failed. Please set difficulty manually.");
        }
    }

    private String analyzeImageWithGemma(String base64Image) {
        // Construct the strict JSON payload Ollama requires for multimodal processing
        Map<String, Object> requestPayload = Map.of(
                "model", "gemma4:12b",
                "prompt", "Analyze this image and determine the difficulty of the service job. Reply with ONLY one word: LOW, MEDIUM, or HIGH.",
                "images", List.of(base64Image),
                "stream", false
        );

        // Make the HTTP POST request to Ollama and map it to a generic Java Map
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/generate")
                .body(requestPayload)
                .retrieve()
                .body(Map.class); // No imports needed for Map!

        if (response != null && response.containsKey("response")) {
            // Extract the "response" text field from the JSON map
            String aiResponse = String.valueOf(response.get("response"));
            return aiResponse.trim().toUpperCase();
        }

        throw new RuntimeException("Invalid response from Ollama API");
    }
}
