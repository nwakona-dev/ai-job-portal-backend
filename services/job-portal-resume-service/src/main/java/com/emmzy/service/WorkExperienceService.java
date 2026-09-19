package com.emmzy.service;

import com.emmzy.dto.WorkExperienceResponse;
import com.emmzy.model.WorkExperience;
import com.emmzy.payload.AddWorkExperience;

import java.util.List;

public interface WorkExperienceService {

    WorkExperienceResponse addWorkExperience(Long resumeId, Long candidateId, AddWorkExperience req) throws Exception;

    List<WorkExperienceResponse> getWorkExperiences(Long resumeId);

    WorkExperienceResponse updateWorkExperience(
            Long resumeId, Long candidateId, Long workExperienceId, AddWorkExperience req) throws Exception;

    void deleteWorkExperience(Long resumeId, Long workExperienceId, Long candidateId) throws Exception;

    WorkExperience getWorkExperienceEntity(Long workExperienceId) throws Exception;
}
