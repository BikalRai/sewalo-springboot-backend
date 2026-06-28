package raicod3.example.com.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_categories")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobCategory extends AbstractBaseEntity{

    @Column(unique = true, nullable = false)
    private String name;
    private String description;
    private String iconUrl;
    private Integer baseTokenCost;
}
