package com.emmzy.client;

import com.emmzy.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.user.url}") String userServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(userServiceUrl)
                .build();
    }

    /**
     * Retrieves a user by ID.
     */
    public Map<String, Object> getUserById(Long userId) {
        try {
            Map<String, Object> user = restClient
                    .get()
                    .uri("/api/users/{userId}", userId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "User Service returned an error while fetching user "
                                                + userId
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return user != null ? user : Map.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve user " + userId,
                    exception
            );
        }
    }

    /**
     * Retrieves the profile associated with an email address.
     *
     * The User Service expects the user's email in X-User-Email.
     */
    public Map<String, Object> getUserProfile(String userEmail) {
        try {
            Map<String, Object> profile = restClient
                    .get()
                    .uri("/api/users/profile")
                    .header("X-User-Email", userEmail)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "User Service returned an error "
                                                + "while fetching user profile"
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return profile != null ? profile : Map.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve user profile",
                    exception
            );
        }
    }
}