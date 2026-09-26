package com.emmzy.client;

import com.emmzy.dto.ResumeResponse;
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
public class ResumeServiceClient {

    private final RestTemplate restTemplate;

    @Value("${resume-service.url}")
    private String resumeServiceUrl;

    public ResumeResponse getResumeById(
            Long resumeId,
            Long candidateId
    ) {

        String url = resumeServiceUrl + "/api/resumes/" + resumeId;

        HttpHeaders headers = new HttpHeaders();

        headers.set("X-User-Id", candidateId.toString());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ResumeResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        ResumeResponse.class
                );

        return response.getBody();
    }
}