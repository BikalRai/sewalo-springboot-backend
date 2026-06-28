package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.model.Job;
import raicod3.example.com.service.JobService;
import raicod3.example.com.utilities.APIResponse;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> postJob(@AuthenticationPrincipal CustomUserDetails principal, @Valid  @RequestBody JobRequestDto dto) {
        JobResponseDto result = jobService.postJob(principal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success(result, "Job posted successfully", Http_Constants.CREATED));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> getMyJobs(@AuthenticationPrincipal CustomUserDetails principal) {
        List<JobResponseDto> result = jobService.getJobs(principal.getId());
        return ResponseEntity.ok(APIResponse.success(result, "Jobs retrieved successfully", Http_Constants.OK));
    }

    @PatchMapping("/{jobId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> cancelJob(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable("jobId") UUID jobId) {
        JobResponseDto result = jobService.cancelJob(principal.getId(), jobId);

        return ResponseEntity.ok(APIResponse.success(result, "Job cancelled successfully", Http_Constants.OK));
    }

    @GetMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> listOpenJobs(@AuthenticationPrincipal CustomUserDetails principal) {
        List<JobResponseDto> result = jobService.getOpenJobs(principal.getId());

        return ResponseEntity.ok(APIResponse.success(result, "Jobs retrieved successfully", Http_Constants.OK));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('Provider')")
    public ResponseEntity<APIResponse> getJobById(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("jobId") UUID jobId) {
        JobResponseDto result = jobService.getJob(principal.getId(), jobId);

        return ResponseEntity.ok(APIResponse.success(result, "Job found successfully", Http_Constants.OK));
    }
}
