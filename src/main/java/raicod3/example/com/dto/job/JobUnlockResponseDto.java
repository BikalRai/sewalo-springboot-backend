package raicod3.example.com.dto.job;

import raicod3.example.com.model.JobUnlock;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobUnlockResponseDto(UUID unlockId, UUID jobId, int tokensSpent, LocalDateTime unlockedAt) {
    public static  JobUnlockResponseDto from(JobUnlock unlock) {
        return new JobUnlockResponseDto(
                unlock.getId(),
                unlock.getJob().getId(),
                unlock.getTokensSpent(),
                unlock.getCreatedAt()
        );
    }
}
