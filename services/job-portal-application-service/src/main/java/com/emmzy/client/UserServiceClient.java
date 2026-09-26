package com.emmzy.client;

import com.emmzy.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public UserResponse getUserById(Long userId) {

        String url = userServiceUrl + "/api/users/" + userId;

        return restTemplate.getForObject(
                url,
                UserResponse.class
        );
    }
}