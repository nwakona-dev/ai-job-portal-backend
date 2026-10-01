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

        /*
         * Get the JWT from the original request
         * that came from the frontend.
         *
         * The frontend sends:
         *
         * Authorization: Bearer <JWT>
         *
         * We need to forward that same token
         * to the User Service.
         */
        String authorizationHeader =
                httpServletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        HttpHeaders headers =
                new HttpHeaders();

        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            headers.set(
                    HttpHeaders.AUTHORIZATION,
                    authorizationHeader
            );
        }

        /*
         * Create the request entity containing
         * the forwarded Authorization header.
         */
        HttpEntity<Void> requestEntity =
                new HttpEntity<>(headers);

        /*
         * Call the User Service.
         */
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