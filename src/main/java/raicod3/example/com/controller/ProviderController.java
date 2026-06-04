package raicod3.example.com.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raicod3.example.com.dto.provider.OnboardingProviderRequestDto;
import raicod3.example.com.service.ProviderService;
import raicod3.example.com.utilities.APIResponse;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')") // locks out customer
public class ProviderController {

    private final ProviderService providerService;

    // patch personal details
    @PatchMapping("/update-personal")
    public ResponseEntity<APIResponse> updatePersonal(@RequestBody OnboardingProviderRequestDto request, Principal principal) {
        APIResponse res = providerService.updateProviderProfile(request, principal.getName());

        return ResponseEntity.ok(res);
    }
}
