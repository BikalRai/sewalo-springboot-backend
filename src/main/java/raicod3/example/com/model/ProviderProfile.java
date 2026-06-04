package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.*;
import raicod3.example.com.dto.provider.OnboardingProviderRequestDto;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "providers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "user")
public class ProviderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String gender;

    private String experience;

    @ElementCollection
    @CollectionTable(name = "provider_services", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "service")
    private List<String> services;

    @ElementCollection
    @CollectionTable(name = "provider_work_areas", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "district")
    private List<String> workDistrict;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String pricingBasis;

    private String startingRate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;


    public void updateFromDto(OnboardingProviderRequestDto req) {
        this.gender = req.getGender();
        this.experience = req.getExperience();
        this.services = req.getServices();
        this.workDistrict = req.getWorkArea();
        this.bio = req.getBio();
        this.pricingBasis = req.getPricingBasis();
        this.startingRate = req.getStartingRate();
    }
}
