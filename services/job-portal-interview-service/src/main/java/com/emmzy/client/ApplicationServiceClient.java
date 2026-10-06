package com.emmzy.client;

import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.UpdateApplicationStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ApplicationServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;

    @Value("${application-service.url}")
    private String applicationServiceUrl;

//    Get an application by ID The JWT and X-User-Id from the original request
//     are forwarded to the Application Service.
    public ApplicationResponse getApplicationById(
            Long applicationId
    ) {

        String url =
                applicationServiceUrl
                        + "/api/applications/"
                        + applicationId;

        HttpHeaders headers = new HttpHeaders();

//        Get the JWT from the request that came into the Interview Service
        String authorizationHeader =
                httpServletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

//        Get the current user ID
        String userIdHeader =
                httpServletRequest.getHeader(
                        "X-User-Id"
                );


//        Forward JWT to Application Service
        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            headers.set(
                    HttpHeaders.AUTHORIZATION,
                    authorizationHeader
            );
        }

//        Forward X-User-Id to Application Service
        if (userIdHeader != null &&
                !userIdHeader.isBlank()) {

            headers.set(
                    "X-User-Id",
                    userIdHeader
            );
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

//        Debug information
        System.out.println("[ApplicationServiceClient] Calling Application Service");
        System.out.println("[ApplicationServiceClient] URL: " + url);
        System.out.println(
                "[ApplicationServiceClient] Authorization: " +
                        (
                                authorizationHeader != null
                                        ? "Present"
                                        : "Missing"
                        )
        );

        System.out.println("[ApplicationServiceClient] X-User-Id: " + userIdHeader);

        ResponseEntity<ApplicationResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        ApplicationResponse.class
                );

        return response.getBody();
    }


//    Update application status
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

//        Send employer ID
        headers.set(
                "X-User-Id",
                employerId.toString()
        );

//        Forward the original JWT
        String authorizationHeader =
                httpServletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            headers.set(
                    HttpHeaders.AUTHORIZATION,
                    authorizationHeader
            );
        }

        HttpEntity<UpdateApplicationStatusRequest> entity =
                new HttpEntity<>(
                        request,
                        headers
                );

//        Debug information
        System.out.println("[ApplicationServiceClient] Updating application status");
        System.out.println("[ApplicationServiceClient] URL: " + url);
        System.out.println("[ApplicationServiceClient] Status: " + status);
        System.out.println("[ApplicationServiceClient] Employer ID: " + employerId);
        System.out.println("[ApplicationServiceClient] Authorization: " +
                        (
                                authorizationHeader != null
                                        ? "Present"
                                        : "Missing"
                        )
        );

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