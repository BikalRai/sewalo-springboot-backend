package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.*;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.repository.*;
import raicod3.example.com.utilities.LocationUtils;

import java.util.Collections;
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
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    // Customer post a job
    @Transactional
    @Auditable(action = "CUSTOMER_POST_JOB")
    public JobResponseDto postJob(UUID userId, JobRequestDto dto) {

        // 1. Get customer profile
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // 2. Get category
        JobCategory category = jobCategoryRepository.findById(UUID.fromString(dto.getCategory()))
                .orElseThrow(() -> new ResourceNotFoundException("Job category not found"));

        // 3. Build Job (This natively pulls latitude, longitude, and address from the DTO)
        Job job = new Job(dto, customer, category);

        // 4. Handle Status
        List<String> images = dto.getImages();
        if (images != null && !images.isEmpty()) {
            job.setStatus(JobStatus.ANALYZING);
        } else {
            job.setStatus(JobStatus.OPEN);
        }

        // 5. Save Job (Location is now safely stored in the `jobs` table)
        Job savedJob = jobRepository.saveAndFlush(job);

        // 6. Publish to RabbitMQ safely AFTER commit
        if (images != null && !images.isEmpty()) {
            JobAnalysisEvent event = new JobAnalysisEvent(savedJob.getId(), customer.getUser().getId().toString(), images);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.JOB_ANALYSIS_ROUTING_KEY, event);
                    log.info("Published JobAnalysisEvent to RabbitMQ for Job ID: {}", savedJob.getId());
                }
            });
        }

        return toDto(savedJob, true, true);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getJobs(UUID userId) {
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return jobRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream().map(job -> toDto(job, true, true)).toList();
    }

    @Transactional
    @Auditable(action = "CUSTOMER_CANCEL_JOB")
    public JobResponseDto cancelJob(UUID userId, UUID jobId) {
        Job job = getJobAndValidateOwner(userId, jobId);

        if(job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("Only OPEN jobs can be cancelled");
        }

        job.setStatus(JobStatus.CANCELLED);
        return toDto(jobRepository.save(job), true, true);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getOpenJobs(UUID userId) {
        // 1. Fetch the provider profile
        ProviderProfile provider = providerRepository.findByUserId(userId);

        if (provider == null || provider.getServices() == null || provider.getServices().isEmpty()) {
            return Collections.emptyList();
        }

        // --- Extract Provider's Base Coordinates ---
        UserAddress providerAddress = provider.getUser().getUserAddress();
        if (providerAddress == null) {
            // Defensive check: If provider bypassed onboarding somehow, return jobs without distance
            return jobRepository.findByStatusAndCategoryNameInOrderByCreatedAtDesc(JobStatus.OPEN, provider.getServices())
                    .stream().map(job -> toDto(job, false, false)).toList();
        }

        Double providerLat = providerAddress.getLatitude();
        Double providerLon = providerAddress.getLongitude();

        // 2. Fetch matched jobs
        List<Job> matchedJobs = jobRepository.findByStatusAndCategoryNameInOrderByCreatedAtDesc(
                JobStatus.OPEN,
                provider.getServices()
        );

        // 3. Map to DTO and calculate distance dynamically against the JOB's coordinates
        return matchedJobs.stream()
                .map(job -> toDtoWithDistance(job, false, false, providerLat, providerLon))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJob(UUID jobId, UUID userId) { // Assuming controller passes the authenticated User ID
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // 1. Get the provider profile using the User ID
        ProviderProfile provider = providerRepository.findByUserId(userId);
        if (provider == null) {
            throw new UnauthorizedException("Provider profile not found.");
        }

        // 2. Security Checks (using the actual provider.getId())
        boolean isUnlocked = jobUnlockRepository.existsByJob_IdAndProvider_Id(jobId, provider.getId());
        boolean hasWonBid = bidService.isBidAccepted(jobId, provider.getId());

        // 3. Distance Calculation Setup
        UserAddress providerAddress = provider.getUser().getUserAddress();

        if (providerAddress == null) {
            // Fallback: If they somehow have no address, return without distance
            return toDto(job, isUnlocked, hasWonBid);
        }

        // 4. Return with full masking logic AND distance
        return toDtoWithDistance(
                job,
                isUnlocked,
                hasWonBid,
                providerAddress.getLatitude(),
                providerAddress.getLongitude()
        );
    }

    @Transactional(readOnly = true)
    public JobResponseDto getMyJob(UUID userId, UUID jobId) {
        Job job = getJobAndValidateOwner(userId, jobId);
        return toDto(job, true, true); // customer always sees their own full address
    }

    private Job getJobAndValidateOwner(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        boolean isOwner = job.getCustomer().getUser().getId().equals(userId);
        if(!isOwner) {
            throw new UnauthorizedException("You are not allowed to perform this action");
        }
        return job;
    }

    private JobResponseDto toDto(Job job, boolean showFullAddress, boolean showContact) {
        return JobResponseDto.builder()
                .id(job.getId())
                .description(job.getDescription())
                .categoryName(job.getCategory().getName())
                .categoryIcon(job.getCategory().getIconUrl())
                .urgency(job.getUrgency())
                .status(job.getStatus())
                .difficulty(job.getDifficulty())
                .images(job.getImages() != null ? new java.util.ArrayList<>(job.getImages()) : new java.util.ArrayList<>())
                .latitude(job.getLatitude())
                .longitude(job.getLongitude())

                // --- THE GATEKEEPER LOGIC ---
                // If true, show the real address. If false, show the masked area (e.g., "New Baneshwor").
                .address(showFullAddress ? job.getAddress() : job.getMaskedAddress())

                // If true, show the real number. If false, send null so the UI can't render it.
                .contactNumber(showContact ? job.getContactNumber() : null)

                .customerName(job.getCustomer().getUser().getFullName())
                .customerImageUrl(job.getCustomer().getUser().getImageUrl())
                .bidCount(job.getBids() != null ? job.getBids().size() : 0)
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }

    private JobResponseDto toDtoWithDistance(Job job, boolean includeAddress, boolean includeContact, Double providerLat, Double providerLon) {
        JobResponseDto dto = toDto(job, includeAddress, includeContact);

        // Calculate distance: Provider Base (UserAddress) -> Job Location (Job entity)
        dto.setDistance(LocationUtils.calculateDistance(
                providerLat, providerLon,
                job.getLatitude(), job.getLongitude()
        ));

        return dto;
    }
}
