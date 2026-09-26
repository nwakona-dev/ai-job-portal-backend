package com.emmzy.service;

import com.emmzy.domain.ApplicationStatus;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.model.Application;
import com.emmzy.payload.CompanyApplicationFilterRequest;
import com.emmzy.payload.CreateApplicationRequest;
import com.emmzy.payload.WithdrawApplicationRequest;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse createApplication(
            Long candidateId,
            CreateApplicationRequest req
    ) throws Exception;

    ApplicationResponse getApplicationById(Long id) throws Exception;

    List<ApplicationResponse> getMyApplication(Long candidateId);

    List<ApplicationResponse> getApplicationForJob(Long jobId);

    List<ApplicationResponse> getApplicationForCompany(Long userId,
                                                       CompanyApplicationFilterRequest request) throws Exception;

    ApplicationResponse updateStatus(Long applicationId, Long employerId,  ApplicationStatus status) throws Exception;

    ApplicationResponse withdraw(Long applicationId,
                                 Long candidateId,
                                 WithdrawApplicationRequest req) throws Exception;

    ApplicationResponse toggleStar(Long applicationId, Long employerId) throws Exception;

    void deleteApplication(Long applicationId, Long candidateId) throws Exception;

    Application getApplicationEntity(Long id) throws Exception;

}
