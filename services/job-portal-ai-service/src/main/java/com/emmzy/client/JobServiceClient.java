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
public class JobServiceClient {

    private final RestClient restClient;

    public JobServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${job-service.url}") String jobServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(jobServiceUrl)
                .build();
    }

    /**
     * Retrieves all jobs from the Job Service.
     */
    public List<Map<String, Object>> getJobs() {
        try {
            List<Map<String, Object>> jobs = restClient
                    .get()
                    .uri("/api/jobs")
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                String responseBody = response.getBody()
                                        .toString();

                                throw new ExternalServiceException(
                                        "Job Service returned HTTP "
                                                + response.getStatusCode().value()
                                                + " while fetching jobs. Response: "
                                                + responseBody
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return jobs != null ? jobs : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to communicate with Job Service at "
                            + "http://localhost:5003/api/jobs. "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * Retrieves a specific job by ID.
     */
    public Map<String, Object> getJobById(Long jobId) {
        try {
            Map<String, Object> job = restClient
                    .get()
                    .uri("/api/jobs/{jobId}", jobId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                String responseBody = response.getBody()
                                        .toString();

                                throw new ExternalServiceException(
                                        "Job Service returned HTTP "
                                                + response.getStatusCode().value()
                                                + " while fetching job "
                                                + jobId
                                                + ". Response: "
                                                + responseBody
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return job != null ? job : Map.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve job "
                            + jobId
                            + " from Job Service. "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * Retrieves all jobs belonging to a specific employer.
     */
    public List<Map<String, Object>> getJobsByEmployer(Long employerId) {
        try {
            List<Map<String, Object>> jobs = restClient
                    .get()
                    .uri("/api/jobs/employer/{employerId}", employerId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                String responseBody = response.getBody()
                                        .toString();

                                throw new ExternalServiceException(
                                        "Job Service returned HTTP "
                                                + response.getStatusCode().value()
                                                + " while fetching jobs for employer "
                                                + employerId
                                                + ". Response: "
                                                + responseBody
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return jobs != null ? jobs : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve jobs for employer "
                            + employerId
                            + " from Job Service. "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}