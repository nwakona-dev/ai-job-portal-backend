package com.emmzy.service;

import com.emmzy.dto.ProjectResponse;
import com.emmzy.payload.AddProjectRequest;

import java.util.List;

public interface ProjectService {

    ProjectResponse addProject(
            Long resumeId, Long candidateId, AddProjectRequest req
    ) throws Exception;

    List<ProjectResponse> getAllProjects(Long resumeId);

    ProjectResponse updateProject(Long projectId, Long resumeId,
                                  Long candidateId,
                                  AddProjectRequest req) throws Exception;

    void deleteProject(Long projectId, Long resumeId, Long candidateId) throws Exception;
}
