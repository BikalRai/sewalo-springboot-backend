package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.provider.OnboardingProviderRequestDto;
import raicod3.example.com.dto.provider.ProviderResponseDto;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.ProviderRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    @Auditable(action = "PROVIDER_PERSONAL_DETAILS")
    @Transactional
    public APIResponse updateProviderProfile (OnboardingProviderRequestDto dto, String email) {
        log.debug("Validating user...");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Unauthorized. user not found"));

        log.debug("Updating provider's user details...");
        user.setImageUrl(dto.getImageUrl());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setOnboarded(true);

        log.debug("Fetching pre-existing provider details...");
        ProviderProfile providerProfile = providerRepository.findByUserId(user.getId());

        if (providerProfile == null) {
            throw new IllegalStateException("Critical Data Error: Provider profile is missing for a registered user.");
        }

        providerProfile.updateFromDto(dto);

        userRepository.save(user);
        providerRepository.save(providerProfile);

        log.info("Updating provider profile details successful.");

        return APIResponse.success(new ProviderResponseDto(providerProfile, user), "Updated provider personal details", 200);
    }
}
