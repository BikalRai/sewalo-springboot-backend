package raicod3.example.com.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class JobRequestDto {
    @NotBlank(message = "Category ID is required")
    private String category; // This receives the UUID string from the frontend

    @NotBlank(message = "Urgency level is required")
    private String urgency;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    // WHY: We no longer accept List<MultipartFile>. The frontend already uploaded
    // the files to Cloudinary and is handing us the secure URLs.
    @Size(max = 3, message = "Maximum of 3 images allowed")
    private List<String> images;
}
