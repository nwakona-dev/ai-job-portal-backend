
package com.emmzy.dto;

import com.emmzy.domain.CompanySize;
import com.emmzy.domain.CompanyType;
import com.emmzy.domain.IndustryType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String tagline;

    private String description;

    private String logoUrl;

    private String coverImageUrl;

    private String website;

    @Email(message = "Please provide a valid company email")
    private String email;

    private String phone;

    private Integer foundedYear;

    private CompanySize companySize;

    private CompanyType companyType;

    private IndustryType industryType;

    private String registrationNumber;

    private List<SocialLinkResponse> socialLinks;
}

