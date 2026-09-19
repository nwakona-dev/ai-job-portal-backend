package com.emmzy.service;

import com.emmzy.dto.ResumeSkillResponse;
import com.emmzy.payload.AddResumeSkillRequest;

import java.util.List;

public interface ResumeSkillService {

    ResumeSkillResponse addSkill(Long resumeId, Long candidateId, AddResumeSkillRequest req) throws Exception;

    List<ResumeSkillResponse> getSkill(Long resumeId);

    ResumeSkillResponse updateSkill(
            Long skillId, Long resumeId, Long candidateId, AddResumeSkillRequest req
    ) throws Exception;
    void deleteSkill(Long skillId, Long resumeId, Long candidateId) throws Exception;
}
