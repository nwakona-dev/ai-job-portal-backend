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
public class ResumeServiceClient {

    private final RestClient restClient;

    public ResumeServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.resume.url}") String resumeServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(resumeServiceUrl)
                .build();
    }

    /**
     * Retrieves all resumes from the Resume Service.
     */
    public List<Map<String, Object>> getResumes() {
        try {
            List<Map<String, Object>> resumes = restClient
                    .get()
                    .uri("/api/resumes")
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Resume Service returned an error while fetching resumes"
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return resumes != null ? resumes : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to communicate with Resume Service",
                    exception
            );
        }
    }

    /**
     * Retrieves a specific resume by ID.
     */
    public Map<String, Object> getResumeById(Long resumeId) {
        try {
            Map<String, Object> resume = restClient
                    .get()
                    .uri("/api/resumes/{resumeId}", resumeId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Resume Service returned an error while fetching resume "
                                                + resumeId
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return resume != null ? resume : Map.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve resume " + resumeId,
                    exception
            );
        }
    }

    /**
     * Retrieves resumes belonging to a specific candidate.
     */
    public List<Map<String, Object>> getResumesByCandidate(Long candidateId) {
        try {
            List<Map<String, Object>> resumes = restClient
                    .get()
                    .uri(
                            uriBuilder -> uriBuilder
                                    .path("/api/resumes")
                                    .queryParam("candidateId", candidateId)
                                    .build()
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ExternalServiceException(
                                        "Resume Service returned an error while fetching resumes "
                                                + "for candidate " + candidateId
                                );
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});

            return resumes != null ? resumes : List.of();

        } catch (ExternalServiceException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Unable to retrieve resumes for candidate " + candidateId,
                    exception
            );
        }
    }
}