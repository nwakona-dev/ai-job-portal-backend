package com.emmzy.client;

import com.emmzy.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL =
            "http://localhost:5001";

    public UserResponse getUserById(Long userId) {

        System.out.println(
                "[UserServiceClient] Getting user by ID: " + userId
        );

        String url = USER_SERVICE_URL + "/api/users/" + userId;

        System.out.println(
                "[UserServiceClient] URL: " + url
        );

        HttpHeaders headers = new HttpHeaders();

        // Forward the Authorization header from the original request
        String authorization =
                org.springframework.web.context.request.RequestContextHolder
                        .getRequestAttributes() instanceof
                        org.springframework.web.context.request.ServletRequestAttributes attributes
                        ? attributes.getRequest().getHeader("Authorization")
                        : null;

        if (authorization != null && !authorization.isBlank()) {
            headers.set("Authorization", authorization);

            System.out.println(
                    "[UserServiceClient] Authorization header forwarded"
            );
        } else {
            System.out.println(
                    "[UserServiceClient] WARNING: No Authorization header found"
            );
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<UserResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            UserResponse.class
                    );

            System.out.println(
                    "[UserServiceClient] Response status: "
                            + response.getStatusCode()
            );

            if (response.getBody() != null) {

                System.out.println(
                        "[UserServiceClient] User found: "
                                + response.getBody().getFullName()
                );

            }

            return response.getBody();

        } catch (Exception e) {

            System.out.println(
                    "[UserServiceClient] Failed to get user: "
                            + e.getMessage()
            );

            return null;
        }
    }
}