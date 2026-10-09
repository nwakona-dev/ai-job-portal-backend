package com.emmzy.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResumeServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.resume.url}")
    private String resumeServiceUrl;

    /**
     * Get resumes belonging to a specific candidate.
     *
     * Resume Service:
     *
     * GET /api/resumes/my
     *
     * Header:
     *
     * X-User-Id: {candidateId}
     */
    public List<Map<String, Object>> getResumesByCandidate(
            Long candidateId
    ) {

        if (candidateId == null) {
            return Collections.emptyList();
        }

        try {

            RestClient client =
                    restClientBuilder
                            .baseUrl(resumeServiceUrl)
                            .build();

            List<Map<String, Object>> response =
                    client.get()
                            .uri("/api/resumes/my")
                            .header(
                                    "X-User-Id",
                                    String.valueOf(candidateId)
                            )
                            .retrieve()
                            .body(
                                    new ParameterizedTypeReference<
                                            List<Map<String, Object>>
                                            >() {}
                            );

            System.out.println(
                    "================================================"
            );

            System.out.println(
                    "RESUME SERVICE RESPONSE"
            );

            System.out.println(
                    "Candidate ID: " + candidateId
            );

            System.out.println(
                    "Response: " + response
            );

            System.out.println(
                    "================================================"
            );

            return response != null
                    ? response
                    : Collections.emptyList();

        } catch (Exception e) {

            System.err.println(
                    "================================================"
            );

            System.err.println(
                    "RESUME SERVICE ERROR"
            );

            System.err.println(
                    "Candidate ID: " + candidateId
            );

            System.err.println(
                    "Resume Service URL: "
                            + resumeServiceUrl
            );

            System.err.println(
                    "Error Type: "
                            + e.getClass().getName()
            );

            System.err.println(
                    "Error Message: "
                            + e.getMessage()
            );

            e.printStackTrace();

            System.err.println(
                    "================================================"
            );

            return Collections.emptyList();
        }
    }

    /**
     * Get a specific resume.
     *
     * Resume Service requires:
     *
     * X-User-Id: {candidateId}
     */
    public Map<String, Object> getResumeById(
            Long resumeId,
            Long candidateId
    ) {

        if (resumeId == null
                || candidateId == null) {

            return null;
        }

        try {

            RestClient client =
                    restClientBuilder
                            .baseUrl(resumeServiceUrl)
                            .build();

            Map<String, Object> response =
                    client.get()
                            .uri(
                                    "/api/resumes/{resumeId}",
                                    resumeId
                            )
                            .header(
                                    "X-User-Id",
                                    String.valueOf(candidateId)
                            )
                            .retrieve()
                            .body(
                                    new ParameterizedTypeReference<
                                            Map<String, Object>
                                            >() {}
                            );

            System.out.println(
                    "================================================"
            );

            System.out.println(
                    "SINGLE RESUME RESPONSE"
            );

            System.out.println(
                    "Resume ID: " + resumeId
            );

            System.out.println(
                    "Candidate ID: " + candidateId
            );

            System.out.println(
                    "Response: " + response
            );

            System.out.println(
                    "================================================"
            );

            return response;

        } catch (Exception e) {

            System.err.println(
                    "================================================"
            );

            System.err.println(
                    "SINGLE RESUME SERVICE ERROR"
            );

            System.err.println(
                    "Resume ID: " + resumeId
            );

            System.err.println(
                    "Candidate ID: " + candidateId
            );

            System.err.println(
                    "Error Type: "
                            + e.getClass().getName()
            );

            System.err.println(
                    "Error Message: "
                            + e.getMessage()
            );

            e.printStackTrace();

            System.err.println(
                    "================================================"
            );

            return null;
        }
    }
}