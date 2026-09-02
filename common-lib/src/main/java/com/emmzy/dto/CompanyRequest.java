package com.emmzy.dto;

import com.emmzy.domain.CompanySize;
import com.emmzy.domain.CompanyType;
import com.emmzy.domain.IndustryType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String tagline;

    private String description;

    private String logoUrl;

    private String coverImageUrl;

    @Pattern(regexp = "^(https?://.*)", message = "Website must be a valid URL")
    private String website;

    @Email(message = "Company email must be valid")
    private String email;

    private String phone;

    @Min(value = 1800, message = "Founded year seems too old")
    @Max(value = 2100, message = "Founded year is invalid")
    private Integer foundedYear;

    @NotNull(message = "Company size is required")
    private CompanySize companySize;

    @NotNull(message = "Company type is required")
    private CompanyType companyType;

    @NotNull(message = "Industry type is required")
    private IndustryType industryType;

    private String registrationNumber;

    private List<SocialLinkResponse> socialLinks;

}
