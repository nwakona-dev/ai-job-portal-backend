package com.emmzy.service;

import com.emmzy.dto.JobTagResponse;
import com.emmzy.model.JobTag;
import com.emmzy.payload.JobTagRequest;

import java.util.List;
import java.util.Set;

public interface JobTagService {

    JobTagResponse createTag(JobTagRequest req) throws Exception;

    List<JobTagResponse> getAllTags();

    JobTagResponse getById(Long id) throws Exception;

    JobTagResponse updateTag(Long id, JobTagRequest req) throws Exception;

    void deleteTag(Long id) throws Exception;

    JobTag getTagEntityById(Long id) throws Exception;

    Set<JobTag> getTagByIds(Set<Long> ids) throws Exception;
}
