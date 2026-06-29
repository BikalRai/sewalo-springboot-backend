package raicod3.example.com.payload;

public record ImageProcessingTask(
        Long jobId,
        String userId, // We need this to route the WebSocket message
        String base64Image
) {
}
