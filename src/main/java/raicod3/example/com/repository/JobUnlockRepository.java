package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.JobUnlock;

import java.util.UUID;

@Repository
public interface JobUnlockRepository extends JpaRepository<JobUnlock, UUID> {

    boolean existsByJobIdAndProviderId(UUID jobId, UUID providerId);
}
