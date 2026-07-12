package raicod3.example.com.dto.bid;

import lombok.Builder;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.model.Bid;

import java.time.LocalDateTime;
import java.util.UUID;


@Builder
public record BidConfirmationDto(UUID id,
                             UUID jobId,
                             String message,
                             Integer quotedPrice,
                             String pricingBasis,
                             BidStatus status,
                             LocalDateTime createdAt) {
    public static BidConfirmationDto from(Bid bid) {
        return BidConfirmationDto.builder()
                .id(bid.getId())
                .jobId(bid.getJob().getId())
                .message(bid.getMessage())
                .quotedPrice(bid.getQuotedPrice())
                .pricingBasis(bid.getPricingBasis())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .build();
    }

}
