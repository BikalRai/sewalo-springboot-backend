package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.enums.BidStatus;

@Entity
@Table(name = "bids", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_id", "provider_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Bid extends AbstractBaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Integer quotedPrice;

    private String pricingBasis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    private Boolean contactUnlocked;

    protected void onCreate() {
        super.onCreate();
        this.status = BidStatus.PENDING;
        this.contactUnlocked = false;
    }
}
