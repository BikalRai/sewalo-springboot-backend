package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.provider.OnboardingProviderRequestDto;
import raicod3.example.com.dto.provider.ProviderCreditsResponseDto;
import raicod3.example.com.dto.provider.ProviderResponseDto;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.ProviderCredits;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final ProviderCreditsRepository providerCreditsRepository;

    public ProviderResponseDto findById(UUID id) {
        ProviderProfile provider = providerRepository.findByUserId(id);

        if(provider == null){
            throw new ResourceNotFoundException("Provider not found");
        }

        return new ProviderResponseDto(provider, provider.getUser());
    }

    public ProviderCreditsResponseDto getProviderCredits(UUID providerId) {
        log.debug("Fetching provider with ID: {}", providerId);
        ProviderProfile provider = providerRepository.findByUserId(providerId);

        if (provider == null) {
            throw new ResourceNotFoundException("Provider not found");
        }

        log.debug("Fetching provider credits...");
        ProviderCredits credits =  providerCreditsRepository.findById(provider.getId()).orElseThrow(() -> new ResourceNotFoundException("provider credits not found"));

        return new ProviderCreditsResponseDto(provider.getId(), credits.getBalance());
    }

    @Auditable(action = "PROVIDER_PERSONAL_DETAILS")
    @Transactional
    public APIResponse updateProviderProfile (OnboardingProviderRequestDto dto, String email) {
        log.debug("Validating user...");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized. user not found"));

        log.debug("Updating provider's user details...");
        user.setImageUrl(dto.getImageUrl());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setOnboarded(true);

        // --- NEW: Handle User Address Mapping ---
        log.debug("Updating user address...");
        UserAddress address = user.getUserAddress();
        if (address == null) {
            // If they don't have an address record yet, create one
            address = new UserAddress();
            address.setUser(user);
            user.setUserAddress(address); // Link it back to the user for the cascade
        }
        // Update the coordinates and formatted string
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        address.setFormattedAddress(dto.getAddress());

        log.debug("Fetching pre-existing provider details...");
        ProviderProfile providerProfile = providerRepository.findByUserId(user.getId());

        if (providerProfile == null) {
            throw new IllegalStateException("Critical Data Error: Provider profile is missing for a registered user.");
        }

        // This now ONLY updates bio, services, rates, etc.
        providerProfile.updateFromDto(dto);

        // Because of CascadeType.ALL on UserAddress, saving the User saves the Address too.
        userRepository.save(user);
        providerRepository.save(providerProfile);

        log.info("Updating provider profile details successful.");

        return APIResponse.success(new ProviderResponseDto(providerProfile, user), "Updated provider personal details", 200);
    }
}
