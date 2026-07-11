package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.enums.PurchaseStatus;

@Entity
@Table(name = "credit_purchase")
@NoArgsConstructor
@Getter
@Setter
public class CreditPurchase extends AbstractBaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @Column(nullable = false, unique = true)
    private String pidx; // khalti's payment identifier

    @Column(nullable = false)
    private Integer creditsRequested;

    @Column(nullable = false)
    private Integer amountPaisa;  // amount in paisa, matches Khalti's unit

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;
}
