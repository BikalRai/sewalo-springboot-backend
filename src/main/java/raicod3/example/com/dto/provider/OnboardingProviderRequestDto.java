package raicod3.example.com.dto.provider;

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
    @NotBlank
    String imageUrl;
    @NotBlank String phoneNumber;
    @NotBlank String gender;
    @NotEmpty
    List<String> services;
    @NotBlank String experience;
    @NotEmpty
    List<String> workArea;
    @NotBlank String bio;
    @NotBlank String pricingBasis;
    @NotBlank String startingRate;
}
