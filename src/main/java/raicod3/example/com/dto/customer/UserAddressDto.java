package raicod3.example.com.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserAddressDto {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotBlank
    private String formattedAddress;
}
