package raicod3.example.com.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.job.JobUnlockResponseDto;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobUnlock;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.JobUnlockRepository;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.List;
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

    @Auditable(action = "PROVIDER_UNLOCKED_JOB")
    @Transactional
    public JobUnlockResponseDto unlockJob(UUID userId, UUID jobId) {
        log.debug("Validating provider...");
        ProviderProfile provider = providerProfileRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        log.debug("Unlocking job with id {}", jobId);
        // 1. idempotency check - reject if already unlocked
        if(jobUnlockRepository.existsByJob_IdAndProvider_Id(jobId, provider.getId())) {
            throw new BadRequestException("You have already unlocked this job.");
        }

        // 2. atomic credit deduction - fails safely if balance insufficient
        int rowsUpdated = providerCreditsRepository.deductCredits(provider.getId(), UNLOCK_COST);
        if(rowsUpdated == 0) {
            throw new BadRequestException("Insufficient credits to unlock this job..");
        }

        // 3. fetch the actual entities needed to build the Job unlock row
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found."));

        // 4. record the unlock - audit trail + DB-level unique constraint backstop
        JobUnlock unlock = new  JobUnlock();
        unlock.setJob(job);
        unlock.setProvider(provider);
        unlock.setTokensSpent(UNLOCK_COST);

        log.info("Unlocked job successfully");

        JobUnlock result = jobUnlockRepository.save(unlock);

        log.info("Job unlocked successfully: {}", result);

        return JobUnlockResponseDto.from(result);
    }

    public List<JobUnlockResponseDto> unlockedobs(UUID userId) {
        log.debug("Validating provider...");
        ProviderProfile provider = providerProfileRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        List<JobUnlock> unlocks = jobUnlockRepository.findAllByProviderId(provider.getId());

        return unlocks.stream().map(JobUnlockResponseDto::from).toList();
    }

}
