package com.emmzy.service.impl;

import com.emmzy.dto.ResumeSkillResponse;
import com.emmzy.mapper.ResumeMapper;
import com.emmzy.model.Resume;
import com.emmzy.model.ResumeSkill;
import com.emmzy.payload.AddResumeSkillRequest;
import com.emmzy.repository.ResumeSkillRepository;
import com.emmzy.service.ResumeService;
import com.emmzy.service.ResumeSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeSkillServiceImpl implements ResumeSkillService {

    private final ResumeService resumeService;
    private final ResumeSkillRepository resumeSkillRepository;

    @Override
    public ResumeSkillResponse addSkill(Long resumeId, Long candidateId, AddResumeSkillRequest req) throws Exception {
        Resume resume = resumeService.getResumeEntity(resumeId);
        assertOwner(resume, candidateId);

        ResumeSkill skill = ResumeSkill.builder()
                .resume(resume)
                .skillName(req.getSkillName())
                .proficiencyLevel(req.getProficiencyLevel())
                .yearsOfExperience(req.getYearsOfExperience())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .build();

        ResumeSkill savedSkill = resumeSkillRepository.save(skill);
        return ResumeMapper.toSkillResponse(savedSkill);
    }

    @Override
    public List<ResumeSkillResponse> getSkill(Long resumeId) {
        return resumeSkillRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId)
                .stream().map(ResumeMapper::toSkillResponse).toList();
    }

    @Override
    public ResumeSkillResponse updateSkill(Long skillId, Long resumeId, Long candidateId, AddResumeSkillRequest req) throws Exception {
        ResumeSkill skill = resumeSkillRepository.findById(skillId).orElseThrow(
                ()->new Exception("Skill not found")
        );
        assertOwner(skill.getResume(), candidateId);

        skill.setSkillName(req.getSkillName());
        skill.setProficiencyLevel(req.getProficiencyLevel());
        skill.setYearsOfExperience(req.getYearsOfExperience());
        if (req.getDisplayOrder() != null) skill.setDisplayOrder(req.getDisplayOrder());

        return ResumeMapper.toSkillResponse(resumeSkillRepository.save(skill));
    }

    @Override
    public void deleteSkill(Long skillId, Long resumeId, Long candidateId) throws Exception {
        ResumeSkill skill = resumeSkillRepository.findById(skillId).orElseThrow(
                ()->new Exception("Skill not found")
        );
        assertOwner(skill.getResume(), candidateId);
        resumeSkillRepository.delete(skill);
    }

    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if (!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("resume not found");
        }
    }
}
