package com.emmzy.service;

import com.emmzy.dto.JobRequest;
import com.emmzy.dto.JobResponse;
import com.emmzy.payload.JobSearchRequest;

import java.util.List;

public interface JobService {

    JobResponse createJob(Long employerId, JobRequest req) throws Exception;

    JobResponse getJobById(Long jobId) throws Exception;

    List<JobResponse> getJobs(JobSearchRequest request);

    List<JobResponse> getJobsByCompany(Long companyId);

    List<JobResponse> getJobsByEmployer(Long employerId);

    JobResponse updateJob(Long jobId, Long employerId,  JobRequest req) throws Exception;

    JobResponse publishJob(Long jobId, Long employerId) throws Exception;

    JobResponse closeJob(Long jobId, Long employerId) throws Exception;

    void deleteJob(Long jobId, Long employerId) throws Exception;

    void toggleSaveJob(Long jobId, Long userId, boolean saved ) throws Exception;

    List<Long> getSavedJobIds(Long userId);

    List<JobResponse> getAllJobsAdmin();

}
