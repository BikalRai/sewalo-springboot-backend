package raicod3.example.com.payload;

import java.util.UUID;

public record JobStatusPayload(
        UUID jobId,
        String status,     // "ACTIVE" or "FAILED"
        String difficulty, // Will be null if it failed
        String errorReason // Optional: tell the frontend what went wrong
) {
}
