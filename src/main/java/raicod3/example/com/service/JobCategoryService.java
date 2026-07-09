package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import raicod3.example.com.dto.category.JobCategoryResponseDto;
import raicod3.example.com.model.JobCategory;
import raicod3.example.com.repository.JobCategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobCategoryService {
    private final JobCategoryRepository jobCategoryRepository;

    public List<JobCategoryResponseDto> findAll() {
        return jobCategoryRepository.findAll().stream().map(JobCategoryResponseDto::new).toList();
    }

}
