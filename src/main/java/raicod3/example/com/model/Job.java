package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.enums.Urgency;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Job extends AbstractBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private  CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private JobCategory category;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bid> bids;

    @ElementCollection
    @CollectionTable(name = "job_images", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "image_url")
    private List<String> images;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;


    protected void onCreate()    {
        super.onCreate();
        this.expiresAt = LocalDateTime.now().plusHours(1);
        this.status = JobStatus.OPEN;
    }

    public Job(JobRequestDto dto, CustomerProfile customer, JobCategory category, UserAddress address) {
        this.customer = customer;
        this.category = category;
        this.title = dto.getTitle();
        this.urgency = dto.getUrgency();
        this.description = dto.getDescription();
        this.images = dto.getImages();
        this.address = address.getFormattedAddress();
        this.latitude = address.getLatitude();
        this.longitude = address.getLongitude();
    }
}
