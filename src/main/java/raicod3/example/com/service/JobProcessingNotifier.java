package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.enums.JobDifficulty;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Job;
import raicod3.example.com.payload.JobStatusPayload;
import raicod3.example.com.repository.JobRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessingNotifier {

    private final JobRepository jobRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notifyUserJobActive(String userId, UUID jobId, String difficulty) {
        // 1. Database Update: Persistence is mandatory
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        job.setStatus(JobStatus.OPEN);
        try {
            job.setDifficulty(JobDifficulty.valueOf(difficulty));
        } catch (Exception e) {
            job.setDifficulty(JobDifficulty.MEDIUM);
        }
        jobRepository.save(job);

        // 2. Real-Time Update: Push to WebSocket
        JobStatusPayload payload = new JobStatusPayload(jobId, "OPEN", difficulty, null);
        messagingTemplate.convertAndSendToUser(userId, "/queue/job-updates", payload);

        log.info("Job {} updated to OPEN and notified user {}", jobId, userId);
    }

    @Transactional
    public void notifyUserJobFailed(String userId, UUID jobId, String reason) {
        // 1. Database Update: Optional - keep as ANALYZING or set to FAILED?
        // Usually, we set to FAILED so the user knows they need to intervene
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.FAILED);
        jobRepository.save(job);

        // 2. Real-Time Update
        JobStatusPayload payload = new JobStatusPayload(jobId, "FAILED", null, reason);
        messagingTemplate.convertAndSendToUser(userId, "/queue/job-updates", payload);

        log.error("Job {} failed: {}", jobId, reason);
    }
}