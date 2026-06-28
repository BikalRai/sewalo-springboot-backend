package raicod3.example.com.dto.job;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.enums.Urgency;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class JobResponseDto {
    private UUID id;
    private String title;
    private String description;
    private String categoryName;
    private String categoryIcon;
    private Urgency urgency;
    private JobStatus status;
    private List<String> images;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Double latitude;
    private Double longitude;
    private String address;
    private String customerName;
    private String customerImageUrl;
    private int bidCount;
}
