package com.emmzy.client;

import com.emmzy.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public UserResponse getUserById(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }

        String url =
                userServiceUrl + "/api/users/" + userId;

        try {

            return restTemplate.getForObject(
                    url,
                    UserResponse.class
            );

        } catch (RestClientException ex) {

            throw new IllegalArgumentException(
                    "Unable to retrieve user from User Service",
                    ex
            );
        }
    }
}