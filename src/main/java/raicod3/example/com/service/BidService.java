package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.bid.BidRequestDto;
import raicod3.example.com.dto.bid.BidResponseDto;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.Bid;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {
    private final BidRepository bidRepository;
    private final JobRepository jobRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final JobUnlockRepository jobUnlockRepository;

    // Provider place a bid
    @Transactional
    @Auditable(action = "PROVIDER_PLACE_BID")
    public BidResponseDto placeBid(UUID userId, UUID jobId, BidRequestDto dto) {
        // Get provider and run safety gate
        ProviderProfile provider = providerProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        if(!provider.getIsVerified()) {
            throw new UnauthorizedException("Your account is pending verification. You cannot bid yet.");
        }

        if(!provider.getIsActive()) {
            throw  new UnauthorizedException("Your account is suspended.");
        }

        // Get job and validate biddable
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if(job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job is no longer accepting bids.");
        }

        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This job has expired");
        }

        // Provider cannot bid on their own
        boolean isOwnJob = job.getCustomer().getUser().getId().equals(userId);
        if (isOwnJob) {
            throw new BadRequestException("You cannot bid on your own job");
        }

        // Check for duplicate bid
        if (bidRepository.existsByJobIdAndProviderId(jobId, provider.getId())) {
            throw new BadRequestException("You have already placed a bid on this job");
        }

        // Enforce max 3 active bids
        int activeBidCount = bidRepository.countActiveBids(jobId);
        if (activeBidCount >= 3) {
            throw new BadRequestException(
                    "This job already has the maximum number of bids");
        }

        // Save bid
        Bid bid = new Bid();
        bid.setJob(job);
        bid.setProvider(provider);
        bid.setMessage(dto.getMessage());
        bid.setQuotedPrice(dto.getQuotedPrice());
        bid.setPricingBasis(dto.getPricingBasis());

        return toDto(bidRepository.save(bid), false);
    }

    // Customer get bids for job
    public List<BidResponseDto> getBidsForJob(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Only the job owner can see bids
        if (!job.getCustomer().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this job's bids");
        }

        return bidRepository.findByJobIdOrderByCreatedAtAsc(jobId)
                .stream()
                .map(bid -> toDto(bid, false)) // customer sees provider info, not phone
                .toList();
    }

    // Customer accept bid
    @Transactional
    @Auditable(action = "CUSTOMER_ACCEPT_BID")
    public BidResponseDto acceptBid(UUID userId, UUID jobId, UUID bidId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getCustomer().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't own this job");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job is not open for acceptance");
        }

        Bid winningBid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        if (!winningBid.getJob().getId().equals(jobId)) {
            throw new BadRequestException("Bid does not belong to this job");
        }

        // Accept winning bid
        winningBid.setStatus(BidStatus.ACCEPTED);

        // Reject all other bids on this job
        bidRepository.findByJobIdAndStatusNot(jobId, BidStatus.WITHDRAWN)
                .stream()
                .filter(b -> !b.getId().equals(bidId))
                .forEach(b -> b.setStatus(BidStatus.REJECTED));

        // Move job to IN_PROGRESS
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        return toDto(bidRepository.save(winningBid), false);
    }

    // Provider withdraw their bid
    @Transactional
    @Auditable(action = "PROVIDER_WITHDRAW_BID")
    public BidResponseDto withdrawBid(UUID userId, UUID bidId) {
        ProviderProfile provider = providerProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        Bid bid = bidRepository.findByIdAndProviderId(bidId, provider.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new BadRequestException("Only pending bids can be withdrawn");
        }

        bid.setStatus(BidStatus.WITHDRAWN);
        return toDto(bidRepository.save(bid), false);
    }

//    Provider view their own bids
public List<BidResponseDto> getMyBids(UUID userId) {
    ProviderProfile provider = providerProfileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

    return bidRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId())
            .stream()
            .map(bid -> toDto(bid, false))
            .toList();
}


    private BidResponseDto toDto(Bid bid, boolean includePhone) {
        User providerUser = bid.getProvider().getUser();

        return BidResponseDto.builder()
                .id(bid.getId())
                .jobId(bid.getJob().getId())
                .providerId(bid.getProvider().getId())
                .providerName(providerUser.getFullName())
                .providerImageUrl(providerUser.getImageUrl())
                .providerBio(bid.getProvider().getBio())
                .providerStartingRate(bid.getProvider().getStartingRate())
                .message(bid.getMessage())
                .quotedPrice(bid.getQuotedPrice())
                .pricingBasis(bid.getPricingBasis())
                .status(bid.getStatus())
                .contactUnlocked(bid.getContactUnlocked())
                .providerPhone(includePhone ? providerUser.getPhoneNumber() : null)
                .createdAt(bid.getCreatedAt())
                .build();
    }
}
