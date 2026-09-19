package com.emmzy.service;

import com.emmzy.dto.EducationResponse;
import com.emmzy.payload.AddEducationRequest;

import java.util.List;

public interface EducationService {

    EducationResponse addEducation(Long resumeId,
                                   Long candidateId,
                                   AddEducationRequest req) throws Exception;

    List<EducationResponse> getEducation(Long resumeId);

    EducationResponse updateEducation (Long educationId,
                                       Long resumeId, Long candidateId,
                                       AddEducationRequest req) throws Exception;

    void deleteEducation(Long educationId, Long resumeId, Long candidateId) throws Exception;


}
