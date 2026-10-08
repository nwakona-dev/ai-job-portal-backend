package com.emmzy.client;

import com.emmzy.exception.ExternalServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final HttpServletRequest httpServletRequest;

    public UserServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${user-service.url}") String userServiceUrl,
            HttpServletRequest httpServletRequest
    ) {
        this.restClient = restClientBuilder
                .baseUrl(userServiceUrl)
                .build();

        this.httpServletRequest = httpServletRequest;
    }

    /**
     * Get a user by ID from the User Service.
     */
    public Map<String, Object> getUserById(Long userId) {

        try {

            String authorizationHeader =
                    httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

            System.out.println(
                    "[AI -> USER] Authorization header present: "
                            + (authorizationHeader != null)
            );

            Map<String, Object> user = restClient
                    .get()
                    .uri("/api/users/{userId}", userId)
                    .headers(headers -> {

                        if (authorizationHeader != null
                                && !authorizationHeader.isBlank()) {

                            headers.set(
                                    HttpHeaders.AUTHORIZATION,
                                    authorizationHeader
                            );
                        }
                    })
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {

                                throw new ExternalServiceException(
                                        "User Service returned HTTP "
                                                + response.getStatusCode().value()
                                                + " while fetching user "
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
                    "Unable to retrieve user "
                            + userId
                            + " from User Service",
                    exception
            );
        }
    }

    /**
     * Get a user profile from the User Service.
     */
    public Map<String, Object> getUserProfile(String userEmail) {

        try {

            String authorizationHeader =
                    httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

            System.out.println(
                    "[AI -> USER] Authorization header present: "
                            + (authorizationHeader != null)
            );

            Map<String, Object> profile = restClient
                    .get()
                    .uri("/api/users/profile")
                    .headers(headers -> {

                        if (authorizationHeader != null
                                && !authorizationHeader.isBlank()) {

                            headers.set(
                                    HttpHeaders.AUTHORIZATION,
                                    authorizationHeader
                            );
                        }

                        if (userEmail != null
                                && !userEmail.isBlank()) {

                            headers.set(
                                    "X-User-Email",
                                    userEmail
                            );
                        }
                    })
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {

                                throw new ExternalServiceException(
                                        "User Service returned HTTP "
                                                + response.getStatusCode().value()
                                                + " while fetching user profile"
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return profile != null ? profile : Map.of();

        } catch (ExternalServiceException exception) {

            throw exception;

        } catch (RestClientException exception) {

            throw new ExternalServiceException(
                    "Unable to retrieve user profile from User Service",
                    exception
            );
        }
    }
}
