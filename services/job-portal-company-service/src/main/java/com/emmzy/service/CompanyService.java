package com.emmzy.service;

import com.emmzy.domain.CompanyStatus;
import com.emmzy.domain.CompanyType;
import com.emmzy.domain.IndustryType;
import com.emmzy.dto.CompanyRequest;
import com.emmzy.dto.CompanyResponse;
import com.emmzy.model.Company;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(Long ownerId, CompanyRequest req) throws Exception;
    CompanyResponse getCompanyById(Long id) throws Exception;
    CompanyResponse getMyCompany(Long ownerId) throws Exception;
    List<CompanyResponse> getAllCompanies(CompanyType companyType,
                                          IndustryType industryType,
                                          CompanyStatus companyStatus);
    CompanyResponse updateCompany(Long companyId, Long ownerId, CompanyRequest req) throws Exception;
    CompanyResponse verifyCompany(Long companyId) throws Exception;
    void deleteCompany(Long companyId, Long ownerId) throws Exception;
    CompanyResponse deactivateCompany(Long companyId) throws Exception;

    Company getCompanyEntityById(Long id) throws Exception;

}
