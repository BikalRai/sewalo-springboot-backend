package raicod3.example.com.dto.user;

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
    private Double lat;

    @NotNull
    private Double lng;

    @NotBlank
    private String address;
}
