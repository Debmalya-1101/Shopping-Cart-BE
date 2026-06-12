package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AddressDTO;
import com.demoproject.shoppingcart.service.PincodeValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class PincodeValidationServiceImpl implements PincodeValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PincodeValidationServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PincodeValidationServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SSLContext sslContext = null;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            logger.error("Failed to create trust-all SSL context", e);
        }

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1);
                
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        
        this.httpClient = builder.build();
    }

    @Override
    public void validateAndUpdatePincode(AddressDTO addressDTO) {
        String pincode = addressDTO.getPostalCode().trim();
        String url = "https://api.postalpincode.in/pincode/" + pincode;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "Mozilla/5.0")  // <-- add this
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.isArray() && rootNode.size() > 0) {
                    JsonNode resultNode = rootNode.get(0);
                    String status = resultNode.path("Status").asText();

                    if ("Success".equalsIgnoreCase(status)) {
                        JsonNode postOfficeArray = resultNode.path("PostOffice");
                        if (postOfficeArray.isArray() && postOfficeArray.size() > 0) {
                            JsonNode firstPostOffice = postOfficeArray.get(0);
                            String district = firstPostOffice.path("District").asText();
                            String state = firstPostOffice.path("State").asText();

                            // If user left city or state blank, auto-fill it.
                            if (addressDTO.getCity() == null || addressDTO.getCity().trim().isEmpty()) {
                                addressDTO.setCity(district);
                            }
                            if (addressDTO.getState() == null || addressDTO.getState().trim().isEmpty()) {
                                addressDTO.setState(state);
                            }
                            logger.info("Successfully validated pincode {} as {}, {}", pincode, district, state);
                            return;
                        }
                    } else {
                        // Genuine validation failure (invalid pincode according to post office database)
                        throw new RuntimeException("Invalid PIN code: " + pincode);
                    }
                }
            }

            logger.warn("Pincode API returned non-200 code or empty response. Falling back to manual check.");

        } catch (IOException | InterruptedException e) {
            logger.warn("Pincode Validation API is unreachable ({}). Proceeding with manual fallback if available.", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        // Graceful degradation fallback: If the service is unreachable (network error/timeout),
        // we allow saving ONLY if they've supplied City and State manually.
        if (addressDTO.getCity() != null && !addressDTO.getCity().trim().isEmpty() &&
                addressDTO.getState() != null && !addressDTO.getState().trim().isEmpty()) {
            logger.info("Validation service unreachable, but City/State are manually populated. Proceeding.");
        } else {
            throw new RuntimeException("Pincode validation API is currently unreachable. Please enter your City and State manually.");
        }
    }
}
