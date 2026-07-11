package raicod3.example.com.lib.khalti;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import java.util.Map;

@Slf4j
@Component
public class KhaltiClient {

    @Value("${khalti.base-url}")
    private String baseUrl;

    @Value("${khalti.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> initiatePayment(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + secretKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/epayment/initiate/",
                HttpMethod.POST,
                request,
                Map.class
        );

        return response.getBody();
    }

    public Map<String, Object> lookupPayment(String pidx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + secretKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("pidx", pidx), headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/epayment/lookup/",
                HttpMethod.POST,
                request,
                Map.class
        );

        return response.getBody();
    }
}
