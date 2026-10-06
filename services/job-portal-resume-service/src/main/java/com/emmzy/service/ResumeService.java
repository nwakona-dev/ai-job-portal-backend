package com.emmzy.service;

import com.emmzy.dto.PersonalInfoResponse;
import com.emmzy.dto.ResumeResponse;
import com.emmzy.model.Resume;
import com.emmzy.payload.CreateResumeRequest;

import java.util.List;

public interface ResumeService {

    ResumeResponse createResume(
            Long candidateId,
            CreateResumeRequest req
    );

    ResumeResponse getResumeById(
            Long resumeId,
            Long candidateId
    ) throws Exception;

    List<ResumeResponse> getMyResumes(
            Long candidateId
    );

    ResumeResponse updatePersonalInfo(
            Long resumeId,
            Long candidateId,
            PersonalInfoResponse req
    ) throws Exception;

    ResumeResponse updateSummary(
            Long resumeId,
            Long candidateId,
            String summary
    ) throws Exception;

    ResumeResponse setDefaultResume(
            Long resumeId,
            Long candidateId
    ) throws Exception;

    void deleteResume(
            Long resumeId,
            Long candidateId
    ) throws Exception;

    Resume getResumeEntity(
            Long resumeId
    ) throws Exception;
}