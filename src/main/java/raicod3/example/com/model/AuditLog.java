package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private String email;

    private String action;

    private String status;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime createdAt;

    public AuditLog(String userId, String email, String action, String status, String ipAddress, String details) {
        this.userId = userId;
        this.email = email;
        this.action = action;
        this.status = status;
        this.ipAddress = ipAddress;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }
}
