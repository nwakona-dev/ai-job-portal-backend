package com.emmzy.service;

import com.emmzy.dto.JobRequest;
import com.emmzy.dto.JobResponse;
import com.emmzy.payload.JobSearchRequest;

import java.util.List;

public interface JobService {

    JobResponse createJob(
            Long employerId,
            JobRequest req
    ) throws Exception;

    JobResponse getJobById(
            Long jobId
    ) throws Exception;

    /*
     * Existing method retained so other backend code does not
     * immediately break.
     */
    List<JobResponse> getJobs(
            JobSearchRequest request
    );

    /*
     * User-aware version used by the frontend.
     */
    List<JobResponse> getJobs(
            JobSearchRequest request,
            Long userId
    );

    List<JobResponse> getJobsByCompany(
            Long companyId
    );

    List<JobResponse> getJobsByEmployer(
            Long employerId
    );

    JobResponse updateJob(
            Long jobId,
            Long employerId,
            JobRequest req
    ) throws Exception;

    JobResponse publishJob(
            Long jobId,
            Long employerId
    ) throws Exception;

    JobResponse closeJob(
            Long jobId,
            Long employerId
    ) throws Exception;

    void deleteJob(
            Long jobId,
            Long employerId
    ) throws Exception;

    List<JobResponse> getAllJobsAdmin();

    /*
     * Save / unsave
     */
    boolean toggleSaveJob(
            Long jobId,
            Long userId,
            boolean saved
    ) throws Exception;
}