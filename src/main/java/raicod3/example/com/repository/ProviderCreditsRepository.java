package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.ProviderCredits;

import java.util.UUID;

@Repository
public interface ProviderCreditsRepository extends JpaRepository<ProviderCredits, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProviderCredits pc SET pc.balance = pc.balance - :amount " + "WHERE pc.providerId = :providerId and pc.balance >= :amount")
    int deductCredits(@Param("providerId") UUID providerId, @Param("amount") int amount);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProviderCredits  pc SET pc.balance = pc.balance + :amount " + "WHERE pc.providerId = :providerId")
    int addCredits(@Param("providerId") UUID providerId, @Param("amount") int amount);
}
