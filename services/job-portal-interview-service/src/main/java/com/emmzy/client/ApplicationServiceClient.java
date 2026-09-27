package com.emmzy.client;

import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.UpdateApplicationStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ApplicationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${application-service.url}")
    private String applicationServiceUrl;

    public ApplicationResponse getApplicationById(
            Long applicationId
    ) {

        String url =
                applicationServiceUrl
                        + "/api/applications/"
                        + applicationId;

        ResponseEntity<ApplicationResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        ApplicationResponse.class
                );

        return response.getBody();
    }

    public ApplicationResponse updateApplicationStatus(
            Long applicationId,
            Long employerId,
            String status
    ) {

        String url =
                applicationServiceUrl
                        + "/api/applications/"
                        + applicationId
                        + "/status";

        UpdateApplicationStatusRequest request =
                UpdateApplicationStatusRequest.builder()
                        .status(status)
                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", employerId.toString());

        HttpEntity<UpdateApplicationStatusRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<ApplicationResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.PATCH,
                        entity,
                        ApplicationResponse.class
                );

        return response.getBody();
    }
}