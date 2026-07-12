package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.CustomerProfile;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobCategory;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.repository.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final CustomerRepository customerRepository;
    private final JobUnlockRepository jobUnlockRepository;
    private final RabbitTemplate rabbitTemplate;
    private final BidService bidService;
    private final ProviderCreditsRepository providerCreditsRepository;

    // Customer post a job
    @Transactional
    @Auditable(action = "CUSTOMER_POST_JOB")
    public JobResponseDto postJob(UUID userId, JobRequestDto dto) {

        // 1. Get customer profile
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // 2. Get category (Extracting the UUID string sent from the frontend)
        JobCategory category = jobCategoryRepository.findById(UUID.fromString(dto.getCategory()))
                .orElseThrow(() -> new ResourceNotFoundException("Job category not found"));

        // 3. Build Job using the clean constructor
        Job job = new Job(dto, customer, category);

        // 4. Handle Status based on Image presence
        List<String> images = dto.getImages();
        if (images != null && !images.isEmpty()) {
            job.setStatus(JobStatus.ANALYZING);
        } else {
            job.setStatus(JobStatus.OPEN);
        }

        // 5. Save Job
        Job savedJob = jobRepository.saveAndFlush(job);

        // 6. Publish to RabbitMQ
        if (images != null && !images.isEmpty()) {
            // WHY: We pass the pre-uploaded string URLs directly from the DTO to the event
            JobAnalysisEvent event = new JobAnalysisEvent(savedJob.getId(), customer.getUser().getId().toString(), images);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.JOB_ANALYSIS_ROUTING_KEY, event);
            log.info("Published JobAnalysisEvent to RabbitMQ for Job ID: {}", savedJob.getId());
        }

        return toDto(savedJob, true);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getJobs(UUID userId) {
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return jobRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream().map(job -> toDto(job, true)).toList();
    }

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

    @Transactional(readOnly = true)
    public List<JobResponseDto> getOpenJobs(UUID providerId) {
        return jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.OPEN)
                .stream().map(job -> toDto(job, false)).toList();
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJob(UUID jobId, UUID providerId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        boolean isUnlocked = jobUnlockRepository.existsByJob_IdAndProvider_Id(jobId, providerId);
        return toDto(job, isUnlocked);
    }

    @Transactional(readOnly = true)
    public JobResponseDto getMyJob(UUID userId, UUID jobId) {
        Job job = getJobAndValidateOwner(userId, jobId);
        return toDto(job, true); // customer always sees their own full address
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
                // WHY: Removed .title() mapping since the field no longer exists
                .description(job.getDescription())
                .categoryName(job.getCategory().getName())
                .categoryIcon(job.getCategory().getIconUrl())
                .urgency(job.getUrgency())
                .status(job.getStatus())
                .difficulty(job.getDifficulty())
                .images(job.getImages() != null ? new java.util.ArrayList<>(job.getImages()) : new java.util.ArrayList<>())
                .latitude(job.getLatitude())
                .longitude(job.getLongitude())
                .address(includeAddress ? job.getAddress() : null)
                .customerName(job.getCustomer().getUser().getFullName())
                .contactNumber(job.getContactNumber())
                .customerImageUrl(job.getCustomer().getUser().getImageUrl())
                .bidCount(job.getBids() != null ? job.getBids().size() : 0)
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }
}
