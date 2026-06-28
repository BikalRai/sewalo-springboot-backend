package raicod3.example.com.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import raicod3.example.com.enums.Urgency;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class JobRequestDto {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Urgency is required")
    private Urgency urgency;

    private List<String> images;
}
