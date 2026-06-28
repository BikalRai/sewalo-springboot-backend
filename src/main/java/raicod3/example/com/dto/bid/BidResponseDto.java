package raicod3.example.com.dto.bid;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import raicod3.example.com.enums.BidStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class BidResponseDto {
    private UUID id;
    private UUID jobId;

    // Provider info visible to homeowner
    private UUID providerId;
    private String providerName;
    private String providerImageUrl;
    private String providerBio;
    private Integer providerStartingRate;

    private String message;
    private Integer quotedPrice;
    private String pricingBasis;
    private BidStatus status;
    private Boolean contactUnlocked;

    // Only populated after unlock
    private String providerPhone;

    private LocalDateTime createdAt;
}
