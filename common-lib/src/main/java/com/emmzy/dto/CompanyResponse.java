package com.emmzy.dto;


import com.emmzy.domain.CompanySize;
import com.emmzy.domain.CompanyStatus;
import com.emmzy.domain.CompanyType;
import com.emmzy.domain.IndustryType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CompanyResponse {

    private Long id;
    private String name;
    private String slug;
    private String tagline;
    private String description;
    private String logoUrl;
    private String coverImageUrl;
    private String website;
    private String email;
    private String phone;
    private Integer foundedYear;

    private CompanySize companySize;
    private CompanyType companyType;
    private IndustryType industryType;
    private CompanyStatus status;
    private Boolean active;

    private Long ownerId;

    private List<SocialLinkResponse> socialLinks;
;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime verifiedAt;
}
