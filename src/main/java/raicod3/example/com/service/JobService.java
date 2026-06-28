package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.CustomerProfile;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobCategory;
import raicod3.example.com.model.UserAddress;
import raicod3.example.com.repository.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final CustomerRepository customerRepository;
    private final UserAddressRepository userAddressRepository;
    private final JobUnlockRepository jobUnlockRepository;


    // Customer post a job
    @Transactional
    @Auditable(action = "CUSTOMER_POST_JOB")
    public JobResponseDto postJob(UUID userId, JobRequestDto dto) {

        // Get customer profile
        CustomerProfile customer = customerRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // Get address of customer
        UserAddress address = userAddressRepository.findByUserId(userId).orElseThrow(() -> new BadRequestException("Please set your address before posting a job"));

        // Get category
        JobCategory category = jobCategoryRepository.findById(dto.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Job category not found"));

        // Build job
        Job job = new Job(dto, customer, category, address);

        Job savedJob = jobRepository.save(job);

        return toDto(savedJob, false);
    }

    // Customer get their own jobs
    public List<JobResponseDto> getJobs(UUID userId) {
        CustomerProfile customer = customerRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return jobRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId()).stream().map(job -> toDto(job, true)).toList();
    }

    // Customer cancel job
    @Transactional
    @Auditable(action = "CUSTOMER_CANCEL_JOB")
    public JobResponseDto cancelJob(UUID userId, UUID jobId) {
        Job job = getJobAndValidateOwner(userId, jobId);

        if(job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("Only OPEN jobs can be cancelled");
        }

        job.setStatus(JobStatus.CANCELLED);
        return toDto(jobRepository.save(job), true);
    }

    // Provider browse open jobs
    public List<JobResponseDto> getOpenJobs(UUID providerId) {
        return jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.OPEN).stream().map(job -> toDto(job, false)).toList();
    }

    // Provider get single job (address revealed only if unlocked)
    public JobResponseDto getJob(UUID jobId, UUID providerId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        boolean isUnlocked = jobUnlockRepository.existsByJobIdAndProviderId(jobId, providerId);

        return toDto(job, isUnlocked);
    }

    private Job getJobAndValidateOwner(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        boolean isOwner = job.getCustomer().getUser().getId().equals(userId);

        if(!isOwner) {
            throw new UnauthorizedException("You are not allowed to perform this action");
        }

        return job;
    }

    private JobResponseDto toDto(Job job, boolean includeAddress) {
        return JobResponseDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .categoryName(job.getCategory().getName())
                .categoryIcon(job.getCategory().getIconUrl())
                .urgency(job.getUrgency())
                .status(job.getStatus())
                .images(job.getImages())
                .latitude(job.getLatitude())
                .longitude(job.getLongitude())
                .address(includeAddress ? job.getAddress() : null)
                .customerName(job.getCustomer().getUser().getFullName())
                .customerImageUrl(job.getCustomer().getUser().getImageUrl())
                .bidCount(job.getBids() !=null ? job.getBids().size() : 0)
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }
}
