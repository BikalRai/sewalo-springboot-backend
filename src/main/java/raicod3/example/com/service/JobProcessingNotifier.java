package raicod3.example.com.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import raicod3.example.com.payload.JobStatusPayload;

@Service
public class JobProcessingNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public JobProcessingNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUserJobActive(String userId, Long jobId, String difficulty) {
        JobStatusPayload payload = new JobStatusPayload(jobId, "ACTIVE", difficulty, null);
        messagingTemplate.convertAndSendToUser(userId, "/queue/job-updates", payload);
    }

    public void notifyUserJobFailed(String userId, Long jobId, String reason) {
        JobStatusPayload payload = new JobStatusPayload(jobId, "FAILED", null, reason);
        messagingTemplate.convertAndSendToUser(userId, "/queue/job-updates", payload);
    }
}
