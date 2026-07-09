package raicod3.example.com.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raicod3.example.com.dto.category.JobCategoryResponseDto;
import raicod3.example.com.model.JobCategory;
import raicod3.example.com.service.JobCategoryService;
import raicod3.example.com.utilities.APIResponse;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;

    @GetMapping
    public ResponseEntity<APIResponse> findAll() {
        List<JobCategoryResponseDto> categories = jobCategoryService.findAll();

        return ResponseEntity.ok(APIResponse.success(categories, "List of categories", HttpStatus.OK.value()));
    }
}
