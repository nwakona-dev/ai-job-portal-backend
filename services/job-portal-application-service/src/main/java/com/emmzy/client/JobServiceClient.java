package com.emmzy.client;

import com.emmzy.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class JobServiceClient {

    private final RestTemplate restTemplate;

    @Value("${job-service.url}")
    private String jobServiceUrl;

    public JobResponse getJobById(Long jobId) {

        String url = jobServiceUrl + "/api/jobs/" + jobId;

        return restTemplate.getForObject(
                url,
                JobResponse.class
        );
    }
}