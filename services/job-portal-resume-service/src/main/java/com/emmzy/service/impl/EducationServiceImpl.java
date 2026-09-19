package com.emmzy.service.impl;

import com.emmzy.dto.EducationResponse;
import com.emmzy.mapper.ResumeMapper;
import com.emmzy.model.Education;
import com.emmzy.model.Resume;
import com.emmzy.repository.EducationRepository;
import com.emmzy.payload.AddEducationRequest;
import com.emmzy.service.EducationService;
import com.emmzy.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements EducationService {

    private final ResumeService resumeService;
    private final EducationRepository educationRepository;

    @Override
    public EducationResponse addEducation(Long resumeId, Long candidateId, AddEducationRequest req) throws Exception {
        Resume resume = resumeService.getResumeEntity(resumeId);
        assertOwner(resume, candidateId);

        Education education = Education.builder()
                .resume(resume)
                .institutionName(req.getInstitutionName())
                .degree(req.getDegree())
                .fieldOfStudy(req.getFieldOfStudy())
                .grade(req.getGrade())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isCurrentlyStudying(Boolean.TRUE.equals(req.getIsCurrentlyStudying()))
                .description(req.getDescription())
                .displayOrder(req.getDisplayOrder() != null ?req.getDisplayOrder(): 0)

                .build();

        Education savedEducation = educationRepository.save(education);
        return ResumeMapper.toEducationResponse(savedEducation);
    }

    @Override
    public List<EducationResponse> getEducation(Long resumeId) {
        return educationRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId)
                .stream().map(ResumeMapper::toEducationResponse).toList();
    }

    @Override
    public EducationResponse updateEducation(Long educationId, Long resumeId, Long candidateId, AddEducationRequest req) throws Exception {
        Education edu = educationRepository.findById(educationId).orElseThrow(
                ()-> new Exception ("Education does not exist")
        );
        assertOwner(edu.getResume(), candidateId);

        edu.setInstitutionName(req.getInstitutionName());
        edu.setDegree(req.getDegree());
        edu.setFieldOfStudy(req.getFieldOfStudy());
        edu.setGrade(req.getGrade());
        edu.setStartDate(req.getStartDate());
        edu.setEndDate(req.getEndDate());
        edu.setIsCurrentlyStudying(req.getIsCurrentlyStudying());
        edu.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) edu.setDisplayOrder(req.getDisplayOrder());


         return ResumeMapper.toEducationResponse(educationRepository.save(edu));
    }

    @Override
    public void deleteEducation(Long educationId, Long resumeId, Long candidateId) throws Exception {

        Education edu = educationRepository.findById(educationId).orElseThrow(
                ()-> new Exception ("Education does not exist")
        );
        assertOwner(edu.getResume(), candidateId);
        educationRepository.delete(edu);
    }

    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if (!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("resume not found");
        }
    }
}
