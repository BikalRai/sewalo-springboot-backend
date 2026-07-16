package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.model.Bid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    boolean existsByJobIdAndProviderIdAndStatus(UUID jobId, UUID providerId, BidStatus status);

    // All bids on a job — homeowner uses this
    List<Bid> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    // All bids by a provider — provider dashboard
    List<Bid> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    // Check if provider already bid on this job
    boolean existsByJobIdAndProviderId(UUID jobId, UUID providerId);

    // Count active (non-withdrawn) bids on a job — enforce max 3
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.job.id = :jobId AND b.status != 'WITHDRAWN'")
    int countActiveBids(@Param("jobId") UUID jobId);

    // For accepting a bid — need to reject all others
    List<Bid> findByJobIdAndStatusNot(UUID jobId, BidStatus status);

    Optional<Bid> findByIdAndProviderId(UUID bidId, UUID providerId);
}
