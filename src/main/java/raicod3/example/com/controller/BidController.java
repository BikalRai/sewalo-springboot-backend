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
import raicod3.example.com.dto.bid.BidConfirmationDto;
import raicod3.example.com.dto.bid.BidRequestDto;
import raicod3.example.com.dto.bid.BidSummaryDto;
import raicod3.example.com.service.BidService;
import raicod3.example.com.utilities.APIResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BidController {
    private final BidService bidService;

    // PROVIDER — place a bid on a job
    @PostMapping("/jobs/{jobId}/bids")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> placeBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID jobId,
            @Valid @RequestBody BidRequestDto dto
    ) {
        BidConfirmationDto result = bidService.placeBid(userDetails.getId(), jobId, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(APIResponse.success(result, "Successfully placed bid", Http_Constants.CREATED));
    }

    // CUSTOMER — view all bids on their job
    @GetMapping("/jobs/{jobId}/bids")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> getBidsForJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID jobId
    ) {
        List<BidSummaryDto> result = bidService.getBidsForJob(userDetails.getId(), jobId);
        return ResponseEntity.ok(APIResponse.success(result, "List of bids", Http_Constants.OK));
    }

    // CUSTOMER — accept a specific bid
    @PatchMapping("/jobs/{jobId}/bids/{bidId}/accept")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> acceptBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID jobId,
            @PathVariable UUID bidId
    ) {
        BidSummaryDto result = bidService.acceptBid(userDetails.getId(), jobId, bidId);
        return ResponseEntity.ok(APIResponse.success(result, "Accepted bid successfully", Http_Constants.OK));
    }

    // PROVIDER — withdraw their own bid
    @PatchMapping("/bids/{bidId}/withdraw")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> withdrawBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID bidId
    ) {
        BidConfirmationDto result = bidService.withdrawBid(userDetails.getId(), bidId);
        return ResponseEntity.ok(APIResponse.success(result, "Withdrawal successfully", Http_Constants.OK));
    }

    // PROVIDER — view all their own bids
    @GetMapping("/bids/my")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> getMyBids(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<BidConfirmationDto> result = bidService.getMyBids(userDetails.getId());
        return ResponseEntity.ok(APIResponse.success(result, "My bids", Http_Constants.OK));
    }
}
