package com.emmzy.client;

import com.emmzy.dto.CompanyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class CompanyServiceClient {

    private final RestTemplate restTemplate;

    @Value("${company-service.url}")
    private String companyServiceUrl;

    public CompanyResponse getCompanyById(Long companyId) {

        String url = companyServiceUrl + "/api/companies/" + companyId;

        return restTemplate.getForObject(
                url,
                CompanyResponse.class
        );
    }

    public CompanyResponse getMyCompany(Long ownerId) {

        String url = companyServiceUrl + "/api/companies/my";

        org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();

        headers.set("X-User-Id", ownerId.toString());

        org.springframework.http.HttpEntity<Void> entity =
                new org.springframework.http.HttpEntity<>(headers);

        org.springframework.http.ResponseEntity<CompanyResponse> response =
                restTemplate.exchange(
                        url,
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        CompanyResponse.class
                );

        return response.getBody();
    }
}