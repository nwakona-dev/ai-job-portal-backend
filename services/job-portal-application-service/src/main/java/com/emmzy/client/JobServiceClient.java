package com.emmzy.client;

import com.emmzy.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class JobServiceClient {

    private final RestTemplate restTemplate;

    @Value("${job-service.url}")
    private String jobServiceUrl;

    public JobResponse getJobById(Long jobId) {

        String url =
                jobServiceUrl +
                        "/api/jobs/" +
                        jobId;

        try {

            return restTemplate.getForObject(
                    url,
                    JobResponse.class
            );

        } catch (HttpClientErrorException.NotFound e) {

            System.err.println(
                    "[JobServiceClient] Job not found: " +
                            jobId
            );

            return null;

        } catch (HttpClientErrorException |
                 HttpServerErrorException e) {

            System.err.println(
                    "[JobServiceClient] Failed to fetch job " +
                            jobId +
                            ". Status: " +
                            e.getStatusCode()
            );

            return null;
        }
    }
}