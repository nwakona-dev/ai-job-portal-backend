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

    @Override
    public CompanyResponse createCompany(Long ownerId, CompanyRequest req) throws Exception {

        if (companyRepository.existsByOwnerId(ownerId)) {
            throw new Exception(
                    "You already have a company registered. " +
                            "Only one company per account is allowed"
            );
        }

        if (companyRepository.existsByName(req.getName())) {
            throw new Exception(
                    "Company already exists. " +
                            "Please choose a different name"
            );
        }


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

        String slug = generateUniqueSlug(req.getName());

        Company company = Company.builder()
                .name(req.getName())
                .slug(slug)
                .tagline(req.getTagline())
                .description(req.getDescription())
                .logoUrl(req.getLogoUrl())
                .coverImageUrl(req.getCoverImageUrl())
                .website(req.getWebsite())
                .email(req.getEmail())
                .phone(req.getPhone())
                .foundedYear(req.getFoundedYear())
                .companySize(req.getCompanySize())
                .companyType(req.getCompanyType())
                .industryType(req.getIndustryType())
                .registrationNumber(req.getRegistrationNumber())
                .ownerId(ownerId)
                .socialLinks(mapSocialLinks(req.getSocialLinks()))
                .build();

        Company savedCompany = companyRepository.save(company);
        return CompanyMapper.toResponse(savedCompany);
    }

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

    @Override
    public CompanyResponse getCompanyById(Long id) throws Exception {
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

    @Override
    public CompanyResponse getMyCompany(Long ownerId) throws Exception {
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

    @Override
    public CompanyResponse updateCompany(Long companyId, Long ownerId, CompanyRequest req
    ) throws Exception {

        Company company = getCompanyEntityById(companyId);

        assertOwner(company, ownerId);

        if (!company.getName().equals(req.getName())
                && companyRepository.existsByName(req.getName())) {

            throw new Exception(
                    "Company already exists. " +
                            "Please choose a different name"
            );
        }



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

        company.setName(req.getName());
        company.setTagline(req.getTagline());
        company.setDescription(req.getDescription());

        if (req.getLogoUrl() != null && !req.getLogoUrl().isBlank()) {
            company.setLogoUrl(
                    req.getLogoUrl()
            );
        }

        if (req.getCoverImageUrl() != null && !req.getCoverImageUrl().isBlank()) {
            company.setCoverImageUrl(
                    req.getCoverImageUrl()
            );
        }

        company.setWebsite(req.getWebsite());
        company.setEmail(req.getEmail());
        company.setPhone(req.getPhone());
        company.setFoundedYear(req.getFoundedYear());
        company.setCompanySize(req.getCompanySize());
        company.setCompanyType(req.getCompanyType());
        company.setIndustryType(req.getIndustryType());
        company.setRegistrationNumber(req.getRegistrationNumber());
        company.setSocialLinks(mapSocialLinks(req.getSocialLinks()));

        Company updatedCompany = companyRepository.save(company);
        return CompanyMapper.toResponse(updatedCompany);
    }

    @Override
    public CompanyResponse verifyCompany(Long companyId) throws Exception {

        Company company = getCompanyEntityById(companyId);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setVerified(true);

        return CompanyMapper.toResponse(companyRepository.save(company)
        );
    }

    @Override
    public void deleteCompany(Long companyId, Long ownerId) throws Exception {

        Company company = getCompanyEntityById(companyId);
        assertOwner(company, ownerId);

        companyRepository.delete(company);
    }


    private void assertOwner(Company company, Long ownerId) throws Exception {

        if (!company.getOwnerId().equals(ownerId)) {
            throw new Exception(
                    "You are not the owner of this company"
            );
        }
    }

    @Override
    public CompanyResponse deactivateCompany(Long companyId) throws Exception {

        Company company = getCompanyEntityById(companyId);
        company.setStatus(CompanyStatus.SUSPENDED);
        company.setVerified(false);

        return CompanyMapper.toResponse(companyRepository.save(company)
        );
    }

    // GET COMPANY ENTITY
    @Override
    public Company getCompanyEntityById(Long id) throws Exception {

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