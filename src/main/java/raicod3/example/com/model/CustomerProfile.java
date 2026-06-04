package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "customers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "user")
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;
}
