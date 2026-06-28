package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_wallets")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TokenWallet extends AbstractBaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    private Integer balance;

    private LocalDateTime lastUpdated;

    protected void onCreate() {
        this.balance = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
