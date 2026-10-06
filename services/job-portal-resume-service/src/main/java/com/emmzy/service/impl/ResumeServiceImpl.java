package com.emmzy.service.impl;

import com.emmzy.client.UserServiceClient;
import com.emmzy.domain.ResumeTemplate;
import com.emmzy.domain.ResumeVisibility;
import com.emmzy.dto.*;
import com.emmzy.dto.response.UserResponse;
import com.emmzy.mapper.ResumeMapper;
import com.emmzy.mapper.WorkExperienceMapper;
import com.emmzy.model.PersonalInfo;
import com.emmzy.model.Resume;
import com.emmzy.payload.CreateResumeRequest;
import com.emmzy.repository.*;
import com.emmzy.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final ProjectRepository projectRepository;

    private final UserServiceClient userServiceClient;

    @Override
    public ResumeResponse createResume(
            Long candidateId,
            CreateResumeRequest req
    ) {

        // =========================================================
        // 1. Validate candidate
        // =========================================================

        if (candidateId == null) {
            throw new IllegalArgumentException(
                    "Candidate ID is required"
            );
        }

        UserResponse user = validateCandidate(candidateId);

        // =========================================================
        // 2. Only job seekers can create resumes
        // =========================================================

        String role = user.getRole();

        if (role == null ||
                !role.trim().equalsIgnoreCase("ROLE_JOB_SEEKER")) {

            throw new IllegalArgumentException(
                    "Only job seekers can create resumes"
            );
        }

        // =========================================================
        // 3. Handle default resume
        // =========================================================

        if (Boolean.TRUE.equals(req.getIsDefault())) {

            resumeRepository
                    .findByCandidateIdAndIsDefaultTrue(candidateId)
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        resumeRepository.save(existing);
                    });
        }

        // =========================================================
        // 4. Create resume
        // =========================================================

        Resume resume = Resume.builder()
                .candidateId(candidateId)
                .title(req.getTitle())
                .template(
                        req.getTemplate() != null
                                ? req.getTemplate()
                                : ResumeTemplate.PROFESSIONAL
                )
                .visibility(
                        req.getVisibility() != null
                                ? req.getVisibility()
                                : ResumeVisibility.PUBLIC
                )
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .isActive(true)
                .build();
        // =========================================================
        // 5. Save
        // =========================================================

        Resume saved = resumeRepository.save(resume);

        // =========================================================
        // 6. Return complete resume
        // =========================================================

        return buildFullResponse(saved);
    }

    /**
     * Verify that the candidate exists in User Service.
     */
    private UserResponse validateCandidate(Long candidateId) {

        try {

            UserResponse user =
                    userServiceClient.getUserById(candidateId);

            if (user == null) {
                throw new IllegalArgumentException(
                        "Candidate not found"
                );
            }

            return user;

        } catch (IllegalArgumentException ex) {
            throw ex;

        } catch (Exception ex) {

            throw new IllegalArgumentException(
                    "Unable to verify candidate with User Service",
                    ex
            );
        }
    }

    @Override
    public ResumeResponse getResumeById(
            Long resumeId,
            Long candidateId
    ) throws Exception {

        Resume resume = getResumeEntity(resumeId);

        assertOwner(resume, candidateId);

        return buildFullResponse(resume);
    }

    @Override
    public List<ResumeResponse> getMyResumes(Long candidateId) {

        return resumeRepository
                .findByCandidateIdAndIsActiveTrue(candidateId)
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public ResumeResponse updatePersonalInfo(
            Long resumeId,
            Long candidateId,
            PersonalInfoResponse req
    ) throws Exception {

        Resume resume = getResumeEntity(resumeId);

        assertOwner(resume, candidateId);

        PersonalInfo info = resume.getPersonalInfo();

        if (info == null) {
            info = new PersonalInfo();
        }

        if (req.getFirstName() != null) {
            info.setFirstName(req.getFirstName());
        }

        if (req.getLastName() != null) {
            info.setLastName(req.getLastName());
        }

        if (req.getHeadLine() != null) {
            info.setHeadLine(req.getHeadLine());
        }

        if (req.getEmail() != null) {
            info.setEmail(req.getEmail());
        }

        if (req.getPhone() != null) {
            info.setPhone(req.getPhone());
        }

        if (req.getCity() != null) {
            info.setCity(req.getCity());
        }

        if (req.getCountry() != null) {
            info.setCountry(req.getCountry());
        }

        if (req.getLinkedinUrl() != null) {
            info.setLinkedinUrl(req.getLinkedinUrl());
        }

        if (req.getGithubUrl() != null) {
            info.setGithubUrl(req.getGithubUrl());
        }

        if (req.getPortfolioUrl() != null) {
            info.setPortfolioUrl(req.getPortfolioUrl());
        }

        if (req.getWebsiteUrl() != null) {
            info.setWebsiteUrl(req.getWebsiteUrl());
        }

        resume.setPersonalInfo(info);

        Resume updated = resumeRepository.save(resume);

        return buildFullResponse(updated);
    }

    @Override
    public ResumeResponse updateSummary(
            Long resumeId,
            Long candidateId,
            String summary
    ) throws Exception {

        Resume resume = getResumeEntity(resumeId);

        assertOwner(resume, candidateId);

        resume.setSummary(summary);

        Resume updated = resumeRepository.save(resume);

        return buildFullResponse(updated);
    }

    @Override
    public ResumeResponse setDefaultResume(
            Long resumeId,
            Long candidateId
    ) throws Exception {

        Resume resume = getResumeEntity(resumeId);

        assertOwner(resume, candidateId);

        resumeRepository
                .findByCandidateIdAndIsDefaultTrue(candidateId)
                .ifPresent(existing -> {

                    existing.setIsDefault(false);

                    resumeRepository.save(existing);
                });

        resume.setIsDefault(true);

        Resume updated = resumeRepository.save(resume);

        return buildFullResponse(updated);
    }

    @Override
    public void deleteResume(
            Long resumeId,
            Long candidateId
    ) throws Exception {

        Resume resume = getResumeEntity(resumeId);

        assertOwner(resume, candidateId);

        resume.setIsActive(false);
        resume.setIsDefault(false);

        resumeRepository.save(resume);
    }

    @Override
    public Resume getResumeEntity(Long resumeId) throws Exception {

        return resumeRepository
                .findById(resumeId)
                .orElseThrow(
                        () -> new Exception(
                                "Resume not found with id " + resumeId
                        )
                );
    }

    private ResumeResponse buildFullResponse(Resume resume) {

        Long resumeId = resume.getId();

        List<WorkExperienceResponse> workExperienceResponses =
                workExperienceRepository
                        .findByResume_IdOrderByDisplayOrderAsc(resumeId)
                        .stream()
                        .map(WorkExperienceMapper::toWorkExperienceResponse)
                        .toList();

        List<EducationResponse> educationResponses =
                educationRepository
                        .findByResumeIdOrderByDisplayOrderAsc(resumeId)
                        .stream()
                        .map(ResumeMapper::toEducationResponse)
                        .toList();

        List<ResumeSkillResponse> skills =
                resumeSkillRepository
                        .findByResumeIdOrderByDisplayOrderAsc(resumeId)
                        .stream()
                        .map(ResumeMapper::toSkillResponse)
                        .toList();

        List<ProjectResponse> projects =
                projectRepository
                        .findByResumeIdOrderByDisplayOrderAsc(resumeId)
                        .stream()
                        .map(ResumeMapper::toProjectResponse)
                        .toList();

        return ResumeMapper.toResponse(
                resume,
                workExperienceResponses,
                educationResponses,
                skills,
                projects
        );
    }

    private void assertOwner(
            Resume resume,
            Long candidateId
    ) throws Exception {

        if (resume.getCandidateId() == null ||
                !resume.getCandidateId().equals(candidateId)) {

            throw new Exception(
                    "Resume not found with id " + resume.getId()
            );
        }
    }
}