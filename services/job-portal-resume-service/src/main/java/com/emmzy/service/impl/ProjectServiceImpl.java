package com.emmzy.service.impl;

import com.emmzy.dto.ProjectResponse;
import com.emmzy.mapper.ResumeMapper;
import com.emmzy.model.Project;
import com.emmzy.model.Resume;
import com.emmzy.payload.AddProjectRequest;
import com.emmzy.repository.ProjectRepository;
import com.emmzy.service.ProjectService;
import com.emmzy.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ResumeService resumeService;
    private final ProjectRepository projectRepository;
    @Override
    public ProjectResponse addProject(Long resumeId, Long candidateId, AddProjectRequest req) throws Exception {
        Resume resume = resumeService.getResumeEntity(resumeId);
        assertOwner(resume, candidateId);

        Project project = Project.builder()
                .resume(resume)
                .title(req.getTitle())
                .description(req.getDescription())
                .technologies(req.getTechnologies() != null ? req.getTechnologies() : List.of())
                .projectUrl(req.getProjectUrl())
                .sourceCodeUrl(req.getSourceCodeUrl())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isOngoing(Boolean.TRUE.equals(req.getIsOngoing()))
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .build();

        Project saved = projectRepository.save(project);
        return ResumeMapper.toProjectResponse(saved);
    }

    @Override
    public List<ProjectResponse> getAllProjects(Long resumeId) {

        return projectRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId)
                .stream().map(ResumeMapper::toProjectResponse).toList();
    }

    @Override
    public ProjectResponse updateProject(Long projectId, Long resumeId, Long candidateId, AddProjectRequest req) throws Exception {
        Project project = projectRepository.findById(projectId).orElseThrow(
                ()->new Exception("Project not found")
        );
        assertOwner(project.getResume(), candidateId);

        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        if (req.getTechnologies() != null) project.setTechnologies(req.getTechnologies());
        project.setProjectUrl(req.getProjectUrl());
        project.setSourceCodeUrl(req.getSourceCodeUrl());
        project.setStartDate(req.getStartDate());
        project.setEndDate(req.getEndDate());
        project.setIsOngoing(Boolean.TRUE.equals(req.getIsOngoing()));
        if (req.getDisplayOrder() != null) project.setDisplayOrder(req.getDisplayOrder());

        return ResumeMapper.toProjectResponse(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long projectId, Long resumeId, Long candidateId) throws Exception {
        Project project = projectRepository.findById(projectId).orElseThrow(
                ()->new Exception("Project not found")
        );
        assertOwner(project.getResume(), candidateId);
        projectRepository.delete(project);
    }

    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if (!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("resume not found");
        }
    }
}
