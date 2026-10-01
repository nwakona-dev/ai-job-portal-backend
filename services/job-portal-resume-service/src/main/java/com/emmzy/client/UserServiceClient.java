package com.emmzy.client;

import com.emmzy.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public UserResponse getUserById(Long userId) {

        String url = userServiceUrl + "/api/users/" + userId;

        try {

            // Get the JWT sent from the frontend to Resume Service
            String authorizationHeader =
                    request.getHeader("Authorization");

            // Create headers for the request to User Service
            HttpHeaders headers = new HttpHeaders();

            if (authorizationHeader != null
                    && authorizationHeader.startsWith("Bearer ")) {

                headers.set("Authorization", authorizationHeader);
            }

            HttpEntity<Void> entity =
                    new HttpEntity<>(headers);

            // Forward the request to User Service with the JWT
            ResponseEntity<UserResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            UserResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {

            throw new RuntimeException(
                    "Unable to verify candidate with User Service",
                    e
            );
        }
    }
}

