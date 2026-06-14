package raicod3.example.com.dto.provider;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OnboardingProviderRequestDto {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotEmpty(message = "At least one service must be provided")
    private List<String> services;

    @NotBlank(message = "Experience level is required")
    private String experience;

    @NotEmpty(message = "At least one work area must be provided")
    private List<String> workArea;

    @NotBlank(message = "Bio is required")
    private String bio;

    @NotBlank(message = "Pricing basis is required")
    private String pricingBasis;

    @NotNull(message = "Starting rate cannot be null")
    @Min(value = 0, message = "Starting rate cannot be negative")
    private Integer startingRate;
}