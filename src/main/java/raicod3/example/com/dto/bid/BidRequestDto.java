package raicod3.example.com.dto.bid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidRequestDto {

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Quoted price is required")
    private Integer quotedPrice;

    @NotBlank(message = "Pricing basis is required")
    private String pricingBasis;
}
