package raicod3.example.com.dto.user;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OTPResendDto {
    String userId;
}
