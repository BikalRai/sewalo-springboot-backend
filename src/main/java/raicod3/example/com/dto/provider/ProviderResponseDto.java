package raicod3.example.com.dto.provider;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProviderResponseDto {


    private UUID id;

    private String gender;

    private List<String> workDistrict;

    private List<String> services;

    private String bio;

    private String pricingBasis;

    private Integer startingRate;

    private UserResponseDto user;

    public ProviderResponseDto(ProviderProfile profile, User user) {
        this.id = profile.getId();
        this.gender = profile.getGender();
        this.workDistrict = profile.getWorkDistrict();
        this.bio = profile.getBio();
        this.pricingBasis = profile.getPricingBasis();
        this.startingRate = profile.getStartingRate();
        this.services = profile.getServices();
        this.user = new UserResponseDto(user);
    }
}
