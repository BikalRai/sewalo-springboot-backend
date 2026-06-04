package raicod3.example.com.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserAddressResponseDto {
    private UUID id;
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserResponseDto user;

    public UserAddressResponseDto(UserAddress address, User user) {
        this.id = address.getId();
        this.latitude = address.getLatitude();
        this.longitude = address.getLongitude();
        this.address = address.getFormattedAddress();
        this.createdAt = address.getCreatedAt();
        this.updatedAt = address.getUpdatedAt();
        this.user = new  UserResponseDto(user);
    }
}