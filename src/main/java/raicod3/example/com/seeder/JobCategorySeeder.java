package raicod3.example.com.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.model.JobCategory;
import raicod3.example.com.repository.JobCategoryRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCategorySeeder implements CommandLineRunner {

    private final JobCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Idempotency Check: Prevent duplicate inserts on every server restart
        if (categoryRepository.count() > 0) {
            log.info("Job Categories already seeded. Skipping...");
            return;
        }

        log.info("Seeding Job Categories...");

        // 2. Define the categories mapping exactly to your React frontend
        List<JobCategory> categories = List.of(
                createCategory("Plumbing", "Pipes, leaks, and water systems", "LuDroplets", 10),
                createCategory("Electrical", "Wiring, fixtures, and power issues", "LuZap", 15),
                createCategory("Painting", "Interior and exterior painting", "LuPaintRoller", 8),
                createCategory("Carpentry", "Woodwork and furniture repair", "LuHammer", 12),
                createCategory("Cleaning", "Deep cleaning and maintenance", "LuSparkles", 5),
                createCategory("Appliance Repair", "Fixing household machines", "LuWrench", 15)
        );

        // 3. Batch save for optimal performance
        categoryRepository.saveAll(categories);

        log.info("Successfully seeded {} Job Categories.", categories.size());
    }

    private JobCategory createCategory(String name, String description, String iconUrl, Integer baseTokenCost) {
        JobCategory category = new JobCategory();
        category.setName(name);
        category.setDescription(description);

        // Storing the exact React Icon name allows your frontend to map it dynamically later
        category.setIconUrl(iconUrl);
        category.setBaseTokenCost(baseTokenCost);
        return category;
    }
}
