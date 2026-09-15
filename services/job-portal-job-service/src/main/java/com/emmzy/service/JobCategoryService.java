package com.emmzy.service;


import com.emmzy.dto.JobCategoryResponse;
import com.emmzy.model.JobCategory;
import com.emmzy.payload.JobCategoryRequest;

import java.util.List;

public interface JobCategoryService {

    JobCategoryResponse createCategory(JobCategoryRequest req) throws Exception;

    List<JobCategoryResponse> getAllCategories();

    JobCategoryResponse getCategoryById(Long id) throws Exception;

    JobCategoryResponse updateCategory(Long id, JobCategoryRequest req) throws Exception;

    void deleteCategory(Long id) throws Exception;

    JobCategory getCategoryEntityById(Long id) throws Exception;
}
