package raicod3.example.com.payload;

public record JobStatusPayload(
        Long jobId,
        String status,     // "ACTIVE" or "FAILED"
        String difficulty, // Will be null if it failed
        String errorReason // Optional: tell the frontend what went wrong
) {
}
