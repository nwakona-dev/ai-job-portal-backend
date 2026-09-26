package com.emmzy.service;

import com.emmzy.model.Application;
import com.emmzy.model.ApplicationStatus;

import java.util.List;

public interface ApplicationService {

    Application createApplication(
            Long jobId,
            Long jobSeekerId,
            Long resumeId
    ) throws Exception;

    List<Application> getApplicationsByJobSeeker(Long jobSeekerId);

    List<Application> getApplicationsByJob(Long jobId);

    Application getApplication(
            Long applicationId,
            Long jobSeekerId
    ) throws Exception;

    Application updateStatus(
            Long applicationId,
            ApplicationStatus status
    ) throws Exception;

    void withdrawApplication(
            Long applicationId,
            Long jobSeekerId
    ) throws Exception;
}