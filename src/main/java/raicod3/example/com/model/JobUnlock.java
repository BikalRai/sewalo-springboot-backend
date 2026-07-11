package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_unlocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_id", "provider_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobUnlock extends AbstractBaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    private Integer tokensSpent;

}
