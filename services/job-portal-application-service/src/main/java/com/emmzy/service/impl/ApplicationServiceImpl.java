package com.emmzy.service.impl;

import com.emmzy.model.Application;
import com.emmzy.model.ApplicationStatus;
import com.emmzy.repository.ApplicationRepository;
import com.emmzy.service.ApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public Application createApplication(
            Long jobId,
            Long jobSeekerId,
            Long resumeId
    ) throws Exception {

        if (jobId == null) {
            throw new IllegalArgumentException("Job ID is required");
        }

        if (jobSeekerId == null) {
            throw new IllegalArgumentException("Job seeker ID is required");
        }

        if (resumeId == null) {
            throw new IllegalArgumentException("Resume ID is required");
        }

        boolean alreadyApplied =
                applicationRepository.existsByJobIdAndJobSeekerId(
                        jobId,
                        jobSeekerId
                );

        if (alreadyApplied) {
            throw new IllegalStateException(
                    "You have already applied for this job"
            );
        }

        Application application = Application.builder()
                .jobId(jobId)
                .jobSeekerId(jobSeekerId)
                .resumeId(resumeId)
                .status(ApplicationStatus.APPLIED)
                .build();

        return applicationRepository.save(application);
    }

    @Override
    public List<Application> getApplicationsByJobSeeker(
            Long jobSeekerId
    ) {
        return applicationRepository.findByJobSeekerId(jobSeekerId);
    }

    @Override
    public List<Application> getApplicationsByJob(
            Long jobId
    ) {
        return applicationRepository.findByJobId(jobId);
    }

    @Override
    public Application getApplication(
            Long applicationId,
            Long jobSeekerId
    ) throws Exception {

        Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new Exception("Application not found")
                );

        if (!application.getJobSeekerId().equals(jobSeekerId)) {
            throw new IllegalStateException(
                    "You are not allowed to access this application"
            );
        }

        return application;
    }

    @Override
    @Transactional
    public Application updateStatus(
            Long applicationId,
            ApplicationStatus status
    ) throws Exception {

        Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new Exception("Application not found")
                );

        if (status == null) {
            throw new IllegalArgumentException(
                    "Application status is required"
            );
        }

        application.setStatus(status);

        return applicationRepository.save(application);
    }

    @Override
    @Transactional
    public void withdrawApplication(
            Long applicationId,
            Long jobSeekerId
    ) throws Exception {

        Application application = getApplication(
                applicationId,
                jobSeekerId
        );

        if (application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.HIRED) {

            throw new IllegalStateException(
                    "This application can no longer be withdrawn"
            );
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);

        applicationRepository.save(application);
    }
}