package raicod3.example.com.dto.google;

import lombok.*;
import raicod3.example.com.enums.UserRole;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class GoogleOnboardingRequestDto {
    private String role;
}
