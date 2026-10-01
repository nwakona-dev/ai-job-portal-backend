package com.emmzy.service.impl;

import com.emmzy.domain.CompanyStatus;
import com.emmzy.domain.CompanyType;
import com.emmzy.domain.IndustryType;
import com.emmzy.dto.CompanyRequest;
import com.emmzy.dto.CompanyResponse;
import com.emmzy.dto.SocialLinkResponse;
import com.emmzy.mapper.CompanyMapper;
import com.emmzy.model.Company;
import com.emmzy.model.SocialLink;
import com.emmzy.repository.CompanyRepository;
import com.emmzy.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;


    // =========================================================
    // CREATE COMPANY
    // =========================================================

    @Override
    public CompanyResponse createCompany(
            Long ownerId,
            CompanyRequest req
    ) throws Exception {

        // -----------------------------------------------------
        // Only one company per employer account
        // -----------------------------------------------------

        if (companyRepository.existsByOwnerId(ownerId)) {

            throw new Exception(
                    "You already have a company registered. " +
                            "Only one company per account is allowed"
            );
        }


        // -----------------------------------------------------
        // Check duplicate company name
        // -----------------------------------------------------

        if (companyRepository.existsByName(req.getName())) {

            throw new Exception(
                    "Company already exists. " +
                            "Please choose a different name"
            );
        }


        // -----------------------------------------------------
        // Check duplicate registration number
        // -----------------------------------------------------

        if (req.getRegistrationNumber() != null
                && !req.getRegistrationNumber().isBlank()
                && companyRepository.existsByRegistrationNumber(
                req.getRegistrationNumber()
        )) {

            throw new Exception(
                    "Company already exists. " +
                            "Please choose a different registration number."
            );
        }


        // -----------------------------------------------------
        // Generate unique slug
        // -----------------------------------------------------

        String slug = generateUniqueSlug(req.getName());


        // -----------------------------------------------------
        // Create company
        // -----------------------------------------------------

        Company company = Company.builder()
                .name(req.getName())
                .slug(slug)
                .tagline(req.getTagline())
                .description(req.getDescription())

                // Company images
                .logoUrl(req.getLogoUrl())
                .coverImageUrl(req.getCoverImageUrl())

                .website(req.getWebsite())
                .email(req.getEmail())
                .phone(req.getPhone())

                .foundedYear(req.getFoundedYear())
                .companySize(req.getCompanySize())
                .companyType(req.getCompanyType())
                .industryType(req.getIndustryType())

                .registrationNumber(
                        req.getRegistrationNumber()
                )

                .ownerId(ownerId)

                .socialLinks(
                        mapSocialLinks(req.getSocialLinks())
                )

                .build();


        // -----------------------------------------------------
        // Save company
        // -----------------------------------------------------

        Company savedCompany =
                companyRepository.save(company);


        return CompanyMapper.toResponse(savedCompany);
    }


    // =========================================================
    // MAP SOCIAL LINKS
    // =========================================================

    private List<SocialLink> mapSocialLinks(
            List<SocialLinkResponse> socialLinks
    ) {

        if (socialLinks == null || socialLinks.isEmpty()) {

            return new ArrayList<>();
        }


        return socialLinks.stream()
                .map(link ->
                        SocialLink.builder()
                                .platform(link.getPlatform())
                                .url(link.getUrl())
                                .build()
                )
                .collect(Collectors.toList());
    }


    // =========================================================
    // GENERATE UNIQUE SLUG
    // =========================================================

    private String generateUniqueSlug(String name) {

        String base = name
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");


        // If slug doesn't exist, use it
        if (!companyRepository.existsBySlug(base)) {

            return base;
        }


        // Otherwise add number
        int counter = 1;

        while (
                companyRepository.existsBySlug(
                        base + "-" + counter
                )
        ) {

            counter++;
        }


        return base + "-" + counter;
    }


    // =========================================================
    // GET COMPANY BY ID
    // =========================================================

    @Override
    public CompanyResponse getCompanyById(
            Long id
    ) throws Exception {

        Company company =
                companyRepository.findById(id)
                        .orElseThrow(
                                () -> new Exception(
                                        "Company not found with id "
                                                + id
                                )
                        );


        return CompanyMapper.toResponse(company);
    }


    // =========================================================
    // GET MY COMPANY
    // =========================================================

    @Override
    public CompanyResponse getMyCompany(
            Long ownerId
    ) throws Exception {

        Company company =
                companyRepository
                        .findByOwnerId(ownerId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Company does not exist for owner "
                                                + ownerId
                                )
                        );


        return CompanyMapper.toResponse(company);
    }


    // =========================================================
    // GET ALL COMPANIES
    // =========================================================

    @Override
    public List<CompanyResponse> getAllCompanies(
            CompanyType companyType,
            IndustryType industryType,
            CompanyStatus companyStatus
    ) {

        return companyRepository
                .findByFilter(
                        companyType,
                        industryType,
                        companyStatus
                )
                .stream()
                .map(CompanyMapper::toResponse)
                .collect(Collectors.toList());
    }


    // =========================================================
    // UPDATE COMPANY
    // =========================================================

    @Override
    public CompanyResponse updateCompany(
            Long companyId,
            Long ownerId,
            CompanyRequest req
    ) throws Exception {

        // -----------------------------------------------------
        // Get company
        // -----------------------------------------------------

        Company company =
                getCompanyEntityById(companyId);


        // -----------------------------------------------------
        // Make sure current user owns the company
        // -----------------------------------------------------

        assertOwner(company, ownerId);


        // -----------------------------------------------------
        // Check duplicate company name
        // -----------------------------------------------------

        if (!company.getName().equals(req.getName())
                && companyRepository.existsByName(
                req.getName()
        )) {

            throw new Exception(
                    "Company already exists. " +
                            "Please choose a different name"
            );
        }


        // -----------------------------------------------------
        // Check duplicate registration number
        //
        // IMPORTANT:
        // We exclude the current company.
        //
        // This means:
        //
        // Company A has REG123
        // Company A updates its logo
        // REG123 remains valid.
        //
        // But if Company B tries to use REG123,
        // it will be rejected.
        // -----------------------------------------------------

        if (req.getRegistrationNumber() != null
                && !req.getRegistrationNumber().isBlank()
                && companyRepository
                .existsByRegistrationNumberAndIdNot(
                        req.getRegistrationNumber(),
                        companyId
                )) {

            throw new Exception(
                    "Company already exists. " +
                            "Please choose a different registration number"
            );
        }


        // -----------------------------------------------------
        // Update basic company information
        // -----------------------------------------------------

        company.setName(req.getName());

        company.setTagline(
                req.getTagline()
        );

        company.setDescription(
                req.getDescription()
        );


        // -----------------------------------------------------
        // UPDATE COMPANY LOGO
        // -----------------------------------------------------

        /*
         * The frontend now sends the selected image
         * as a Base64 string.
         *
         * Example:
         *
         * data:image/jpeg;base64,/9j/4AAQ...
         *
         * We simply store that value.
         */

        if (req.getLogoUrl() != null
                && !req.getLogoUrl().isBlank()) {

            company.setLogoUrl(
                    req.getLogoUrl()
            );
        }


        // -----------------------------------------------------
        // UPDATE COMPANY COVER IMAGE
        // -----------------------------------------------------

        if (req.getCoverImageUrl() != null
                && !req.getCoverImageUrl().isBlank()) {

            company.setCoverImageUrl(
                    req.getCoverImageUrl()
            );
        }


        // -----------------------------------------------------
        // Other company information
        // -----------------------------------------------------

        company.setWebsite(
                req.getWebsite()
        );

        company.setEmail(
                req.getEmail()
        );

        company.setPhone(
                req.getPhone()
        );

        company.setFoundedYear(
                req.getFoundedYear()
        );

        company.setCompanySize(
                req.getCompanySize()
        );

        company.setCompanyType(
                req.getCompanyType()
        );

        company.setIndustryType(
                req.getIndustryType()
        );

        company.setRegistrationNumber(
                req.getRegistrationNumber()
        );


        // -----------------------------------------------------
        // Social links
        // -----------------------------------------------------

        company.setSocialLinks(
                mapSocialLinks(
                        req.getSocialLinks()
                )
        );


        // -----------------------------------------------------
        // Save
        // -----------------------------------------------------

        Company updatedCompany =
                companyRepository.save(company);


        return CompanyMapper.toResponse(
                updatedCompany
        );
    }


    // =========================================================
    // VERIFY COMPANY
    // =========================================================

    @Override
    public CompanyResponse verifyCompany(
            Long companyId
    ) throws Exception {

        Company company =
                getCompanyEntityById(companyId);


        company.setStatus(
                CompanyStatus.ACTIVE
        );

        company.setVerified(true);


        return CompanyMapper.toResponse(
                companyRepository.save(company)
        );
    }


    // =========================================================
    // DELETE COMPANY
    // =========================================================

    @Override
    public void deleteCompany(
            Long companyId,
            Long ownerId
    ) throws Exception {

        Company company =
                getCompanyEntityById(companyId);


        assertOwner(
                company,
                ownerId
        );


        companyRepository.delete(company);
    }


    // =========================================================
    // CHECK COMPANY OWNER
    // =========================================================

    private void assertOwner(
            Company company,
            Long ownerId
    ) throws Exception {

        if (!company.getOwnerId().equals(ownerId)) {

            throw new Exception(
                    "You are not the owner of this company"
            );
        }
    }


    // =========================================================
    // DEACTIVATE COMPANY
    // =========================================================

    @Override
    public CompanyResponse deactivateCompany(
            Long companyId
    ) throws Exception {

        Company company =
                getCompanyEntityById(companyId);


        company.setStatus(
                CompanyStatus.SUSPENDED
        );

        company.setVerified(false);


        return CompanyMapper.toResponse(
                companyRepository.save(company)
        );
    }


    // =========================================================
    // GET COMPANY ENTITY
    // =========================================================

    @Override
    public Company getCompanyEntityById(
            Long id
    ) throws Exception {

        return companyRepository
                .findById(id)
                .orElseThrow(
                        () -> new Exception(
                                "Company not found with id "
                                        + id
                        )
                );
    }
}