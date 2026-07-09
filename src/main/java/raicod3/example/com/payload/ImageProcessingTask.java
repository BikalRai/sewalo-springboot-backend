package raicod3.example.com.payload;

import java.util.List;
import java.util.UUID;

public record ImageProcessingTask(
        UUID jobId,
        String userId, // We need this to route the WebSocket message
        List<String> imagePaths
) {
}
