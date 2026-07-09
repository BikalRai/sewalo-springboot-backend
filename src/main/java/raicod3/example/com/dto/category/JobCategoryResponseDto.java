package raicod3.example.com.dto.category;

import lombok.Getter;
import lombok.Setter;
import raicod3.example.com.model.JobCategory;

import java.util.UUID;

@Getter
@Setter
public class JobCategoryResponseDto {
    UUID id;
    String name;
    String iconUrl;

    public JobCategoryResponseDto (JobCategory category){
        this.id = category.getId();
        this.name = category.getName();
        this.iconUrl = category.getIconUrl();
    }
}
