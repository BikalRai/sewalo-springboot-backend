package raicod3.example.com.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobUnlock;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.JobUnlockRepository;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class JobUnlockService {
    private static final int UNLOCK_COST = 1;

    private final ProviderProfileRepository providerProfileRepository;
    private final JobRepository jobRepository;
    private final JobUnlockRepository jobUnlockRepository;
    private final ProviderCreditsRepository providerCreditsRepository;

    @Transactional
    public JobUnlock unlockJob(UUID providerId, UUID jobId) {
        log.debug("Unlocking job with id {}", jobId);
        // 1. idempotency check - reject if already unlocked
        if(jobUnlockRepository.existsByJob_IdAndProvider_Id(jobId, providerId)) {
            throw new BadRequestException("You have already unlocked this job.");
        }

        // 2. atomic credit deduction - fails safely if balance insufficient
        int rowsUpdated = providerCreditsRepository.deductCredits(providerId, UNLOCK_COST);
        if(rowsUpdated == 0) {
            throw new BadRequestException("Insufficient credits to unlock this job..");
        }

        // 3. fetch the actual entities needed to build the Job unlock row
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found."));

        ProviderProfile providerProfile = providerProfileRepository.findById(providerId).orElseThrow(() -> new ResourceNotFoundException("Provider profile not found."));

        // 4. record the unlock - audit trail + DB-level unique constraint backstop
        JobUnlock unlock = new  JobUnlock();
        unlock.setJob(job);
        unlock.setProvider(providerProfile);
        unlock.setTokensSpent(UNLOCK_COST);

        log.info("Unlocked job successfully");

        return jobUnlockRepository.save(unlock);
    }

}
