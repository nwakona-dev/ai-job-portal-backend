package com.emmzy.client;

import com.emmzy.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class ApplicationServiceClient {

    private final RestClient restClient;

    public ApplicationServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.application.url}") String applicationServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(applicationServiceUrl)
                .build();
    }

    /**
     * Retrieves all applications.
     */
    public List<Map<String, Object>> getApplications() {
        try {
            List<Map<String, Object>> applications = restClient
                    .get()
                    .uri("/api/applications")
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Application Service returned an error "
                                                + "while fetching applications"
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return applications != null ? applications : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to communicate with Application Service",
                    exception
            );
        }
    }

    /**
     * Retrieves a specific application by ID.
     */
    public Map<String, Object> getApplicationById(Long applicationId) {
        try {
            Map<String, Object> application = restClient
                    .get()
                    .uri("/api/applications/{applicationId}", applicationId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Application Service returned an error while fetching application "
                                                + applicationId
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return application != null ? application : Map.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve application " + applicationId,
                    exception
            );
        }
    }

    /**
     * Retrieves applications belonging to the current candidate.
     */
    public List<Map<String, Object>> getMyApplications() {
        try {
            List<Map<String, Object>> applications = restClient
                    .get()
                    .uri("/api/applications/my")
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Application Service returned an error "
                                                + "while fetching candidate applications"
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return applications != null ? applications : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve candidate applications",
                    exception
            );
        }
    }

    /**
     * Retrieves applications submitted to the current company's jobs.
     */
    public List<Map<String, Object>> getCompanyApplications() {
        try {
            List<Map<String, Object>> applications = restClient
                    .get()
                    .uri("/api/applications/company")
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Application Service returned an error "
                                                + "while fetching company applications"
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return applications != null ? applications : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve company applications",
                    exception
            );
        }
    }
}