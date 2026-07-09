package raicod3.example.com.payload;

import java.util.List;
import java.util.UUID;

public record JobAnalysisEvent(UUID jobId,
                               String userId,
                               List<String> imageUrls) {

}
