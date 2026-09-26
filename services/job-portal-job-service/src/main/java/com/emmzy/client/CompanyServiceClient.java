package com.emmzy.client;

import com.emmzy.dto.CompanyResponse;
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

    public CompanyResponse getMyCompany(Long employerId) {

        String url = companyServiceUrl + "/api/companies/my";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", employerId.toString());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CompanyResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        CompanyResponse.class
                );

        return response.getBody();
    }
}