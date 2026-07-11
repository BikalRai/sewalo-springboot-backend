package raicod3.example.com.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.enums.PurchaseStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.lib.khalti.KhaltiClient;
import raicod3.example.com.model.CreditPurchase;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.CreditPurchaseRepository;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CreditPurchaseService {

    private static  final int PRICE_PER_CREDIT_PAISA = 4500; // NPR 45 per credit

    private final KhaltiClient khaltiClient;
    private final CreditPurchaseRepository creditPurchaseRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderCreditsRepository providerCreditsRepository;

    @Transactional
    public Map<String, Object> initiatePurchase(UUID providerId, int creditsRequested) {
        ProviderProfile providerProfile = providerProfileRepository.findById(providerId).orElseThrow(() -> new ResourceNotFoundException("Provider Profile Not Found"));

        int amountPaisa = creditsRequested * PRICE_PER_CREDIT_PAISA;

        Map<String, Object> payload = Map.of(
                "return_url", "http://localhost:5173/payment/verify",
                "website_url", "http://localhost:5173",
                "amount", amountPaisa,
                "purchase_order_id", "SEWALO-" + UUID.randomUUID(),
                "purchase_order_name", creditsRequested + " Sewalo Credits",
                "customer_info", Map.of(
                        "name", providerProfile.getUser().getFullName(),
                        "email", providerProfile.getUser().getEmail(),
                        "phone", providerProfile.getUser().getPhoneNumber() != null ? providerProfile.getUser().getPhoneNumber() : "9800000000"
                )
        );

        Map<String, Object> khaltiResponse = khaltiClient.initiatePayment(payload);

        String pidx = (String) khaltiResponse.get("pidx");
        String paymentUrl = (String) khaltiResponse.get("payment_url");

        CreditPurchase  purchase = new CreditPurchase();
        purchase.setProvider(providerProfile);
        purchase.setPidx(pidx);
        purchase.setCreditsRequested(creditsRequested);
        purchase.setAmountPaisa(amountPaisa);
        purchase.setStatus(PurchaseStatus.PENDING);
        creditPurchaseRepository.save(purchase);

        return Map.of("payment_url", paymentUrl, "pidx", pidx);
    }

    @Transactional
    public String verifyPurchase(String pidx) {
        CreditPurchase purchase = creditPurchaseRepository.findByPidx(pidx).orElseThrow(() -> new ResourceNotFoundException("Credit Purchase record Not Found"));

        if(purchase.getStatus().equals(PurchaseStatus.COMPLETED)) {
            return "Already processed";
        }

        Map<String, Object> lookupResult = khaltiClient.lookupPayment(pidx);
        String status = (String) lookupResult.get("status");

        if("Completed".equals(status)) {
            purchase.setStatus(PurchaseStatus.COMPLETED);
            creditPurchaseRepository.save(purchase);

            providerCreditsRepository.addCredits(purchase.getProvider().getId(), purchase.getCreditsRequested());

            return "Payment verified. Credits added.";
        } else {
            purchase.setStatus(PurchaseStatus.FAILED);
            creditPurchaseRepository.save(purchase);
            throw new BadRequestException("Payment not completed. Status: " + status);
        }
    }
}
