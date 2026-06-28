package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.model.Job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<Job> findByStatusAndExpiresAtBefore(JobStatus status, LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.job.id = :jobId AND b.status != 'WITHDRAWN'")
    int countActiveBids(@Param("jobId") UUID jobId);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.job.id = :jobId AND b.provider.id = :providerId")
    int countBidByProvider(@Param("jobId") UUID jobId, @Param("providerId") UUID providerId);
}
