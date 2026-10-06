package com.emmzy.client;

import com.emmzy.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final HttpServletRequest httpServletRequest;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public UserResponse getUserById(Long userId) {

        String url =
                userServiceUrl +
                        "/api/users/" +
                        userId;

//        Get the JWT from the original request
        String authorizationHeader =
                httpServletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

//        Get the current user ID as well
        String userIdHeader =
                httpServletRequest.getHeader(
                        "X-User-Id"
                );

        HttpHeaders headers =
                new HttpHeaders();

        /*
         * Forward Authorization header.
         */
        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            headers.set(
                    HttpHeaders.AUTHORIZATION,
                    authorizationHeader
            );
        }

//        Forward X-User-Id header
        if (userIdHeader != null &&
                !userIdHeader.isBlank()) {

            headers.set(
                    "X-User-Id",
                    userIdHeader
            );
        }

//        Create request entity
        HttpEntity<Void> requestEntity =
                new HttpEntity<>(headers);

        System.out.println("[UserServiceClient] Calling User Service");
        System.out.println("[UserServiceClient] URL: " + url);
        System.out.println("[UserServiceClient] Authorization: " +
                        (
                                authorizationHeader != null
                                        ? "Present"
                                        : "Missing"
                        )
        );

        System.out.println("[UserServiceClient] X-User-Id: " + userIdHeader);

//        Call User Service
        ResponseEntity<UserResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        UserResponse.class
                );

        return response.getBody();
    }
}

